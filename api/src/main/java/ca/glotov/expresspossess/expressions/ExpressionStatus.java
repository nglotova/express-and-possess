package ca.glotov.expresspossess.expressions;

/**
 * The four states of a wish, in the order they are normally passed through. The allowed
 * transitions are the methods of {@link ExpressionService}; anything else is a 409.
 */
public enum ExpressionStatus {
    EXPRESSED("Expressed"),
    IN_PROCESS("In Process"),
    PROVIDED("Provided"),
    IN_POSSESSION("In Possession");

    private final String label;

    ExpressionStatus(String label) {
        this.label = label;
    }

    /** The name the pages show, for messages written in words. */
    public String label() {
        return label;
    }
}
