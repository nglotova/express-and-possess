package ca.glotov.expresspossess;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal client for Mailpit's HTTP API, enough to read what the application sent.
 */
public class Mailpit {

    public record Message(String to, String subject, String text) {
    }

    private final RestClient client;

    Mailpit(String baseUrl) {
        this.client = RestClient.create(baseUrl);
    }

    public void clear() {
        client.delete().uri("/api/v1/messages").retrieve().toBodilessEntity();
    }

    public List<Message> messagesTo(String address) {
        JsonNode list = client.get().uri("/api/v1/messages").retrieve().body(JsonNode.class);
        List<Message> result = new ArrayList<>();
        for (JsonNode summary : list.path("messages")) {
            String to = summary.path("To").path(0).path("Address").asText();
            if (!to.equalsIgnoreCase(address)) {
                continue;
            }
            JsonNode full = client.get().uri("/api/v1/message/{id}", summary.path("ID").asText())
                    .retrieve().body(JsonNode.class);
            result.add(new Message(to, summary.path("Subject").asText(), full.path("Text").asText()));
        }
        return result;
    }
}
