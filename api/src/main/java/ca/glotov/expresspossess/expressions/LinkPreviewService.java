package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.common.Links;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps a wish's picture in step with the first link in its description, unless the
 * creator uploaded a picture of their own, which is never replaced.
 *
 * <p>Runs on a background thread after the change commits, for links nobody previewed
 * while typing. The picture is written with a single update that leaves the version column
 * alone: its arrival must not make the creator's next Save look like a conflicting edit.
 */
@Component
class LinkPreviewService {

    private final ExpressionRepository expressions;
    private final LinkPreviews previews;
    private final LinkPreviewProperties properties;
    private final ApplicationEventPublisher events;

    LinkPreviewService(ExpressionRepository expressions, LinkPreviews previews,
                       LinkPreviewProperties properties, ApplicationEventPublisher events) {
        this.expressions = expressions;
        this.previews = previews;
        this.properties = properties;
        this.events = events;
    }

    @Async("applicationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(PicturePreviewRequested event) {
        if (properties.enabled()) {
            refresh(event.expressionId());
        }
    }

    /** Wishes written before this feature existed get their picture on the next start. */
    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (properties.enabled()) {
            expressions.findIdsWaitingForLinkPicture()
                    .forEach(id -> events.publishEvent(new PicturePreviewRequested(id)));
        }
    }

    void refresh(Long id) {
        Expression expression = expressions.findById(id).orElse(null);
        if (expression == null) {
            return;
        }
        boolean uploadedByCreator = expression.getPictureUrl() != null && expression.getPictureLink() == null;
        if (uploadedByCreator) {
            return;
        }
        String link = Links.first(expression.getDescription());
        if (link == null) {
            if (expression.getPictureLink() != null) {
                expressions.clearLinkPicture(id);
            }
            return;
        }
        if (link.equals(expression.getPictureLink())) {
            return;
        }
        // Recorded even when the shop gave nothing, so the same link is not fetched again.
        expressions.setLinkPicture(id, previews.lookup(link).pictureUrl(), link);
    }
}
