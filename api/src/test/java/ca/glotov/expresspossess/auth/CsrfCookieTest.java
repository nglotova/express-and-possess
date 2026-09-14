package ca.glotov.expresspossess.auth;

import ca.glotov.expresspossess.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs in its own application context on purpose. Spring Security's {@code csrf()} test
 * helper replaces the CSRF filter's token store for the rest of the context's life, so the
 * cookie can only be observed in a context where that helper has never run.
 */
@SpringBootTest(properties = "test.context=csrf-cookie")
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class CsrfCookieTest {

    @Autowired
    MockMvc mvc;

    @Test
    void theFirstResponseCarriesTheCsrfCookieForTheWebApp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }
}
