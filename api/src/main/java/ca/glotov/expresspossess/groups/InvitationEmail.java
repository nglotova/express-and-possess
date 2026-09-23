package ca.glotov.expresspossess.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One invitation email sent, counted for the daily limit. */
@Entity
@Table(name = "invitation_emails")
class InvitationEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    protected InvitationEmail() {
    }

    InvitationEmail(Long senderId, Instant sentAt) {
        this.senderId = senderId;
        this.sentAt = sentAt;
    }
}
