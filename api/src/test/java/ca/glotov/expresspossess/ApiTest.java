package ca.glotov.expresspossess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for tests that drive the HTTP API as a member would. A "member" here is a
 * registered account with its session, created fresh per test so tests never share data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected Mailpit mailpit;

    public record Member(Long id, String email, String name, MockHttpSession session) {
    }

    protected Member register(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", "Correct-horse-battery-1", "name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        return new Member(body.get("id").asLong(), email, name,
                (MockHttpSession) result.getRequest().getSession(false));
    }

    /** A fresh session for the same account, for example after its role changed. */
    protected Member login(Member who) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", who.email(), "password", "Correct-horse-battery-1"))))
                .andExpect(status().isOk())
                .andReturn();
        return new Member(who.id(), who.email(), who.name(), (MockHttpSession) result.getRequest().getSession(false));
    }

    protected ResultActions getAs(Member who, String path) throws Exception {
        return mvc.perform(get(path).session(who.session()));
    }

    protected ResultActions postAs(Member who, String path) throws Exception {
        return mvc.perform(post(path).with(csrf()).session(who.session()));
    }

    protected ResultActions postAs(Member who, String path, Map<String, ?> body) throws Exception {
        return mvc.perform(withJson(post(path), body).with(csrf()).session(who.session()));
    }

    protected ResultActions putAs(Member who, String path, Map<String, ?> body) throws Exception {
        return mvc.perform(withJson(put(path), body).with(csrf()).session(who.session()));
    }

    protected ResultActions deleteAs(Member who, String path) throws Exception {
        return mvc.perform(delete(path).with(csrf()).session(who.session()));
    }

    protected JsonNode bodyOf(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    /** Body for the creator's Submit, carrying the version from the last response. */
    protected static Map<String, Object> wishBody(String description, String wantedBy, JsonNode current) {
        Map<String, Object> body = new HashMap<>();
        body.put("description", description);
        body.put("wantedBy", wantedBy);
        body.put("version", current.get("version").asLong());
        return body;
    }

    /** Body for the implementer's Submit, carrying the version from the last response. */
    protected static Map<String, Object> careBody(boolean incognito, String providingBy, boolean provided,
                                                  JsonNode current) {
        Map<String, Object> body = new HashMap<>();
        body.put("incognito", incognito);
        body.put("providingBy", providingBy);
        body.put("provided", provided);
        body.put("version", current.get("version").asLong());
        return body;
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder builder, Map<String, ?> body)
            throws Exception {
        return builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
}
