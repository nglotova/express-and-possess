package ca.glotov.expresspossess.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * How other domains put an event in the outbox. Call it inside the transaction that makes
 * the change; the row commits or rolls back together with it.
 */
@Service
public class Outbox {

    private final OutboxEventRepository events;
    private final ObjectMapper json;

    Outbox(OutboxEventRepository events, ObjectMapper json) {
        this.events = events;
        this.json = json;
    }

    public void add(String aggregateType, Long aggregateId, String eventType, Object payload) {
        try {
            events.save(new OutboxEvent(aggregateType, aggregateId, eventType, json.writeValueAsString(payload)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload of " + eventType + " is not serialisable", e);
        }
    }
}
