package ca.glotov.expresspossess.expressions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expressions")
public class Expression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "implementer_id")
    private Long implementerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpressionStatus status = ExpressionStatus.EXPRESSED;

    @Column(nullable = false)
    private String description;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "wanted_by")
    private LocalDate wantedBy;

    @Column(name = "providing_by")
    private LocalDate providingBy;

    @Column(nullable = false)
    private boolean incognito;

    /** Optimistic lock for ordinary edits. Take Care does not rely on it; see the repository. */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Expression() {
    }

    public Expression(Long groupId, Long creatorId, String description, LocalDate wantedBy) {
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.description = description;
        this.wantedBy = wantedBy;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public Long getImplementerId() {
        return implementerId;
    }

    public ExpressionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public LocalDate getWantedBy() {
        return wantedBy;
    }

    public LocalDate getProvidingBy() {
        return providingBy;
    }

    public boolean isIncognito() {
        return incognito;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isCreator(Long userId) {
        return creatorId.equals(userId);
    }

    public boolean isImplementer(Long userId) {
        return implementerId != null && implementerId.equals(userId);
    }

    public boolean is(ExpressionStatus expected) {
        return status == expected;
    }

    // Mutations are named after what happens on the page, not after fields.

    void editWish(String description, LocalDate wantedBy) {
        this.description = description;
        this.wantedBy = wantedBy;
        touch();
    }

    void setPicture(String pictureUrl) {
        this.pictureUrl = pictureUrl;
        touch();
    }

    void editCare(boolean incognito, LocalDate providingBy) {
        this.incognito = incognito;
        this.providingBy = providingBy;
        touch();
    }

    void markProvided() {
        this.status = ExpressionStatus.PROVIDED;
        touch();
    }

    void markReceived() {
        this.status = ExpressionStatus.IN_POSSESSION;
        touch();
    }

    void release() {
        this.implementerId = null;
        this.incognito = false;
        this.providingBy = null;
        this.status = ExpressionStatus.EXPRESSED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
