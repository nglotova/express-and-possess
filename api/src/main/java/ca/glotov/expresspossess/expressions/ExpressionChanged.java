package ca.glotov.expresspossess.expressions;

/**
 * Published inside the transaction that changes an expression. The notifications domain
 * listens to it; nothing in this package depends on who is listening.
 *
 * @param type         what happened
 * @param expressionId the expression
 * @param groupId      its group
 * @param actorId      the member who did it
 */
public record ExpressionChanged(Type type, Long expressionId, Long groupId, Long actorId) {

    public enum Type {
        CREATED,
        TAKEN,
        RELEASED,
        PROVIDED,
        RECEIVED,
        DELETED,
        COMMENTED
    }
}
