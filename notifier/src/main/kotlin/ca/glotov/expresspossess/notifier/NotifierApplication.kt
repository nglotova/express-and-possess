package ca.glotov.expresspossess.notifier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

/**
 * Settings under the `notifier` prefix.
 *
 * @property topic the Kafka topic the API's outbox is relayed to
 * @property mailFrom sender address for every email
 */
@ConfigurationProperties(prefix = "notifier")
data class NotifierProperties(val topic: String, val mailFrom: String)

@SpringBootApplication
@EnableConfigurationProperties(NotifierProperties::class)
class NotifierApplication

fun main(args: Array<String>) {
    runApplication<NotifierApplication>(*args)
}
