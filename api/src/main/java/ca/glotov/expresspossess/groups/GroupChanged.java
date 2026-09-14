package ca.glotov.expresspossess.groups;

/**
 * Published inside the transaction that changes a group's membership or status. The
 * notifications domain listens to it.
 *
 * @param type    what happened
 * @param groupId the group
 * @param userId  the member concerned, or null when the whole group is
 * @param actorId who did it
 */
public record GroupChanged(Type type, Long groupId, Long userId, Long actorId) {

    public enum Type {
        MEMBER_ADDED,
        MEMBER_REMOVED,
        CLOSED
    }
}
