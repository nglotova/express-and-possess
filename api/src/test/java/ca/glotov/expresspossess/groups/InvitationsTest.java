package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.Mailpit;
import ca.glotov.expresspossess.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationsTest extends ApiTest {

    private static final Pattern INVITE_TOKEN = Pattern.compile("/invite\\?token=([A-Za-z0-9_-]+)");

    @Autowired
    MutableClock clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void anExistingAccountIsAddedAtOnceAndEmailed() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long id = createGroup(natasha);

        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", andrei.email().toUpperCase()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("ADDED"));

        getAs(andrei, "/api/groups/" + id).andExpect(status().isOk());
        List<Mailpit.Message> mail = await().atMost(Duration.ofSeconds(10))
                .until(() -> mailpit.messagesTo(andrei.email()), l -> !l.isEmpty());
        assertThat(mail.get(0).subject()).isEqualTo("You were added to Family");

        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", andrei.email()))
                .andExpect(status().isConflict());
    }

    @Test
    void aNewAddressGetsALinkAndJoinsAfterRegistering() throws Exception {
        Member natasha = register("Natasha");
        long id = createGroup(natasha);
        String levEmail = "lev-" + id + "@example.com";

        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", levEmail))
                .andExpect(jsonPath("$.outcome").value("INVITED"));
        String token = tokenFromEmail(levEmail);

        // The invitation page is public and shows what the person is joining.
        mvc.perform(get("/api/invitations/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Family"))
                .andExpect(jsonPath("$.email").value(levEmail))
                .andExpect(jsonPath("$.invitedBy").value("Natasha"));

        MockHttpSession lev = registerWithEmail(levEmail, "Lev");
        mvc.perform(post("/api/invitations/" + token + "/accept").with(csrf()).session(lev))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(id));

        mvc.perform(get("/api/groups/" + id).session(lev))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
        getAs(natasha, "/api/groups/" + id).andExpect(jsonPath("$.invitations").isEmpty());

        // The link works once.
        mvc.perform(post("/api/invitations/" + token + "/accept").with(csrf()).session(lev))
                .andExpect(status().isNotFound());
    }

    @Test
    void anInvitationOnlyWorksForTheAddressItWasSentTo() throws Exception {
        Member natasha = register("Natasha");
        Member impostor = register("Impostor");
        long id = createGroup(natasha);
        String invited = "mila-" + id + "@example.com";
        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", invited));
        String token = tokenFromEmail(invited);

        postAs(impostor, "/api/invitations/" + token + "/accept").andExpect(status().isForbidden());
    }

    @Test
    void anInvitationExpiresAndCanBeCancelled() throws Exception {
        Member natasha = register("Natasha");
        long id = createGroup(natasha);
        String expired = "old-" + id + "@example.com";
        String cancelled = "gone-" + id + "@example.com";
        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", expired));
        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", cancelled));
        String expiredToken = tokenFromEmail(expired);
        String cancelledToken = tokenFromEmail(cancelled);

        long invitationId = bodyOf(getAs(natasha, "/api/groups/" + id))
                .get("invitations").findValuesAsText("id").stream()
                .map(Long::parseLong).max(Long::compare).orElseThrow();
        deleteAs(natasha, "/api/groups/" + id + "/invitations/" + invitationId).andExpect(status().isNoContent());
        mvc.perform(get("/api/invitations/" + cancelledToken)).andExpect(status().isNotFound());

        clock.advance(Duration.ofDays(8));
        mvc.perform(get("/api/invitations/" + expiredToken)).andExpect(status().isNotFound());
    }

    @Test
    void aShareLinkLetsAnyoneJoinUntilItIsSwitchedOff() throws Exception {
        Member natasha = register("Natasha");
        Member cousin = register("Cousin");
        Member latecomer = register("Latecomer");
        long id = createGroup(natasha);

        String link = bodyOf(putAs(natasha, "/api/groups/" + id + "/share-link", Map.of("enabled", true))
                .andExpect(jsonPath("$.enabled").value(true)))
                .get("link").asText();
        String token = GroupsTest.tokenOf(link);

        mvc.perform(get("/api/join/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Family"));
        postAs(cousin, "/api/join/" + token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(id));
        getAs(cousin, "/api/groups/" + id).andExpect(jsonPath("$.myRole").value("MEMBER"));

        putAs(natasha, "/api/groups/" + id + "/share-link", Map.of("enabled", false))
                .andExpect(jsonPath("$.enabled").value(false));
        postAs(latecomer, "/api/join/" + token).andExpect(status().isNotFound());
    }

    private long createGroup(Member admin) throws Exception {
        return bodyOf(postAs(admin, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
    }

    private MockHttpSession registerWithEmail(String email, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", "correct horse battery", "name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private String tokenFromEmail(String email) {
        List<Mailpit.Message> messages = await().atMost(Duration.ofSeconds(10))
                .until(() -> mailpit.messagesTo(email), l -> !l.isEmpty());
        Matcher matcher = INVITE_TOKEN.matcher(messages.get(0).text());
        assertThat(matcher.find()).as("invitation link in email").isTrue();
        return matcher.group(1);
    }
}
