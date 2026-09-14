package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.auth.User;
import ca.glotov.expresspossess.auth.UserRepository;
import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.common.AppProperties;
import ca.glotov.expresspossess.common.EmailService;
import ca.glotov.expresspossess.common.Tokens;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GroupService {

    public enum InviteOutcome {
        /** The address already had an account; the member was added at once. */
        ADDED,
        /** An invitation email with a one-time link was sent. */
        INVITED
    }

    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final GroupInvitationRepository invitations;
    private final UserRepository users;
    private final EmailService email;
    private final AppProperties properties;
    private final Clock clock;

    GroupService(GroupRepository groups,
                 GroupMemberRepository members,
                 GroupInvitationRepository invitations,
                 UserRepository users,
                 EmailService email,
                 AppProperties properties,
                 Clock clock) {
        this.groups = groups;
        this.members = members;
        this.invitations = invitations;
        this.users = users;
        this.email = email;
        this.properties = properties;
        this.clock = clock;
    }

    // ---- reading -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GroupSummary> listFor(Long userId) {
        return groups.findVisibleTo(userId).stream()
                .map(group -> new GroupSummary(
                        group.getId(),
                        group.getName(),
                        users.findById(group.getOwnerId()).map(User::getName).orElse("?"),
                        GroupSummary.statusOf(group, groups.hasExpressions(group.getId())),
                        memberOf(group.getId(), userId).getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetail get(Long groupId, Long userId) {
        Group group = visibleGroup(groupId);
        GroupMember me = memberOf(groupId, userId);
        boolean admin = me.isAdmin();
        return new GroupDetail(
                group.getId(),
                group.getName(),
                GroupSummary.statusOf(group, groups.hasExpressions(groupId)),
                me.getRole(),
                members.findMembers(groupId),
                admin ? invitations.findByGroupIdAndAcceptedAtIsNull(groupId).stream()
                        .map(i -> new GroupDetail.InvitationView(i.getId(), i.getEmail())).toList()
                        : List.of(),
                group.getShareToken() != null,
                null);
    }

    /** Membership check used by the other domains: 404 if the group is not visible to the user. */
    @Transactional(readOnly = true)
    public GroupMember memberOf(Long groupId, Long userId) {
        return members.findByIdGroupIdAndIdUserId(groupId, userId)
                .orElseThrow(() -> ApiException.notFound("No such group"));
    }

    @Transactional(readOnly = true)
    public Group activeGroupFor(Long groupId, Long userId) {
        memberOf(groupId, userId);
        Group group = visibleGroup(groupId);
        if (!group.isActive()) {
            throw ApiException.conflict("This group is closed");
        }
        return group;
    }

    // ---- creating and editing ------------------------------------------------------

    public GroupDetail create(String name, Long ownerId) {
        Group group = groups.save(new Group(name.trim(), ownerId));
        members.save(new GroupMember(group.getId(), ownerId, GroupMemberRole.ADMIN));
        return get(group.getId(), ownerId);
    }

    public GroupDetail rename(Long groupId, Long adminId, String name) {
        Group group = adminGroup(groupId, adminId);
        group.setName(name.trim());
        return get(groupId, adminId);
    }

    public void close(Long groupId, Long adminId) {
        adminGroup(groupId, adminId).setStatus(GroupStatus.CLOSED);
    }

    public void archive(Long groupId, Long adminId) {
        Group group = adminGroup(groupId, adminId);
        requireClosed(group);
        group.setStatus(GroupStatus.ARCHIVED);
    }

    public void delete(Long groupId, Long adminId) {
        Group group = adminGroup(groupId, adminId);
        requireClosed(group);
        groups.delete(group);
    }

    // ---- membership ----------------------------------------------------------------

    public InviteOutcome invite(Long groupId, Long adminId, String address) {
        Group group = adminGroup(groupId, adminId);
        String inviter = users.findById(adminId).map(User::getName).orElse("A member");
        Optional<User> existing = users.findByEmailIgnoreCase(address);
        if (existing.isPresent()) {
            User user = existing.get();
            if (members.findByIdGroupIdAndIdUserId(groupId, user.getId()).isPresent()) {
                throw ApiException.conflict(user.getName() + " is already a member");
            }
            members.save(new GroupMember(groupId, user.getId(), GroupMemberRole.MEMBER));
            if (user.isEmailEnabled()) {
                email.send(user.getEmail(), "You were added to " + group.getName(),
                        "Hello " + user.getName() + ",\n\n"
                                + inviter + " added you to the group \"" + group.getName() + "\".\n"
                                + properties.baseUrl() + "/groups/" + groupId + "\n");
            }
            return InviteOutcome.ADDED;
        }
        if (invitations.findByGroupIdAndEmailIgnoreCaseAndAcceptedAtIsNull(groupId, address).isPresent()) {
            throw ApiException.conflict("This address has already been invited");
        }
        String token = Tokens.random();
        invitations.save(new GroupInvitation(groupId, address.trim(), Tokens.hash(token), adminId,
                clock.instant().plus(properties.invitationTtl())));
        email.send(address, inviter + " invited you to " + group.getName(),
                "Hello,\n\n"
                        + inviter + " invited you to join the group \"" + group.getName()
                        + "\" on Express & Possess.\n\n"
                        + "Open this link to accept:\n"
                        + properties.baseUrl() + "/invite?token=" + token + "\n\n"
                        + "The link works once and expires in " + properties.invitationTtl().toDays() + " days.\n");
        return InviteOutcome.INVITED;
    }

    public void cancelInvitation(Long groupId, Long adminId, Long invitationId) {
        adminGroup(groupId, adminId);
        GroupInvitation invitation = invitations.findById(invitationId)
                .filter(i -> i.getGroupId().equals(groupId))
                .orElseThrow(() -> ApiException.notFound("No such invitation"));
        invitations.delete(invitation);
    }

    /** What the invitation page shows before the person registers. Public. */
    @Transactional(readOnly = true)
    public InvitationInfo invitationInfo(String token) {
        GroupInvitation invitation = usableInvitation(token);
        Group group = groups.findById(invitation.getGroupId())
                .orElseThrow(() -> ApiException.notFound("This group no longer exists"));
        String inviter = users.findById(invitation.getInvitedBy()).map(User::getName).orElse("A member");
        return new InvitationInfo(group.getName(), invitation.getEmail(), inviter);
    }

    public record InvitationInfo(String groupName, String email, String invitedBy) {
    }

    /** The logged-in user accepts an invitation addressed to their email. */
    public Long acceptInvitation(String token, Long userId) {
        GroupInvitation invitation = usableInvitation(token);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("No such user"));
        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw ApiException.forbidden("This invitation was sent to a different email address");
        }
        join(invitation.getGroupId(), userId);
        invitation.markAccepted(clock.instant());
        return invitation.getGroupId();
    }

    public void remove(Long groupId, Long adminId, Long userId) {
        adminGroup(groupId, adminId);
        if (adminId.equals(userId)) {
            throw ApiException.badRequest("The admin cannot remove themself; hand the group over first");
        }
        members.delete(memberOf(groupId, userId));
    }

    public void leave(Long groupId, Long userId) {
        GroupMember me = memberOf(groupId, userId);
        if (me.isAdmin()) {
            throw ApiException.badRequest("The admin cannot leave; hand the group over first");
        }
        members.delete(me);
    }

    public void makeAdmin(Long groupId, Long adminId, Long userId) {
        Group group = adminGroup(groupId, adminId);
        GroupMember successor = memberOf(groupId, userId);
        GroupMember current = memberOf(groupId, adminId);
        successor.setRole(GroupMemberRole.ADMIN);
        current.setRole(GroupMemberRole.MEMBER);
        group.setOwnerId(userId);
    }

    // ---- share link ----------------------------------------------------------------

    /** Switches the share link on (returning a fresh link) or off (returning null). */
    public String setShareLink(Long groupId, Long adminId, boolean enabled) {
        Group group = adminGroup(groupId, adminId);
        if (!enabled) {
            group.setShareToken(null);
            return null;
        }
        String token = Tokens.random();
        group.setShareToken(Tokens.hash(token));
        return shareLink(token);
    }

    @Transactional(readOnly = true)
    public String shareGroupName(String token) {
        return shareableGroup(token).getName();
    }

    public Long joinByShareLink(String token, Long userId) {
        Group group = shareableGroup(token);
        join(group.getId(), userId);
        return group.getId();
    }

    // ---- helpers -------------------------------------------------------------------

    private void join(Long groupId, Long userId) {
        Group group = visibleGroup(groupId);
        if (!group.isActive()) {
            throw ApiException.conflict("This group is closed");
        }
        if (members.findByIdGroupIdAndIdUserId(groupId, userId).isEmpty()) {
            members.save(new GroupMember(groupId, userId, GroupMemberRole.MEMBER));
        }
    }

    private Group visibleGroup(Long groupId) {
        return groups.findById(groupId)
                .filter(g -> g.getStatus() != GroupStatus.ARCHIVED)
                .orElseThrow(() -> ApiException.notFound("No such group"));
    }

    private Group adminGroup(Long groupId, Long userId) {
        Group group = visibleGroup(groupId);
        if (!memberOf(groupId, userId).isAdmin()) {
            throw ApiException.forbidden("Only the group admin can do this");
        }
        return group;
    }

    private static void requireClosed(Group group) {
        if (group.getStatus() != GroupStatus.CLOSED) {
            throw ApiException.conflict("Close the group first");
        }
    }

    private GroupInvitation usableInvitation(String token) {
        return invitations.findByTokenHash(Tokens.hash(token))
                .filter(i -> i.isUsable(clock.instant()))
                .orElseThrow(() -> ApiException.notFound("This invitation is invalid or has expired"));
    }

    private Group shareableGroup(String token) {
        return groups.findByShareToken(Tokens.hash(token))
                .filter(Group::isActive)
                .orElseThrow(() -> ApiException.notFound("This link is no longer valid"));
    }

    private String shareLink(String token) {
        return properties.baseUrl() + "/join/" + token;
    }
}
