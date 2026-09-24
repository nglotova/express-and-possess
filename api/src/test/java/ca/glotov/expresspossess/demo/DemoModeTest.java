package ca.glotov.expresspossess.demo;

import ca.glotov.expresspossess.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Its own context: demo mode wipes the database on startup, which the other tests would
 * not appreciate.
 */
@SpringBootTest(properties = {"app.demo.enabled=true", "test.context=demo"})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class DemoModeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DemoSeeder seeder;

    @Test
    void theFamilyIsSeededAndReviewersCanLogInAsAnyPersona() throws Exception {
        mvc.perform(get("/api/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.personas").value(org.hamcrest.Matchers.contains("Alice", "Bob", "Carol")));

        MvcResult login = mvc.perform(post("/api/auth/demo/Alice").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();
        MockHttpSession alice = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/groups").session(alice))
                .andExpect(jsonPath("$[0].name").value("The Bakers"))
                .andExpect(jsonPath("$[0].status").value("WORKING"))
                .andExpect(jsonPath("$[0].hasUntaken").value(true));
        mvc.perform(get("/api/groups/1/activity").session(alice))
                .andExpect(jsonPath("$.myExpressions[0].status").value("IN_PROCESS"))
                .andExpect(jsonPath("$.notTaken.length()").value(1));
        // Alice is the demo's site administrator and sees the real helper; Carol does not.
        MockHttpSession carol = (MockHttpSession) mvc.perform(post("/api/auth/demo/Carol").with(csrf()))
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/expressions/2").session(carol))
                .andExpect(jsonPath("$.implementer.name").value("Incognito"))
                .andExpect(jsonPath("$.comments[0].author.name").value("Anonymous helper"));
        mvc.perform(get("/api/notifications").session(alice))
                .andExpect(jsonPath("$.unread").value(org.hamcrest.Matchers.greaterThan(0)));

        mvc.perform(post("/api/auth/demo/Mallory").with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void aResetRebuildsTheSameScene() throws Exception {
        MockHttpSession bob = (MockHttpSession) mvc.perform(post("/api/auth/demo/Bob").with(csrf()))
                .andReturn().getRequest().getSession(false);
        mvc.perform(post("/api/groups").with(csrf()).session(bob)
                        .contentType("application/json").content("{\"name\":\"Scratch\"}"))
                .andExpect(status().isCreated());

        seeder.reset();

        MockHttpSession bobAgain = (MockHttpSession) mvc.perform(post("/api/auth/demo/Bob").with(csrf()))
                .andReturn().getRequest().getSession(false);
        mvc.perform(get("/api/groups").session(bobAgain))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name == 'Scratch')]").isEmpty());
    }
}
