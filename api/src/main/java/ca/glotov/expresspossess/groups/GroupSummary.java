package ca.glotov.expresspossess.groups;

/**
 * One row of the My Groups page.
 *
 * @param hasUntaken   the flashing "!": expressions nobody has taken care of yet
 * @param implementing the warning sign: the member is implementing something here
 */
public record GroupSummary(Long id, String name, String ownerName, Status status, GroupMemberRole myRole,
                           boolean hasUntaken, boolean implementing) {

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
