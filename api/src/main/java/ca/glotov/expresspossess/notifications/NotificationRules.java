package ca.glotov.expresspossess.notifications;

import ca.glotov.expresspossess.auth.AccountService;
import ca.glotov.expresspossess.common.Links;
import ca.glotov.expresspossess.expressions.ExpressionChanged;
import ca.glotov.expresspossess.expressions.ExpressionQueries;
import ca.glotov.expresspossess.expressions.ExpressionStatus;
import ca.glotov.expresspossess.groups.GroupChanged;
import ca.glotov.expresspossess.groups.GroupService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Who is told what, for each event: the table in section 12 of the spec. Runs inside the
 * transaction that published the event, so the notification rows commit with the change.
 */
@Component
class NotificationRules {

    private final NotificationService notifications;
    private final ExpressionQueries expressions;
    private final GroupService groups;
    private final AccountService accounts;

    NotificationRules(NotificationService notifications, ExpressionQueries expressions,
                      GroupService groups, AccountService accounts) {
        this.notifications = notifications;
        this.expressions = expressions;
        this.groups = groups;
        this.accounts = accounts;
    }

    @EventListener
    public void on(ExpressionChanged event) {
        ExpressionQueries.Facts wish = expressions.facts(event.expressionId()).orElse(null);
        if (wish == null) {
            return;
        }
        String title = quote(wish.description());
        String actor = accounts.get(event.actorId()).getName();
        // A hidden helper is never named to anyone but themself.
        boolean actorIsHiddenHelper = wish.incognito() && Objects.equals(wish.implementerId(), event.actorId());
        String helper = actorIsHiddenHelper ? "Someone" : actor;

        switch (event.type()) {
            case CREATED -> send(NotificationType.WISH_CREATED, wish,
                    groups.memberIds(wish.groupId()).stream().filter(id -> !id.equals(event.actorId())).toList(),
                    actor + " expressed a wish: " + title);
            case TAKEN -> send(NotificationType.WISH_TAKEN, wish, List.of(wish.creatorId()),
                    helper + " took care of your wish " + title);
            case RELEASED -> send(NotificationType.WISH_RELEASED, wish, List.of(wish.creatorId()),
                    helper + " is no longer taking care of your wish " + title);
            case PROVIDED -> send(NotificationType.WISH_PROVIDED, wish, List.of(wish.creatorId()),
                    helper + " provided your wish " + title);
            case RECEIVED -> {
                if (wish.implementerId() != null) {
                    send(NotificationType.WISH_RECEIVED, wish, List.of(wish.implementerId()),
                            actor + " received " + title + ". Thank you!");
                }
            }
            case DELETED -> {
                if (wish.status() == ExpressionStatus.IN_PROCESS && wish.implementerId() != null) {
                    // The expression is about to disappear, so the row must not point at it.
                    notifications.notify(NotificationType.WISH_DELETED, List.of(wish.implementerId()),
                            actor + " withdrew the wish " + title + " you were taking care of",
                            wish.groupId(), null);
                }
            }
            case COMMENTED -> send(NotificationType.WISH_COMMENTED, wish,
                    Stream.of(wish.creatorId(), wish.implementerId())
                            .filter(Objects::nonNull).filter(id -> !id.equals(event.actorId())).toList(),
                    (actorIsHiddenHelper ? "Anonymous helper" : actor) + " commented on " + title);
        }
    }

    @EventListener
    public void on(GroupChanged event) {
        String group = groups.nameOf(event.groupId());
        String actor = accounts.get(event.actorId()).getName();
        switch (event.type()) {
            case MEMBER_ADDED -> notifications.notify(NotificationType.GROUP_ADDED, List.of(event.userId()),
                    actor + " added you to " + group, event.groupId(), null);
            case MEMBER_REMOVED -> notifications.notify(NotificationType.GROUP_REMOVED, List.of(event.userId()),
                    "You were removed from " + group, event.groupId(), null);
            case CLOSED -> notifications.notify(NotificationType.GROUP_CLOSED,
                    groups.memberIds(event.groupId()).stream().filter(id -> !id.equals(event.actorId())).toList(),
                    actor + " closed the group " + group, event.groupId(), null);
        }
    }

    private void send(NotificationType type, ExpressionQueries.Facts wish, List<Long> recipients, String message) {
        notifications.notify(type, new ArrayList<>(recipients), message, wish.groupId(), wish.id());
    }

    /**
     * The wish's first line without its links. A wish that is nothing but a link is named after
     * the shop, "Amazon link": a cut-off address, or even "amazon.ca", would turn into a wrong
     * link in an email.
     */
    private static String quote(String description) {
        String text = Links.withoutLinks(description);
        String link = Links.first(description);
        String firstLine = !text.isEmpty() ? text.lines().findFirst().orElseThrow()
                : link != null ? Links.shopName(link) + " link"
                : description.strip();
        return "\"" + (firstLine.length() > 60 ? firstLine.substring(0, 57) + "..." : firstLine) + "\"";
    }
}
