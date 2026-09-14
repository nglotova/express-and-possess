package ca.glotov.expresspossess.notifications;

import java.util.List;

/**
 * What goes to Kafka for one event. The notifier needs nothing else to deliver it: the
 * text is final and each recipient carries their address and preference.
 */
public record NotificationMessage(NotificationType type,
                                  String message,
                                  String link,
                                  Long groupId,
                                  Long expressionId,
                                  List<Recipient> recipients) {

    public record Recipient(Long userId, String name, String email, boolean emailEnabled) {
    }
}
