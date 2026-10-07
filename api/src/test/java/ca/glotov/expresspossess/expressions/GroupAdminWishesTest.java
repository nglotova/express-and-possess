package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The group admin can set any status on a wish in their group and delete it, like the site
 * administrator, but does not learn who a hidden provider is.
 */
class GroupAdminWishesTest extends ApiTest {

    @Test
    void theGroupAdminManagesAWishOfAMemberWhoLeft() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);
        long id = wish(lev, group, "Office chair");
        postAs(andrei, "/api/expressions/" + id + "/take-care", Map.of("incognito", true));
        postAs(lev, "/api/groups/" + group + "/leave").andExpect(status().isNoContent());

        getAs(natasha, "/api/expressions/" + id)
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.implementer.name").value("Incognito"));
        getAs(andrei, "/api/expressions/" + id).andExpect(jsonPath("$.canManage").value(false));

        putAs(natasha, "/api/expressions/" + id + "/manage/status", Map.of("status", "EXPRESSED", "reason", "Lev left the team"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.implementer").isEmpty())
                .andExpect(jsonPath("$.comments[0].body").value("Natasha set the status to Expressed: Lev left the team"));
        // The provider is told; Lev is no longer in the group.
        getAs(andrei, "/api/notifications")
                .andExpect(jsonPath("$.items[0].message").value("Natasha set the status of \"Office chair\" to Expressed: Lev left the team"));

        postAs(natasha, "/api/expressions/" + id + "/manage/delete", Map.of("reason", "Lev left the team"))
                .andExpect(status().isNoContent());
        getAs(natasha, "/api/expressions/" + id).andExpect(status().isNotFound());
    }

    @Test
    void aReceivedWishCanStillBeMovedBack() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);
        long id = wish(lev, group, "Scarf");
        putAs(natasha, "/api/expressions/" + id + "/manage/status",
                Map.of("status", "IN_POSSESSION", "providerId", andrei.id(), "reason", "Given at the party"))
                .andExpect(jsonPath("$.status").value("IN_POSSESSION"));

        putAs(natasha, "/api/expressions/" + id + "/manage/status", Map.of("status", "PROVIDED", "reason", "Not received yet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDED"))
                .andExpect(jsonPath("$.implementer.name").value("Andrei"));
        putAs(natasha, "/api/expressions/" + id + "/manage/status", Map.of("status", "PROVIDED", "reason", "Again"))
                .andExpect(status().isConflict());
        putAs(natasha, "/api/expressions/" + id + "/manage/status",
                Map.of("status", "IN_PROCESS", "providerId", natasha.id(), "reason", "Someone else"))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheAdminOfAnOpenGroupMayManage() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member stranger = register("Stranger");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Kettle");
        Map<String, String> body = Map.of("status", "EXPRESSED", "reason", "Tidy up");

        postAs(andrei, "/api/expressions/" + id + "/manage/delete", Map.of("reason", "Mine now")).andExpect(status().isForbidden());
        postAs(stranger, "/api/expressions/" + id + "/manage/delete", Map.of("reason", "Mine now")).andExpect(status().isNotFound());

        postAs(natasha, "/api/groups/" + group + "/close");
        getAs(natasha, "/api/expressions/" + id).andExpect(jsonPath("$.canManage").value(false));
        postAs(natasha, "/api/expressions/" + id + "/manage/delete", Map.of("reason", "Tidy up")).andExpect(status().isForbidden());
        putAs(natasha, "/api/expressions/" + id + "/manage/status", body).andExpect(status().isForbidden());
    }

    // ---- helpers -------------------------------------------------------------------

    long family(Member admin, Member... others) throws Exception {
        long id = bodyOf(postAs(admin, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        for (Member other : others) {
            postAs(admin, "/api/groups/" + id + "/invitations", Map.of("email", other.email()))
                    .andExpect(status().isAccepted());
        }
        return id;
    }

    long wish(Member creator, long groupId, String description) throws Exception {
        return bodyOf(postAs(creator, "/api/groups/" + groupId + "/expressions", Map.of("description", description))
                .andExpect(status().isCreated())).get("id").asLong();
    }
}
