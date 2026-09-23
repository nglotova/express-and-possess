package ca.glotov.expresspossess.groups;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.MutableClock;
import ca.glotov.expresspossess.auth.Role;
import ca.glotov.expresspossess.auth.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationLimitTest extends ApiTest {

    @Autowired
    MutableClock clock;

    @Autowired
    UserRepository users;

    private Member sysadmin;

    @AfterEach
    void restore() throws Exception {
        clock.reset();
        if (sysadmin != null) {
            putAs(sysadmin, "/api/admin/settings", Map.of("invitationsPerDay", 20)).andExpect(status().isOk());
        }
    }

    @Test
    void theAdministratorSetsHowManyInvitationEmailsAMemberMaySendADay() throws Exception {
        sysadmin = makeAdmin(register("Sysadmin"));
        getAs(sysadmin, "/api/admin/settings").andExpect(jsonPath("$.invitationsPerDay").value(20));
        putAs(sysadmin, "/api/admin/settings", Map.of("invitationsPerDay", 0)).andExpect(status().isBadRequest());
        putAs(sysadmin, "/api/admin/settings", Map.of("invitationsPerDay", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitationsPerDay").value(2));

        Member natasha = register("Natasha");
        long id = createGroup(natasha);
        invite(natasha, id, "one").andExpect(status().isAccepted());
        invite(natasha, id, "two").andExpect(status().isAccepted());
        // Cancelling an invitation does not give it back.
        long invitationId = bodyOf(getAs(natasha, "/api/groups/" + id)).get("invitations").get(0).get("id").asLong();
        deleteAs(natasha, "/api/groups/" + id + "/invitations/" + invitationId).andExpect(status().isNoContent());
        invite(natasha, id, "three")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("You have sent 2 invitations in the last 24 hours, which is"
                        + " the limit. Try again tomorrow, or share the group's link instead."));

        // Adding someone who already has an account sends no invitation email and is not counted.
        Member lev = register("Lev");
        postAs(natasha, "/api/groups/" + id + "/invitations", Map.of("email", lev.email()))
                .andExpect(jsonPath("$.outcome").value("ADDED"));

        // Site administrators have no limit.
        long own = createGroup(sysadmin);
        for (String who : new String[] {"a", "b", "c"}) {
            invite(sysadmin, own, who).andExpect(status().isAccepted());
        }

        clock.advance(Duration.ofHours(25));
        invite(natasha, id, "three").andExpect(status().isAccepted());
    }

    private org.springframework.test.web.servlet.ResultActions invite(Member who, long groupId, String name)
            throws Exception {
        return postAs(who, "/api/groups/" + groupId + "/invitations",
                Map.of("email", name + "-" + groupId + "@example.com"));
    }

    private long createGroup(Member admin) throws Exception {
        return bodyOf(postAs(admin, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
    }

    private Member makeAdmin(Member member) throws Exception {
        users.findById(member.id()).ifPresent(u -> {
            u.setRole(Role.ADMIN);
            users.save(u);
        });
        return login(member);
    }
}
