package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupsTest extends ApiTest {

    @Test
    void theCreatorBecomesAdminAndSeesTheGroupAsNew() throws Exception {
        Member natasha = register("Natasha");

        JsonNode created = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.myRole").value("ADMIN"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.members[0].name").value("Natasha"))
                .andExpect(jsonPath("$.members[0].role").value("ADMIN")));

        getAs(natasha, "/api/groups")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(created.get("id").asLong()))
                .andExpect(jsonPath("$[0].name").value("Family"))
                .andExpect(jsonPath("$[0].ownerName").value("Natasha"))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    void aGroupIsInvisibleToNonMembers() throws Exception {
        Member natasha = register("Natasha");
        Member stranger = register("Stranger");
        long id = createGroup(natasha, "Family");

        getAs(stranger, "/api/groups/" + id).andExpect(status().isNotFound());
        getAs(stranger, "/api/groups").andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void onlyTheAdminCanEditTheGroup() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long id = createGroup(natasha, "Family");
        addMember(natasha, id, andrei);

        putAs(andrei, "/api/groups/" + id, Map.of("name", "Hacked")).andExpect(status().isForbidden());
        putAs(natasha, "/api/groups/" + id, Map.of("name", "Glotov family"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Glotov family"));
    }

    @Test
    void membersSeeEachOtherButOnlyTheAdminSeesInvitations() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long id = createGroup(natasha, "Family");
        addMember(natasha, id, andrei);
        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", "lev@example.com"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("INVITED"));

        getAs(natasha, "/api/groups/" + id)
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.invitations[0].email").value("lev@example.com"));
        getAs(andrei, "/api/groups/" + id)
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.invitations").isEmpty());
    }

    @Test
    void theAdminCanRemoveAMemberAndAMemberCanLeave() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long id = createGroup(natasha, "Family");
        addMember(natasha, id, andrei);
        addMember(natasha, id, lev);

        deleteAs(natasha, "/api/groups/" + id + "/members/" + andrei.id()).andExpect(status().isNoContent());
        getAs(andrei, "/api/groups/" + id).andExpect(status().isNotFound());

        postAs(lev, "/api/groups/" + id + "/leave").andExpect(status().isNoContent());
        getAs(natasha, "/api/groups/" + id).andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void theAdminMustHandOverBeforeLeaving() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long id = createGroup(natasha, "Family");
        addMember(natasha, id, andrei);

        postAs(natasha, "/api/groups/" + id + "/leave").andExpect(status().isBadRequest());
        deleteAs(natasha, "/api/groups/" + id + "/members/" + natasha.id()).andExpect(status().isBadRequest());

        postAs(natasha, "/api/groups/" + id + "/members/" + andrei.id() + "/make-admin")
                .andExpect(status().isNoContent());
        postAs(natasha, "/api/groups/" + id + "/leave").andExpect(status().isNoContent());

        getAs(andrei, "/api/groups")
                .andExpect(jsonPath("$[0].ownerName").value("Andrei"))
                .andExpect(jsonPath("$[0].myRole").value("ADMIN"));
    }

    @Test
    void closeThenArchiveOrDelete() throws Exception {
        Member natasha = register("Natasha");
        long family = createGroup(natasha, "Family");
        long office = createGroup(natasha, "Office");

        postAs(natasha, "/api/groups/" + family + "/archive").andExpect(status().isConflict());
        deleteAs(natasha, "/api/groups/" + office).andExpect(status().isConflict());

        postAs(natasha, "/api/groups/" + family + "/close").andExpect(status().isNoContent());
        getAs(natasha, "/api/groups/" + family).andExpect(jsonPath("$.status").value("CLOSED"));
        postAs(natasha, "/api/groups/" + family + "/archive").andExpect(status().isNoContent());
        getAs(natasha, "/api/groups/" + family).andExpect(status().isNotFound());

        postAs(natasha, "/api/groups/" + office + "/close").andExpect(status().isNoContent());
        deleteAs(natasha, "/api/groups/" + office).andExpect(status().isNoContent());
        getAs(natasha, "/api/groups").andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void nobodyCanJoinAClosedGroup() throws Exception {
        Member natasha = register("Natasha");
        Member late = register("Late");
        long id = createGroup(natasha, "Family");
        String link = bodyOf(putAs(natasha, "/api/groups/" + id + "/share-link", Map.of("enabled", true)))
                .get("link").asText();
        postAs(natasha, "/api/groups/" + id + "/close").andExpect(status().isNoContent());

        postAs(late, "/api/join/" + tokenOf(link)).andExpect(status().isNotFound());
    }

    long createGroup(Member admin, String name) throws Exception {
        return bodyOf(postAs(admin, "/api/groups", Map.of("name", name)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    void addMember(Member admin, long groupId, Member member) throws Exception {
        postAs(admin, "/api/groups/" + groupId + "/invitations", Map.of("email", member.email()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.outcome").value("ADDED"));
    }

    static String tokenOf(String link) {
        assertThat(link).isNotBlank();
        return link.substring(link.lastIndexOf('/') + 1).replaceFirst("^.*token=", "");
    }
}
