package ca.glotov.expresspossess.notifier

import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

/** Sends one email per recipient who has email switched on. */
@Component
class EmailChannel(private val mail: JavaMailSender, private val properties: NotifierProperties) {

    fun deliver(notification: NotificationMessage) {
        notification.recipients
            .filter { it.emailEnabled }
            .forEach { recipient ->
                mail.send(SimpleMailMessage().apply {
                    from = properties.mailFrom
                    setTo(recipient.email)
                    subject = "Express & Possess: ${notification.message}"
                    text = """
                        |Hello ${recipient.name},
                        |
                        |${notification.message}
                        |
                        |${links(notification)}
                        |
                        |You can switch these emails off on your profile page.
                        |""".trimMargin()
                })
            }
    }

    /** The link into the app and, for a wish with a shop link, that link too, spelled out in full. */
    private fun links(notification: NotificationMessage): String =
        notification.shopLink?.let { "The wish: ${notification.link}\nIn the shop: $it" } ?: notification.link
}
