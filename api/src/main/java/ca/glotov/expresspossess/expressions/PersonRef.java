package ca.glotov.expresspossess.expressions;

/**
 * A member as shown on an expression. {@code id} is null when the person is hidden from
 * the viewer, in which case {@code name} is "Incognito" or "Anonymous helper".
 */
public record PersonRef(Long id, String name) {

    static final PersonRef INCOGNITO = new PersonRef(null, "Incognito");
    static final PersonRef ANONYMOUS_HELPER = new PersonRef(null, "Anonymous helper");
}
