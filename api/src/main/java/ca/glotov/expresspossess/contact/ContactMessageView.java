package ca.glotov.expresspossess.contact;

import java.time.Instant;

/** A message as the administration page lists it, with who sent it so they can be answered by email. */
public record ContactMessageView(Long id, String senderName, String senderEmail, ContactTopic topic, String body,
                                 String page, Instant createdAt) {
}
