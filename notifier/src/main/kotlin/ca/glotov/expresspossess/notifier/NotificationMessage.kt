package ca.glotov.expresspossess.notifier

/**
 * The message the API puts on the topic. Mirrors `NotificationMessage` in the API module;
 * OutboxPublisherTest there and NotifierTest here each check the shape from their side.
 */
data class NotificationMessage(
    val type: String,
    val message: String,
    val link: String,
    val groupId: Long?,
    val expressionId: Long?,
    val recipients: List<Recipient>,
) {
    data class Recipient(val userId: Long, val name: String, val email: String, val emailEnabled: Boolean)
}
