package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PictureUploadTest extends ApiTest {

    /** A 1x1 transparent PNG. */
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Test
    void theCreatorCanAttachAPictureAndMembersCanFetchIt() throws Exception {
        Member natasha = register("Natasha");
        Member andrei = register("Andrei");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        postAs(natasha, "/api/groups/" + group + "/invitations", Map.of("email", andrei.email()));
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Shoes"))).get("id").asLong();

        MockMultipartFile picture = new MockMultipartFile("file", "shoes.png", "image/png", PNG);
        mvc.perform(multipart("/api/expressions/" + id + "/picture").file(picture)
                        .with(csrf()).session(andrei.session()))
                .andExpect(status().isForbidden());

        String url = bodyOf(mvc.perform(multipart("/api/expressions/" + id + "/picture").file(picture)
                        .with(csrf()).session(natasha.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictureUrl").value(org.hamcrest.Matchers.startsWith("/api/files/"))))
                .get("pictureUrl").asText();

        getAs(andrei, url)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void onlyPicturesAreAccepted() throws Exception {
        Member natasha = register("Natasha");
        long group = bodyOf(postAs(natasha, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
        long id = bodyOf(postAs(natasha, "/api/groups/" + group + "/expressions",
                Map.of("description", "Shoes"))).get("id").asLong();

        MockMultipartFile script = new MockMultipartFile("file", "evil.html", "text/html", "<script>".getBytes());
        mvc.perform(multipart("/api/expressions/" + id + "/picture").file(script)
                        .with(csrf()).session(natasha.session()))
                .andExpect(status().isBadRequest());
    }
}
