package ca.glotov.expresspossess.notifications;

import ca.glotov.expresspossess.auth.AccountService;
import ca.glotov.expresspossess.auth.User;
import ca.glotov.expresspossess.common.AppProperties;
import ca.glotov.expresspossess.outbox.Outbox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

/**
 * Delivers a notification two ways in one transaction: a row per recipient for the in-app
 * bell, which must be consistent with the change it announces, and one outbox row that
 * the publisher relays to Kafka for the channels outside this process (email, later
 * Telegram).
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final AccountService accounts;
    private final Outbox outbox;
    private final AppProperties properties;
    private final Clock clock;

    NotificationService(NotificationRepository notifications, AccountService accounts, Outbox outbox,
                        AppProperties properties, Clock clock) {
        this.notifications = notifications;
        this.accounts = accounts;
        this.outbox = outbox;
        this.properties = properties;
        this.clock = clock;
    }

    public void notify(NotificationType type, Collection<Long> recipientIds, String message,
                       Long groupId, Long expressionId) {
        String link = expressionId != null
                ? properties.baseUrl() + "/expressions/" + expressionId
                : properties.baseUrl() + "/groups/" + groupId;
        deliver(type, recipientIds, message, groupId, expressionId, link,
                expressionId != null ? "expression" : "group", expressionId != null ? expressionId : groupId);
    }

    /**
     * A notification for the site administrators about something outside any group, such as a
     * Contact us message. It links to the administration page.
     *
     * @param sourceId what it is about, for example the message's id; orders the Kafka messages
     */
    public void notifyAdministrators(NotificationType type, Collection<Long> administratorIds, String message,
                                     Long sourceId) {
        deliver(type, administratorIds, message, null, null, properties.baseUrl() + "/admin", "site", sourceId);
    }

    private void deliver(NotificationType type, Collection<Long> recipientIds, String message, Long groupId,
                         Long expressionId, String link, String aggregateType, Long aggregateId) {
        List<User> recipients = accounts.getAll(recipientIds);
        if (recipients.isEmpty()) {
            return;
        }
        for (User user : recipients) {
            notifications.save(new Notification(user.getId(), type, groupId, expressionId, message));
        }
        outbox.add(aggregateType, aggregateId, type.name(),
                new NotificationMessage(type, message, link, groupId, expressionId,
                        recipients.stream().map(u -> new NotificationMessage.Recipient(
                                u.getId(), u.getName(), u.getEmail(), u.isEmailEnabled())).toList()));
    }

    // ---- the bell ------------------------------------------------------------------

    public record Inbox(long unread, List<NotificationView> items) {
    }

    @Transactional(readOnly = true)
    public Inbox inbox(Long userId) {
        return new Inbox(notifications.countByUserIdAndReadAtIsNull(userId),
                notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                        .map(NotificationView::of).toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadAtIsNull(userId);
    }

    public void markRead(Long userId, Long notificationId) {
        notifications.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .ifPresent(n -> n.markRead(clock.instant()));
    }

    public void markAllRead(Long userId) {
        notifications.findByUserIdAndReadAtIsNull(userId).forEach(n -> n.markRead(clock.instant()));
    }
}
