package ca.glotov.expresspossess.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One line under the bell for one member. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "expression_id")
    private Long expressionId;

    @Column(nullable = false)
    private String message;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(Long userId, NotificationType type, Long groupId, Long expressionId, String message) {
        this.userId = userId;
        this.type = type;
        this.groupId = groupId;
        this.expressionId = expressionId;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getExpressionId() {
        return expressionId;
    }

    public String getMessage() {
        return message;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    void markRead(Instant at) {
        if (readAt == null) {
            readAt = at;
        }
    }
}
