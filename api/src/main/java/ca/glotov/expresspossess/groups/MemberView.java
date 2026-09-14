package ca.glotov.expresspossess.groups;

public record MemberView(Long userId, String name, String email, GroupMemberRole role) {
}
