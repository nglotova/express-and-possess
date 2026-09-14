package ca.glotov.expresspossess.expressions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expression_id", nullable = false)
    private Long expressionId;

    /** Null for a system note, for example an administrator forcing a status. */
    @Column(name = "author_id")
    private Long authorId;

    @Column(nullable = false)
    private String body;

    @Column(name = "system_note", nullable = false)
    private boolean systemNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Comment() {
    }

    public Comment(Long expressionId, Long authorId, String body) {
        this.expressionId = expressionId;
        this.authorId = authorId;
        this.body = body;
    }

    public static Comment systemNote(Long expressionId, String body) {
        Comment note = new Comment(expressionId, null, body);
        note.systemNote = true;
        return note;
    }

    public Long getId() {
        return id;
    }

    public Long getExpressionId() {
        return expressionId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public boolean isSystemNote() {
        return systemNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
