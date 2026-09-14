package ca.glotov.expresspossess.outbox;

import ca.glotov.expresspossess.common.AppProperties;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * Relays outbox rows to Kafka. Each row is sent, acknowledged by the broker, and only then
 * marked as published, so a crash between the two steps re-sends the row rather than losing
 * it: delivery is at-least-once and consumers must tolerate a repeat. A failed send stops
 * the batch so that rows of one aggregate keep their order.
 *
 * <p>One instance of the API runs the publisher; running several would need a row lock
 * ({@code SELECT ... FOR UPDATE SKIP LOCKED}), which a family-sized deployment does not.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository events;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final Clock clock;

    OutboxPublisher(OutboxEventRepository events, KafkaTemplate<String, String> kafka,
                    AppProperties properties, Clock clock) {
        this.events = events;
        this.kafka = kafka;
        this.topic = properties.notificationsTopic();
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.outbox-poll-interval}")
    public void publishPending() {
        for (OutboxEvent event : events.findTop100ByPublishedAtIsNullOrderById()) {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.key(), event.getPayload());
            record.headers()
                    .add(new RecordHeader("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("outboxId", event.getId().toString().getBytes(StandardCharsets.UTF_8)));
            try {
                kafka.send(record).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Could not publish outbox event {} ({}); will retry", event.getId(), event.getEventType(), e);
                return;
            }
            events.markPublished(event.getId(), clock.instant());
        }
    }
}
