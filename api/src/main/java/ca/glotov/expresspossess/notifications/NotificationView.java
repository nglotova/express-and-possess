package ca.glotov.expresspossess.notifications;

import java.time.Instant;

public record NotificationView(Long id, NotificationType type, String message, Long groupId, Long expressionId,
                               boolean read, Instant createdAt) {

    static NotificationView of(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getMessage(), n.getGroupId(), n.getExpressionId(),
                n.getReadAt() != null, n.getCreatedAt());
    }
}
