package ca.glotov.expresspossess.groups;

/**
 * Published inside the transaction in which a member leaves or is removed, so other
 * domains can tidy up what that member was doing in the group.
 */
public record MemberLeftEvent(Long groupId, Long userId) {
}
