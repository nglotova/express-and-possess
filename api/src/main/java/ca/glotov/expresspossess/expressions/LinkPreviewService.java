package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.common.Links;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a wish's picture in step with the first link in its description, unless the
 * creator uploaded a picture of their own, which is never replaced.
 *
 * <p>Runs on a background thread after the change commits, for links nobody previewed
 * while typing. The picture is written with a single update that leaves the version column
 * alone: its arrival must not make the creator's next Save look like a conflicting edit.
 *
 * <p>A link that gives no picture is tried again later, a few times with growing pauses:
 * shops such as Amazon sometimes answer a server with a robot check instead of the product
 * page. A shop that always refuses stops being asked after the last try.
 */
@Component
class LinkPreviewService {

    /** How long to wait after the first, second and third failed try; after the fourth, give up. */
    static final List<Duration> RETRY_AFTER = List.of(Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4));

    private final ExpressionRepository expressions;
    private final LinkPreviews previews;
    private final LinkPreviewProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    LinkPreviewService(ExpressionRepository expressions, LinkPreviews previews,
                       LinkPreviewProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.expressions = expressions;
        this.previews = previews;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
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
        fetch(id, link);
    }

    /** Tries again the links whose pause after the last failed try is over. */
    @Scheduled(initialDelay = 1, fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void retryFailed() {
        if (!properties.enabled()) {
            return;
        }
        Instant now = clock.instant();
        for (Expression expression : expressions
                .findByPictureUrlIsNullAndPictureLinkIsNotNullAndPictureAttemptsBetween(1, RETRY_AFTER.size())) {
            Duration pause = RETRY_AFTER.get(expression.getPictureAttempts() - 1);
            boolean due = expression.getPictureTriedAt() == null
                    || !now.isBefore(expression.getPictureTriedAt().plus(pause));
            if (due && expression.getPictureLink().equals(Links.first(expression.getDescription()))) {
                fetch(expression.getId(), expression.getPictureLink());
            }
        }
    }

    // Recorded even when the shop gave nothing, so the page stops waiting; retryFailed asks again later.
    private void fetch(Long id, String link) {
        String pictureUrl = previews.lookup(link).pictureUrl();
        if (pictureUrl != null) {
            expressions.setLinkPicture(id, pictureUrl, link, clock.instant());
        } else {
            expressions.recordLinkPictureFailure(id, link, clock.instant());
        }
    }
}
