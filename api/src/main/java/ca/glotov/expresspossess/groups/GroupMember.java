package ca.glotov.expresspossess.groups;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "group_members")
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected GroupMember() {
    }

    public GroupMember(Long groupId, Long userId, GroupMemberRole role) {
        this.id = new GroupMemberId(groupId, userId);
        this.role = role;
    }

    public GroupMemberId getId() {
        return id;
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public GroupMemberRole getRole() {
        return role;
    }

    public void setRole(GroupMemberRole role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return role == GroupMemberRole.ADMIN;
    }
}
