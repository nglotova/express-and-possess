package ca.glotov.expresspossess.auth;

import ca.glotov.expresspossess.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class AccountsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registeringLogsTheNewMemberIn() throws Exception {
        String email = unique("natasha");

        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, "Correct-horse-battery-1", "Natasha")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Natasha"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andReturn();

        mvc.perform(get("/api/me").session(sessionOf(result)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void anEmailCanOnlyRegisterOnce() throws Exception {
        String email = unique("twice");
        register(email, "Correct-horse-battery-1");

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), "Another-password-2", "Again")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An account with this email already exists"));
    }

    @Test
    void registrationValidatesItsInput() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("not-an-email", "short", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNewPasswordNeedsLengthACapitalANumberAndASymbol() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(unique("weak"), "longenough", "Weak")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("The password needs a capital letter, a number, a special symbol."));

        String email = unique("strong");
        MockHttpSession session = register(email, "Correct-horse-battery-1");
        mvc.perform(put("/api/me/password").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Correct-horse-battery-1\",\"newPassword\":\"Only letters 12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The password needs a special symbol."));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Correct-horse-battery-1")))
                .andExpect(status().isOk());
    }

    @Test
    void loginRequiresTheRightPassword() throws Exception {
        String email = unique("login");
        register(email, "Correct-horse-battery-1");

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."));
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nobody-" + email, "Correct-horse-battery-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Wrong email or password."));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Correct-horse-battery-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        MockHttpSession session = register(unique("logout"), "Correct-horse-battery-1");

        mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsAnswer401NotARedirect() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aMemberCanChangeTheirNameAndEmailPreference() throws Exception {
        MockHttpSession session = register(unique("profile"), "Correct-horse-battery-1");

        mvc.perform(put("/api/me").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Natasha G.\",\"emailEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Natasha G."))
                .andExpect(jsonPath("$.emailEnabled").value(false));
    }

    @Test
    void changingThePasswordNeedsTheCurrentOne() throws Exception {
        String email = unique("password");
        MockHttpSession session = register(email, "Correct-horse-battery-1");

        mvc.perform(put("/api/me/password").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"nope\",\"newPassword\":\"New-long-password-2\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/me/password").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Correct-horse-battery-1\",\"newPassword\":\"New-long-password-2\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "New-long-password-2")))
                .andExpect(status().isOk());
    }

    @Test
    void stateChangingRequestsNeedTheCsrfToken() throws Exception {
        MockHttpSession session = register(unique("csrf"), "Correct-horse-battery-1");

        mvc.perform(put("/api/me").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Attacker\",\"emailEnabled\":true}"))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession register(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, password, "Someone")))
                .andExpect(status().isCreated())
                .andReturn();
        return sessionOf(result);
    }

    static MockHttpSession sessionOf(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    static String registerJson(String email, String password, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"" + name + "\"}";
    }

    static String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
