package ca.glotov.expresspossess.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** A message from a member to the site administrators. */
@Entity
@Table(name = "contact_messages")
class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactTopic topic;

    @Column(nullable = false)
    private String body;

    /** The page the member was on when they opened the form, which helps with a reported problem. */
    private String page;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContactMessage() {
    }

    ContactMessage(Long senderId, ContactTopic topic, String body, String page, Instant createdAt) {
        this.senderId = senderId;
        this.topic = topic;
        this.body = body;
        this.page = page;
        this.createdAt = createdAt;
    }

    Long getId() {
        return id;
    }

    Long getSenderId() {
        return senderId;
    }

    ContactTopic getTopic() {
        return topic;
    }

    String getBody() {
        return body;
    }

    String getPage() {
        return page;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
