package ca.glotov.expresspossess.auth;

import ca.glotov.expresspossess.common.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * What a new password must contain: at least 8 characters, a capital letter, a number and a
 * special symbol. A special symbol is anything other than a letter, a digit or a space.
 * Existing passwords are not checked, only ones being set.
 */
final class PasswordRule {

    static final int MIN_LENGTH = 8;

    private PasswordRule() {
    }

    /** The parts of the rule the password misses, empty when it passes. */
    static List<String> missing(String password) {
        List<String> missing = new ArrayList<>();
        if (password.length() < MIN_LENGTH) {
            missing.add("at least " + MIN_LENGTH + " characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            missing.add("a capital letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            missing.add("a number");
        }
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            missing.add("a special symbol");
        }
        return missing;
    }

    static void require(String password) {
        List<String> missing = missing(password);
        if (!missing.isEmpty()) {
            throw ApiException.badRequest("The password needs " + String.join(", ", missing) + ".");
        }
    }
}
