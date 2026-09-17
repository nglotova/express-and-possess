package ca.glotov.expresspossess.auth;

import ca.glotov.expresspossess.Mailpit;
import ca.glotov.expresspossess.MutableClock;
import ca.glotov.expresspossess.TestcontainersConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.glotov.expresspossess.auth.AccountsTest.loginJson;
import static ca.glotov.expresspossess.auth.AccountsTest.registerJson;
import static ca.glotov.expresspossess.auth.AccountsTest.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class PasswordResetTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("/reset-password\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    Mailpit mailpit;

    @Autowired
    MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void theEmailedLinkResetsThePasswordOnce() throws Exception {
        String email = unique("reset");
        register(email, "Old-password-here-1");

        requestReset(email);
        String token = tokenFromEmail(email);

        confirmReset(token, "Brand-new-password-2").andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Old-password-here-1")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Brand-new-password-2")))
                .andExpect(status().isOk());

        confirmReset(token, "Third-password-3").andExpect(status().isBadRequest());
    }

    @Test
    void anExpiredLinkIsRefused() throws Exception {
        String email = unique("expired");
        register(email, "Old-password-here-1");

        requestReset(email);
        String token = tokenFromEmail(email);
        clock.advance(Duration.ofHours(2));

        confirmReset(token, "Brand-new-password-2").andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoMail() throws Exception {
        String email = unique("nobody");

        requestReset(email);

        assertThat(mailpit.messagesTo(email)).isEmpty();
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, password, "Someone")))
                .andExpect(status().isCreated());
    }

    private void requestReset(String email) throws Exception {
        mvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isAccepted());
    }

    private org.springframework.test.web.servlet.ResultActions confirmReset(String token, String newPassword) throws Exception {
        return mvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private String tokenFromEmail(String email) {
        List<Mailpit.Message> messages = await().atMost(Duration.ofSeconds(10))
                .until(() -> mailpit.messagesTo(email), list -> !list.isEmpty());
        Mailpit.Message message = messages.get(0);
        assertThat(message.subject()).isEqualTo("Reset your Express & Possess password");
        Matcher matcher = TOKEN_IN_LINK.matcher(message.text());
        assertThat(matcher.find()).as("reset link in email body").isTrue();
        return matcher.group(1);
    }
}
