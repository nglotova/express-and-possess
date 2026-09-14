package ca.glotov.expresspossess.groups;

import java.util.List;

/**
 * The group page. {@code invitations} and {@code shareLink} are only filled in for the
 * group's admin; other members receive an empty list and null.
 */
public record GroupDetail(Long id,
                          String name,
                          GroupSummary.Status status,
                          GroupMemberRole myRole,
                          List<MemberView> members,
                          List<InvitationView> invitations,
                          boolean shareLinkEnabled,
                          String shareLink) {

    public record InvitationView(Long id, String email) {
    }
}
