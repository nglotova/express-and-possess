package ca.glotov.expresspossess.notifications;

import ca.glotov.expresspossess.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The "who is told" table from section 12 of the spec, checked through the bell.
 */
class NotificationsTest extends ApiTest {

    @Test
    void aNewWishTellsEveryoneElseInTheGroup() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);
        postAs(andrei, "/api/notifications/read-all");
        postAs(lev, "/api/notifications/read-all");

        postAs(natasha, "/api/groups/" + group + "/expressions", Map.of("description", "Running shoes"));

        for (Member other : new Member[] {andrei, lev}) {
            getAs(other, "/api/notifications")
                    .andExpect(jsonPath("$.unread").value(1))
                    .andExpect(jsonPath("$.items[0].type").value("WISH_CREATED"))
                    .andExpect(jsonPath("$.items[0].message").value("Natasha expressed a wish: \"Running shoes\""));
        }
        getAs(natasha, "/api/notifications").andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void theCreatorHearsAboutTakeCareProvidedAndRelease() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Kindle case");

        JsonNode taken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care"));
        latest(natasha).andExpect(jsonPath("$.items[0].message").value("Andrei took care of your wish \"Kindle case\""))
                .andExpect(jsonPath("$.items[0].expressionId").value(id));

        postAs(andrei, "/api/expressions/" + id + "/release");
        latest(natasha).andExpect(jsonPath("$.items[0].type").value("WISH_RELEASED"));

        JsonNode retaken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care"));
        putAs(andrei, "/api/expressions/" + id + "/care", careBody(false, null, true, retaken));
        latest(natasha).andExpect(jsonPath("$.items[0].message").value("Andrei provided your wish \"Kindle case\""));

        postAs(natasha, "/api/expressions/" + id + "/received");
        latest(andrei).andExpect(jsonPath("$.items[0].message").value("Natasha received \"Kindle case\". Thank you!"));
    }

    @Test
    void aHiddenHelperIsNeverNamedToTheCreator() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Headphones");

        postAs(andrei, "/api/expressions/" + id + "/take-care", Map.of("incognito", true))
                .andExpect(status().isOk());
        latest(natasha).andExpect(jsonPath("$.items[0].message").value("Someone took care of your wish \"Headphones\""));

        postAs(andrei, "/api/expressions/" + id + "/comments", Map.of("body", "Which colour?"));
        latest(natasha).andExpect(jsonPath("$.items[0].message").value("Anonymous helper commented on \"Headphones\""));

        postAs(natasha, "/api/expressions/" + id + "/comments", Map.of("body", "Black, please"));
        latest(andrei).andExpect(jsonPath("$.items[0].message").value("Natasha commented on \"Headphones\""));
    }

    @Test
    void deletingAWishInProcessTellsTheImplementer() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Bicycle pump");
        postAs(andrei, "/api/expressions/" + id + "/take-care");

        deleteAs(natasha, "/api/expressions/" + id).andExpect(status().isNoContent());

        latest(andrei)
                .andExpect(jsonPath("$.items[0].type").value("WISH_DELETED"))
                .andExpect(jsonPath("$.items[0].message").value("Natasha withdrew the wish \"Bicycle pump\" you were taking care of"))
                .andExpect(jsonPath("$.items[0].expressionId").isEmpty())
                .andExpect(jsonPath("$.items[0].groupId").value(group));
    }

    @Test
    void groupEventsReachTheMembersConcerned() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);

        latest(andrei).andExpect(jsonPath("$.items[0].message").value("Natasha added you to Family"));

        deleteAs(natasha, "/api/groups/" + group + "/members/" + lev.id());
        latest(lev).andExpect(jsonPath("$.items[0].message").value("You were removed from Family"));

        postAs(natasha, "/api/groups/" + group + "/close");
        latest(andrei).andExpect(jsonPath("$.items[0].message").value("Natasha closed the group Family"));
        latest(natasha).andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void readingClearsTheCount() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        wish(natasha, group, "One");
        wish(natasha, group, "Two");

        JsonNode inbox = bodyOf(getAs(andrei, "/api/notifications").andExpect(jsonPath("$.unread").value(3)));
        long first = inbox.get("items").get(0).get("id").asLong();

        postAs(andrei, "/api/notifications/" + first + "/read").andExpect(status().isNoContent());
        getAs(andrei, "/api/notifications/unread-count").andExpect(jsonPath("$.unread").value(2));

        // Someone else's notification cannot be marked by me.
        postAs(natasha, "/api/notifications/" + first + "/read").andExpect(status().isNoContent());
        getAs(andrei, "/api/notifications/unread-count").andExpect(jsonPath("$.unread").value(2));

        postAs(andrei, "/api/notifications/read-all").andExpect(status().isNoContent());
        getAs(andrei, "/api/notifications")
                .andExpect(jsonPath("$.unread").value(0))
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    // ---- helpers -------------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions latest(Member who) throws Exception {
        return getAs(who, "/api/notifications").andExpect(status().isOk());
    }

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
