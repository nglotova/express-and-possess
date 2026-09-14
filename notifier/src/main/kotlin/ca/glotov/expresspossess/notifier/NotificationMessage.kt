package ca.glotov.expresspossess.notifier

/**
 * The message the API puts on the topic. Mirrors `NotificationMessage` in the API module;
 * the two are kept in step by the contract test on each side.
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
