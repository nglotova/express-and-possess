package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncognitoTest extends ApiTest {

    @Autowired
    UserRepository users;

    @Test
    void anIncognitoImplementerIsHiddenEverywhereExceptFromThemselvesAndTheSystemAdmin() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        Member admin = register("Sysadmin");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        for (Member m : new Member[] {andrei, lev, admin}) {
            postAs(natasha, "/api/groups/" + group + "/invitations", Map.of("email", m.email()));
        }
        users.findById(admin.id()).ifPresent(u -> {
            u.setRole(Role.ADMIN);
            users.save(u);
        });
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Kindle case"))).get("id").asLong();

        JsonNode taken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care"));
        putAs(andrei, "/api/expressions/" + id + "/care", careBody(true, null, false, taken))
                .andExpect(jsonPath("$.incognito").value(true))
                .andExpect(jsonPath("$.implementer.name").value("Andrei"));
        postAs(andrei, "/api/expressions/" + id + "/comments", Map.of("body", "Does it have to be blue?"));
        postAs(natasha, "/api/expressions/" + id + "/comments", Map.of("body", "Any dark colour."));

        // The creator and other members see a masked implementer and masked comments.
        for (Member viewer : new Member[] {natasha, lev}) {
            getAs(viewer, "/api/expressions/" + id)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.incognito").value(false))
                    .andExpect(jsonPath("$.implementer.id").isEmpty())
                    .andExpect(jsonPath("$.implementer.name").value("Incognito"))
                    .andExpect(jsonPath("$.comments[0].author.name").value("Anonymous helper"))
                    .andExpect(jsonPath("$.comments[0].author.id").isEmpty())
                    .andExpect(jsonPath("$.comments[1].author.name").value("Natasha"));
            getAs(viewer, "/api/groups/" + group + "/activity")
                    .andExpect(jsonPath("$..implementer.name").value(
                            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("Incognito"))));
        }

        // The implementer and a site administrator see the real name.
        for (Member viewer : new Member[] {andrei, admin}) {
            getAs(viewer, "/api/expressions/" + id)
                    .andExpect(jsonPath("$.incognito").value(true))
                    .andExpect(jsonPath("$.implementer.name").value("Andrei"))
                    .andExpect(jsonPath("$.comments[0].author.name").value("Andrei"));
        }
    }
}
