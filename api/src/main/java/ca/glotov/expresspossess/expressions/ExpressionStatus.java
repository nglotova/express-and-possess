package ca.glotov.expresspossess.expressions;

/**
 * The four states of a wish, in the order they are normally passed through. The allowed
 * transitions are the methods of {@link ExpressionService}; anything else is a 409.
 */
public enum ExpressionStatus {
    EXPRESSED,
    IN_PROCESS,
    PROVIDED,
    IN_POSSESSION
}
