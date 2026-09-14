package ca.glotov.expresspossess.admin;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTest extends ApiTest {

    @Autowired
    UserRepository users;

    @Test
    void onlyAnAdministratorReachesTheAdminApi() throws Exception {
        Member member = register("Member");
        getAs(member, "/api/admin/users").andExpect(status().isForbidden());
        getAs(member, "/api/admin/groups").andExpect(status().isForbidden());
    }

    @Test
    void anAdministratorCanSearchDisableAndPromoteUsers() throws Exception {
        Member admin = makeAdmin(register("Sysadmin"));
        Member lev = register("Lev");
        Member mila = register("Mila");

        getAs(admin, "/api/admin/users?q=" + lev.email())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Lev"))
                .andExpect(jsonPath("$[0].enabled").value(true));

        putAs(admin, "/api/admin/users/" + lev.id(), Map.of("enabled", false, "role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        // A disabled account can no longer log in.
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", lev.email(), "password", "correct horse battery"))))
                .andExpect(status().isUnauthorized());

        putAs(admin, "/api/admin/users/" + mila.id(), Map.of("enabled", true, "role", "ADMIN"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        getAs(mila, "/api/admin/users").andExpect(status().isForbidden());
        getAs(login(mila), "/api/admin/users").andExpect(status().isOk());

        // No locking yourself out.
        putAs(admin, "/api/admin/users/" + admin.id(), Map.of("enabled", false, "role", "ADMIN"))
                .andExpect(status().isBadRequest());
        putAs(admin, "/api/admin/users/" + admin.id(), Map.of("enabled", true, "role", "MEMBER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anAdministratorSeesArchivedGroupsAndCanRestoreOrDeleteThem() throws Exception {
        Member admin = makeAdmin(register("Sysadmin"));
        Member natasha = register("Natasha");
        long family = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        long office = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Office"))).get("id").asLong();
        postAs(natasha, "/api/groups/" + family + "/close");
        postAs(natasha, "/api/groups/" + family + "/archive");
        getAs(natasha, "/api/groups").andExpect(jsonPath("$.length()").value(1));

        getAs(admin, "/api/admin/groups")
                .andExpect(jsonPath("$[?(@.id == " + family + ")].status").value("ARCHIVED"))
                .andExpect(jsonPath("$[?(@.id == " + office + ")].status").value("ACTIVE"));
        getAs(admin, "/api/admin/groups/" + family)
                .andExpect(jsonPath("$.members[0].name").value("Natasha"));

        postAs(admin, "/api/admin/groups/" + office + "/restore").andExpect(status().isConflict());
        postAs(admin, "/api/admin/groups/" + family + "/restore").andExpect(status().isNoContent());
        getAs(natasha, "/api/groups").andExpect(jsonPath("$[?(@.id == " + family + ")].status").value("CLOSED"));

        deleteAs(admin, "/api/admin/groups/" + office).andExpect(status().isNoContent());
        getAs(natasha, "/api/groups/" + office).andExpect(status().isNotFound());
    }

    @Test
    void aStuckExpressionCanBeForcedWithAReasonEveryoneCanRead() throws Exception {
        Member admin = makeAdmin(register("Sysadmin"));
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        postAs(natasha, "/api/groups/" + group + "/invitations", Map.of("email", andrei.email()));
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions", Map.of("description", "Shoes"))).get("id").asLong();
        postAs(andrei, "/api/expressions/" + id + "/take-care", Map.of("incognito", true));

        // The administrator sees the real implementer.
        getAs(admin, "/api/admin/expressions/" + id).andExpect(jsonPath("$.implementer.name").value("Andrei"));
        getAs(admin, "/api/admin/groups/" + group).andExpect(jsonPath("$.expressions[0].implementer.name").value("Andrei"));

        putAs(admin, "/api/admin/expressions/" + id + "/status", Map.of("status", "PROVIDED", "reason", "Andrei says it arrived"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDED"));
        getAs(natasha, "/api/expressions/" + id)
                .andExpect(jsonPath("$.status").value("PROVIDED"))
                .andExpect(jsonPath("$.comments[0].systemNote").value(true))
                .andExpect(jsonPath("$.comments[0].body").value("Sysadmin set the status to PROVIDED: Andrei says it arrived"));

        putAs(admin, "/api/admin/expressions/" + id + "/status", Map.of("status", "EXPRESSED", "reason", "Never mind"))
                .andExpect(jsonPath("$.implementer").isEmpty());
        putAs(admin, "/api/admin/expressions/" + id + "/status", Map.of("status", "IN_PROCESS", "reason", "Oops"))
                .andExpect(status().isConflict());
        putAs(admin, "/api/admin/expressions/" + id + "/status", Map.of("status", "PROVIDED", "reason", ""))
                .andExpect(status().isBadRequest());
    }

    /** The role is read at login, so a promoted account needs a fresh session. */
    private Member makeAdmin(Member member) throws Exception {
        users.findById(member.id()).ifPresent(u -> {
            u.setRole(Role.ADMIN);
            users.save(u);
        });
        return login(member);
    }
}
