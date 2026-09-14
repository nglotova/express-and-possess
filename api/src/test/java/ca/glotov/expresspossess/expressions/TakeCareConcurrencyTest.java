package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Twenty members press Take Care on the same wish at the same instant. The claim is one
 * conditional UPDATE, so the database lets exactly one of them through; the other nineteen
 * get 0 rows and therefore a 409. No locks are taken and no retry is needed.
 */
class TakeCareConcurrencyTest extends ApiTest {

    private static final int CONTENDERS = 20;

    @Autowired
    ExpressionService expressions;

    @Test
    void exactlyOneOfManySimultaneousTakeCareCallsWins() throws Exception {
        Member natasha = register("Natasha");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        List<Member> contenders = new ArrayList<>();
        for (int i = 0; i < CONTENDERS; i++) {
            Member member = register("Contender" + i);
            postAs(natasha, "/api/groups/" + group + "/invitations", Map.of("email", member.email()));
            contenders.add(member);
        }
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "The one thing everyone wants to give"))).get("id").asLong();

        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
        List<Future<Outcome>> results = new ArrayList<>();
        try {
            for (Member contender : contenders) {
                results.add(pool.submit(() -> {
                    go.await();
                    try {
                        expressions.takeCare(id, contender.id(), false);
                        return Outcome.WON;
                    } catch (ApiException e) {
                        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
                        return Outcome.LOST;
                    }
                }));
            }
            go.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> result : results) {
                outcomes.add(result.get());
            }
            assertThat(outcomes).hasSize(CONTENDERS);
            assertThat(outcomes.stream().filter(o -> o == Outcome.WON).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(o -> o == Outcome.LOST).count()).isEqualTo(CONTENDERS - 1);
        } finally {
            pool.shutdownNow();
        }

        getAs(natasha, "/api/expressions/" + id)
                .andExpect(jsonPath("$.status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.implementer.name").value(org.hamcrest.Matchers.startsWith("Contender")));
    }

    private enum Outcome {
        WON,
        LOST
    }
}
