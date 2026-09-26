package ca.glotov.expresspossess.notifications;

import java.util.List;

/**
 * What goes to Kafka for one event. The notifier needs nothing else to deliver it: the
 * text is final and each recipient carries their address and preference.
 *
 * @param link     the page in the app the notification is about
 * @param shopLink the first link in the wish's description, for the email; null when there is none
 */
public record NotificationMessage(NotificationType type,
                                  String message,
                                  String link,
                                  Long groupId,
                                  Long expressionId,
                                  String shopLink,
                                  List<Recipient> recipients) {

    public record Recipient(Long userId, String name, String email, boolean emailEnabled) {
    }
}
