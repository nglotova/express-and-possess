package ca.glotov.expresspossess.notifier

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistrar
import org.springframework.web.client.RestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.kafka.KafkaContainer
import java.time.Duration

/**
 * A message on the topic, in the shape the API produces, becomes an email in Mailpit for
 * each recipient who wants one.
 */
@SpringBootTest
@Import(NotifierTest.Containers::class)
class NotifierTest {

    @TestConfiguration(proxyBeanMethods = false)
    class Containers {
        @Bean
        @ServiceConnection
        fun kafka(): KafkaContainer = KafkaContainer("apache/kafka-native:3.8.1")

        @Bean
        fun mailpit(): GenericContainer<Nothing> =
            GenericContainer<Nothing>("axllent/mailpit:v1.24").apply { withExposedPorts(1025, 8025) }

        @Bean
        fun mailProperties(mailpit: GenericContainer<*>) = DynamicPropertyRegistrar { registry ->
            registry.add("spring.mail.host") { mailpit.host }
            registry.add("spring.mail.port") { mailpit.getMappedPort(1025) }
        }
    }

    @Autowired
    lateinit var kafka: KafkaTemplate<String, String>

    @Autowired
    lateinit var mailpit: GenericContainer<*>

    @Test
    fun `a message on the topic is emailed to recipients who want email`() {
        val payload = """
            {"type":"WISH_TAKEN",
             "message":"Andrei took care of your wish \"Running shoes\"",
             "link":"http://localhost:5173/expressions/7",
             "groupId":1,"expressionId":7,
             "recipients":[
               {"userId":1,"name":"Natasha","email":"natasha@example.com","emailEnabled":true},
               {"userId":2,"name":"Lev","email":"lev@example.com","emailEnabled":false}
             ]}
        """.trimIndent()

        kafka.send("notifications", "expression:7", payload).get()

        val api = RestClient.create("http://${mailpit.host}:${mailpit.getMappedPort(8025)}")
        await().atMost(Duration.ofSeconds(30)).untilAsserted {
            val messages = api.get().uri("/api/v1/messages").retrieve().body(JsonNode::class.java)!!
            val to = messages["messages"].map { it["To"][0]["Address"].asText() }
            assertThat(to).containsExactly("natasha@example.com")
            val subject = messages["messages"][0]["Subject"].asText()
            assertThat(subject).isEqualTo("Express & Possess: Andrei took care of your wish \"Running shoes\"")
        }
    }
}
