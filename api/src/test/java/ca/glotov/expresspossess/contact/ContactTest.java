package ca.glotov.expresspossess.contact;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactTest extends ApiTest {

    @Autowired
    UserRepository users;

    @Test
    void aMembersMessageReachesTheAdministratorsWithWhoSentIt() throws Exception {
        Member admin = makeAdmin(register("Sysadmin"));
        Member lev = register("Lev");
        String body = "The picture does not load on my phone " + UUID.randomUUID();

        postAs(lev, "/api/contact", Map.of("topic", "PROBLEM", "body", body, "page", "/expressions/7"))
                .andExpect(status().isNoContent());

        JsonNode message = StreamSupport.stream(bodyOf(getAs(admin, "/api/admin/messages")).spliterator(), false)
                .filter(m -> m.get("body").asText().equals(body)).findFirst().orElseThrow();
        assertThat(message.get("senderName").asText()).isEqualTo("Lev");
        assertThat(message.get("senderEmail").asText()).isEqualTo(lev.email());
        assertThat(message.get("topic").asText()).isEqualTo("PROBLEM");
        assertThat(message.get("page").asText()).isEqualTo("/expressions/7");

        getAs(admin, "/api/notifications")
                .andExpect(jsonPath("$.items[0].type").value("CONTACT_MESSAGE"))
                .andExpect(jsonPath("$.items[0].message").value("Lev wrote about a problem: " + body));

        // Members cannot read the messages.
        getAs(lev, "/api/admin/messages").andExpect(status().isForbidden());
    }

    @Test
    void onlyLoggedInMembersCanWriteAndOnlyFiveTimesADay() throws Exception {
        mvc.perform(post("/api/contact").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"OTHER\",\"body\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());

        Member mila = register("Mila");
        postAs(mila, "/api/contact", Map.of("topic", "OTHER", "body", "  ")).andExpect(status().isBadRequest());
        for (int i = 1; i <= 5; i++) {
            postAs(mila, "/api/contact", Map.of("topic", "SUGGESTION", "body", "Idea " + i))
                    .andExpect(status().isNoContent());
        }
        postAs(mila, "/api/contact", Map.of("topic", "SUGGESTION", "body", "Idea 6"))
                .andExpect(status().isTooManyRequests());
    }

    private Member makeAdmin(Member member) throws Exception {
        users.findById(member.id()).ifPresent(u -> {
            u.setRole(Role.ADMIN);
            users.save(u);
        });
        return login(member);
    }
}
