package ca.glotov.expresspossess.groups;

/**
 * One row of the My Groups page. The attention flags (untaken expressions, expressions the
 * member is implementing) arrive with the expressions milestone.
 */
public record GroupSummary(Long id, String name, String ownerName, Status status, GroupMemberRole myRole) {

    /** The status as the spec names it. */
    public enum Status {
        NEW,
        WORKING,
        CLOSED
    }

    static Status statusOf(Group group, boolean hasExpressions) {
        if (group.getStatus() == GroupStatus.CLOSED) {
            return Status.CLOSED;
        }
        return hasExpressions ? Status.WORKING : Status.NEW;
    }
}
