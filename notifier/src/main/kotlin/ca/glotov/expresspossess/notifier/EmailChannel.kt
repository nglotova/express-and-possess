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
                        |${notification.link}
                        |
                        |You can switch these emails off on your profile page.
                        |""".trimMargin()
                })
            }
    }
}
