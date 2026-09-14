package ca.glotov.expresspossess.notifier

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Reads the topic and hands each message to the channels. Delivery from the API is
 * at-least-once, so a repeated message sends a repeated email; for a family that is the
 * cheaper failure compared with a lost one.
 */
@Component
class NotificationConsumer(private val json: ObjectMapper, private val email: EmailChannel) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${notifier.topic}"])
    fun on(payload: String) {
        val notification = json.readValue<NotificationMessage>(payload)
        log.info("{} to {} recipient(s): {}", notification.type, notification.recipients.size, notification.message)
        email.deliver(notification)
    }
}
