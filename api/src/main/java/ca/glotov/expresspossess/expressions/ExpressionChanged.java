package ca.glotov.expresspossess.expressions;

/**
 * Published inside the transaction that changes an expression. The notifications domain
 * listens to it; nothing in this package depends on who is listening.
 *
 * @param type         what happened
 * @param expressionId the expression
 * @param groupId      its group
 * @param actorId      the member who did it
 * @param formerImplementerId for a change by an admin: who was providing the wish before it
 * @param reason       for a change by an admin: the reason they gave
 */
public record ExpressionChanged(Type type, Long expressionId, Long groupId, Long actorId,
                                Long formerImplementerId, String reason) {

    public ExpressionChanged(Type type, Long expressionId, Long groupId, Long actorId) {
        this(type, expressionId, groupId, actorId, null, null);
    }

    public enum Type {
        CREATED,
        TAKEN,
        RELEASED,
        PROVIDED,
        RECEIVED,
        DELETED,
        COMMENTED,
        /** The site administrator or the group admin set the status. */
        STATUS_SET,
        /** The site administrator or the group admin deleted the wish. */
        REMOVED_BY_ADMIN
    }
}
