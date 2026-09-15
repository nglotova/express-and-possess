package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The transition table from the spec, one test per row or refusal.
 */
class ExpressionLifecycleTest extends ApiTest {

    @Test
    void aNewWishIsExpressedAndShowsUpInTheRightLists() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);

        JsonNode wish = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Running shoes, size 41", "wantedBy", "2026-10-20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXPRESSED"))
                .andExpect(jsonPath("$.creator.name").value("Natasha"))
                .andExpect(jsonPath("$.implementer").isEmpty())
                .andExpect(jsonPath("$.canEditWish").value(true))
                .andExpect(jsonPath("$.canTakeCare").value(false))
                .andExpect(jsonPath("$.canDelete").value(true)));

        getAs(natasha, "/api/groups/" + group + "/activity")
                .andExpect(jsonPath("$.groupName").value("Family"))
                .andExpect(jsonPath("$.myExpressions[0].id").value(wish.get("id").asLong()))
                .andExpect(jsonPath("$.notTaken").isEmpty());
        getAs(andrei, "/api/groups/" + group + "/activity")
                .andExpect(jsonPath("$.myExpressions").isEmpty())
                .andExpect(jsonPath("$.notTaken[0].id").value(wish.get("id").asLong()))
                .andExpect(jsonPath("$.notTaken[0].canTakeCare").value(true));

        getAs(natasha, "/api/groups")
                .andExpect(jsonPath("$[0].status").value("WORKING"))
                .andExpect(jsonPath("$[0].hasUntaken").value(true))
                .andExpect(jsonPath("$[0].implementing").value(false));
        getAs(natasha, "/api/groups/" + group)
                .andExpect(jsonPath("$.members[?(@.name == 'Natasha')].hasExpressions").value(true))
                .andExpect(jsonPath("$.members[?(@.name == 'Andrei')].hasExpressions").value(false));
    }

    @Test
    void takeCareThenProvideThenReceive() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Kindle case");

        postAs(natasha, "/api/expressions/" + id + "/take-care").andExpect(status().isForbidden());

        JsonNode taken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.implementer.name").value("Andrei"))
                .andExpect(jsonPath("$.canEditCare").value(true))
                .andExpect(jsonPath("$.canRelease").value(true)));

        getAs(andrei, "/api/groups")
                .andExpect(jsonPath("$[0].implementing").value(true))
                .andExpect(jsonPath("$[0].hasUntaken").value(false));
        getAs(andrei, "/api/groups/" + group + "/activity")
                .andExpect(jsonPath("$.myImplementations[0].id").value(id));

        // The creator's description is locked, the date is not.
        putAs(natasha, "/api/expressions/" + id + "/wish", wishBody("Kindle case, blue", null, taken))
                .andExpect(status().isConflict());
        JsonNode dated = bodyOf(putAs(natasha, "/api/expressions/" + id + "/wish",
                wishBody("Kindle case", "2026-12-01", taken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wantedBy").value("2026-12-01")));

        // Got it is not available before Provided.
        postAs(natasha, "/api/expressions/" + id + "/received").andExpect(status().isConflict());

        JsonNode provided = bodyOf(putAs(andrei, "/api/expressions/" + id + "/care",
                careBody(false, "2026-11-15", true, dated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDED"))
                .andExpect(jsonPath("$.providingBy").value("2026-11-15")));
        getAs(natasha, "/api/expressions/" + id)
                .andExpect(jsonPath("$.canDelete").value(false))
                .andExpect(jsonPath("$.canMarkReceived").value(true));

        deleteAs(natasha, "/api/expressions/" + id).andExpect(status().isConflict());
        postAs(andrei, "/api/expressions/" + id + "/release").andExpect(status().isConflict());
        putAs(andrei, "/api/expressions/" + id + "/care", careBody(true, null, false, provided))
                .andExpect(status().isConflict());

        postAs(natasha, "/api/expressions/" + id + "/received")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_POSSESSION"))
                .andExpect(jsonPath("$.commentsOpen").value(false))
                .andExpect(jsonPath("$.canEditWish").value(false));

        // Read-only from here on, for everyone.
        postAs(andrei, "/api/expressions/" + id + "/comments", Map.of("body", "Enjoy!"))
                .andExpect(status().isConflict());
        deleteAs(natasha, "/api/expressions/" + id).andExpect(status().isConflict());
    }

    @Test
    void releaseReturnsTheWishToExpressedWithTheCareSectionCleared() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);
        long id = wish(natasha, group, "Graphics tablet");

        JsonNode taken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care"));
        putAs(andrei, "/api/expressions/" + id + "/care", careBody(true, "2026-09-28", false, taken))
                .andExpect(status().isOk());
        postAs(lev, "/api/expressions/" + id + "/take-care").andExpect(status().isConflict());
        postAs(lev, "/api/expressions/" + id + "/release").andExpect(status().isForbidden());

        postAs(andrei, "/api/expressions/" + id + "/release")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPRESSED"))
                .andExpect(jsonPath("$.implementer").isEmpty())
                .andExpect(jsonPath("$.providingBy").isEmpty())
                .andExpect(jsonPath("$.incognito").value(false));

        postAs(lev, "/api/expressions/" + id + "/take-care").andExpect(status().isOk());
    }

    @Test
    void theCreatorMayDeleteWhileInProcess() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        long id = wish(natasha, group, "Bicycle pump");
        postAs(andrei, "/api/expressions/" + id + "/take-care");

        deleteAs(andrei, "/api/expressions/" + id).andExpect(status().isForbidden());
        deleteAs(natasha, "/api/expressions/" + id).andExpect(status().isNoContent());
        getAs(andrei, "/api/expressions/" + id).andExpect(status().isNotFound());
    }

    @Test
    void linksAreTheirOwnFieldAndLockWithTheDescription() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);

        postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Mini chainsaw", "links", java.util.List.of("not a link")))
                .andExpect(status().isBadRequest());
        JsonNode wish = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Mini chainsaw", "links",
                        java.util.List.of(" https://www.example.com/chainsaw ", "https://www.example.com/chainsaw",
                                "https://www.example.com/battery")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.links[0]").value("https://www.example.com/chainsaw")));
        long id = wish.get("id").asLong();

        JsonNode taken = bodyOf(postAs(andrei, "/api/expressions/" + id + "/take-care"));
        Map<String, Object> body = wishBody("Mini chainsaw", null, taken);
        body.put("links", java.util.List.of("https://www.example.com/other"));
        putAs(natasha, "/api/expressions/" + id + "/wish", body).andExpect(status().isConflict());
    }

    @Test
    void aStaleVersionIsRefused() throws Exception {
        Member natasha = register("Natasha");
        long group = family(natasha);
        long id = wish(natasha, group, "Headphones");
        JsonNode first = bodyOf(getAs(natasha, "/api/expressions/" + id));

        putAs(natasha, "/api/expressions/" + id + "/wish", wishBody("Headphones, black", null, first))
                .andExpect(status().isOk());
        putAs(natasha, "/api/expressions/" + id + "/wish", wishBody("Headphones, white", null, first))
                .andExpect(status().isConflict());
    }

    @Test
    void wishesAreInvisibleOutsideTheGroupAndFrozenWhenItCloses() throws Exception {
        Member natasha = register("Natasha");
        Member stranger = register("Stranger");
        long group = family(natasha);
        long id = wish(natasha, group, "Socks");

        getAs(stranger, "/api/expressions/" + id).andExpect(status().isNotFound());
        getAs(stranger, "/api/groups/" + group + "/activity").andExpect(status().isNotFound());

        postAs(natasha, "/api/groups/" + group + "/close");
        postAs(natasha, "/api/groups/" + group + "/expressions", Map.of("description", "More socks"))
                .andExpect(status().isConflict());
        deleteAs(natasha, "/api/expressions/" + id).andExpect(status().isConflict());
        getAs(natasha, "/api/expressions/" + id).andExpect(status().isOk());
    }

    @Test
    void aMemberWhoLeavesDropsWhatTheyWereImplementing() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        Member lev = register("Lev");
        long group = family(natasha, andrei, lev);
        long shoes = wish(natasha, group, "Shoes");
        long book = wish(natasha, group, "Book");
        postAs(andrei, "/api/expressions/" + shoes + "/take-care");
        postAs(lev, "/api/expressions/" + book + "/take-care");

        postAs(andrei, "/api/groups/" + group + "/leave").andExpect(status().isNoContent());
        deleteAs(natasha, "/api/groups/" + group + "/members/" + lev.id()).andExpect(status().isNoContent());

        getAs(natasha, "/api/groups/" + group + "/activity")
                .andExpect(jsonPath("$.myExpressions[*].status").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("EXPRESSED"))))
                .andExpect(jsonPath("$.myExpressions[*].implementer").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    }

    @Test
    void aMembersWishListIsReadableByTheOthers() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = family(natasha, andrei);
        wish(natasha, group, "One");
        wish(natasha, group, "Two");

        getAs(andrei, "/api/groups/" + group + "/members/" + natasha.id() + "/expressions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Two"));
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
