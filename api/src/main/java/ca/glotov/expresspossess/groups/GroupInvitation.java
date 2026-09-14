package ca.glotov.expresspossess.groups;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An emailed invitation for an address that has no account yet. The person joins the
 * group when they open the link and register with that address.
 */
@Entity
@Table(name = "group_invitations")
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "invited_by", nullable = false)
    private Long invitedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected GroupInvitation() {
    }

    public GroupInvitation(Long groupId, String email, String tokenHash, Long invitedBy, Instant expiresAt) {
        this.groupId = groupId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getEmail() {
        return email;
    }

    public Long getInvitedBy() {
        return invitedBy;
    }

    public boolean isUsable(Instant now) {
        return acceptedAt == null && now.isBefore(expiresAt);
    }

    public void markAccepted(Instant now) {
        this.acceptedAt = now;
    }
}
