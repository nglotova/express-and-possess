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
    /** The first link in the wish's description; absent in messages about groups and in older messages. */
    val shopLink: String? = null,
) {
    data class Recipient(val userId: Long, val name: String, val email: String, val emailEnabled: Boolean)
}
