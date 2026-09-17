package ca.glotov.expresspossess.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordRuleTest {

    @Test
    void namesEveryMissingPart() {
        assertThat(PasswordRule.missing("abc")).containsExactly(
                "at least 8 characters", "a capital letter", "a number", "a special symbol");
        assertThat(PasswordRule.missing("Abcdefg1")).containsExactly("a special symbol");
        assertThat(PasswordRule.missing("Abc def 1")).containsExactly("a special symbol");
        assertThat(PasswordRule.missing("Abcdef1!")).isEmpty();
        assertThat(PasswordRule.missing("Пароль-1")).isEmpty();
    }
}
