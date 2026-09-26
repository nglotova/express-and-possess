package ca.glotov.expresspossess.outbox;

import ca.glotov.expresspossess.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * A change in the application ends up as a message on the Kafka topic, carrying everything
 * the notifier needs, and the outbox row is stamped as published.
 */
class OutboxPublisherTest extends ApiTest {

    @Autowired
    KafkaContainer kafka;

    @Autowired
    OutboxEventRepository outbox;

    @Test
    void aWishTakenBecomesAKafkaMessageWithItsRecipientsAndShopLink() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        postAs(natasha, "/api/groups/" + group + "/invitations", Map.of("email", andrei.email()));
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Running shoes https://shop.example/shoes?size=39"))).get("id").asLong();

        try (Consumer<String, String> consumer = consumer()) {
            consumer.subscribe(List.of("notifications"));

            postAs(andrei, "/api/expressions/" + id + "/take-care");

            List<ConsumerRecord<String, String>> received = new ArrayList<>();
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(received::add);
                assertThat(received).anyMatch(r -> r.key().equals("expression:" + id)
                        && header(r, "eventType").equals("WISH_TAKEN"));
            });

            ConsumerRecord<String, String> taken = received.stream()
                    .filter(r -> header(r, "eventType").equals("WISH_TAKEN")).findFirst().orElseThrow();
            JsonNode payload = json.readTree(taken.value());
            assertThat(payload.get("type").asText()).isEqualTo("WISH_TAKEN");
            assertThat(payload.get("message").asText()).isEqualTo("Andrei took care of your wish \"Running shoes\"");
            assertThat(payload.get("link").asText()).endsWith("/expressions/" + id);
            assertThat(payload.get("shopLink").asText()).isEqualTo("https://shop.example/shoes?size=39");
            assertThat(payload.get("recipients")).hasSize(1);
            assertThat(payload.get("recipients").get(0).get("email").asText()).isEqualTo(natasha.email());
            assertThat(payload.get("recipients").get(0).get("emailEnabled").asBoolean()).isTrue();
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(outbox.findAll()).filteredOn(e -> e.key().equals("expression:" + id))
                        .allMatch(e -> e.getPublishedAt() != null));
    }

    private Consumer<String, String> consumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "test-" + System.nanoTime(), "true");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<String, String>(props).createConsumer();
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
