package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.ApiTest;
import ca.glotov.expresspossess.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The picture taken from a wish's first link, against a small shop served from the test
 * machine itself. Its own application context: the feature is switched on here only.
 */
@SpringBootTest(properties = {
        "app.link-preview.enabled=true",
        "app.link-preview.allow-private-addresses=true",
        "test.context=link-preview"})
class LinkPreviewTest extends ApiTest {

    private static final byte[] ONE = {(byte) 0x89, 'P', 'N', 'G', 1};
    private static final byte[] TWO = {(byte) 0x89, 'P', 'N', 'G', 2};
    private static final byte[] MINE = {(byte) 0x89, 'P', 'N', 'G', 3};

    private static final String ROBOT_CHECK = "<html><head><title>Amazon.ca</title></head><body>Robot check</body></html>";
    private static final String PRODUCT = "<html><head><meta property=\"og:image\" content=\"/img/one.png\"></head></html>";

    private static HttpServer shop;
    private static String base;

    /** How many more times each flaky page answers with a robot check before the product. */
    private static final AtomicInteger busyChecksLeft = new AtomicInteger();
    private static final AtomicInteger flakyChecksLeft = new AtomicInteger();
    private static final AtomicInteger refusals = new AtomicInteger();

    @Autowired
    MutableClock clock;

    @Autowired
    LinkPreviewService pictures;

    @BeforeAll
    static void openTheShop() throws IOException {
        shop = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        page("/shop/one", "<html><head><meta property=\"og:image\" content=\"/img/one.png\"></head><body>One</body></html>");
        page("/shop/two", "<html><head><meta property=\"og:image\" content=\"/img/two.png\"></head><body>Two</body></html>");
        page("/shop/amazon", "<html><body><div id=\"imgTagWrapperId\">"
                + "<img id=\"landingImage\" src=\"/small.jpg\" data-old-hires=\"/img/one.png\"></div></body></html>");
        page("/shop/titled", "<html><head><title>Frying pan</title>"
                + "<meta property=\"og:image\" content=\"/img/two.png\"></head><body></body></html>");
        page("/shop/none", "<html><head><title>No picture here</title></head><body></body></html>");
        flaky("/shop/busy", busyChecksLeft);
        flaky("/shop/flaky", flakyChecksLeft);
        shop.createContext("/shop/refuses", exchange -> {
            refusals.incrementAndGet();
            send(exchange, "text/html; charset=utf-8", ROBOT_CHECK.getBytes(StandardCharsets.UTF_8));
        });
        image("/img/one.png", ONE);
        image("/img/two.png", TWO);
        shop.createContext("/go/one", exchange -> {
            exchange.getResponseHeaders().add("Location", "/shop/one");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        shop.start();
        base = "http://127.0.0.1:" + shop.getAddress().getPort();
    }

    @AfterAll
    static void closeTheShop() {
        shop.stop(0);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void thePictureComesFromTheFirstLinkAndFollowsTheLinkWhenItChanges() throws Exception {
        Member natasha = register("Natasha");
        long group = group(natasha);
        JsonNode created = wish(natasha, group, "Mini chainsaw\n" + base + "/go/one");
        long id = created.get("id").asLong();

        JsonNode first = awaitView(natasha, id, v -> !v.get("pictureUrl").isNull());
        assertThat(first.get("pictureFromLink").asBoolean()).isTrue();
        assertThat(first.get("picturePending").asBoolean()).isFalse();
        assertThat(picture(natasha, first)).isEqualTo(ONE);

        // The picture arriving leaves the version alone, so the creator's Save still goes through.
        JsonNode edited = bodyOf(putAs(natasha, "/api/expressions/" + id + "/wish",
                wishBody("Mini chainsaw, the other one " + base + "/shop/two", null, created))
                .andExpect(status().isOk()));
        String firstUrl = first.get("pictureUrl").asText();
        JsonNode second = awaitView(natasha, id,
                v -> !v.get("pictureUrl").isNull() && !v.get("pictureUrl").asText().equals(firstUrl));
        assertThat(picture(natasha, second)).isEqualTo(TWO);

        putAs(natasha, "/api/expressions/" + id + "/wish", wishBody("Mini chainsaw, no link now", null, edited))
                .andExpect(status().isOk());
        JsonNode cleared = awaitView(natasha, id, v -> v.get("pictureUrl").isNull());
        assertThat(cleared.get("pictureFromLink").asBoolean()).isFalse();
    }

    @Test
    void aPictureTheCreatorUploadsIsNeverReplaced() throws Exception {
        Member natasha = register("Natasha");
        long group = group(natasha);
        long id = wish(natasha, group, "Headphones " + base + "/shop/one").get("id").asLong();

        mvc.perform(multipart("/api/expressions/" + id + "/picture")
                        .file(new MockMultipartFile("file", "mine.png", "image/png", MINE))
                        .with(csrf()).session(natasha.session()))
                .andExpect(status().isOk());
        JsonNode afterUpload = awaitView(natasha, id, v -> !v.get("picturePending").asBoolean());
        putAs(natasha, "/api/expressions/" + id + "/wish",
                wishBody("Headphones, other colour " + base + "/shop/two", null, afterUpload))
                .andExpect(status().isOk());

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(6)).untilAsserted(() -> {
            JsonNode view = bodyOf(getAs(natasha, "/api/expressions/" + id));
            assertThat(view.get("pictureFromLink").asBoolean()).isFalse();
            assertThat(picture(natasha, view)).isEqualTo(MINE);
        });
    }

    @Test
    void amazonsProductImageIsFoundAndAPageWithoutOneEndsQuietly() throws Exception {
        Member natasha = register("Natasha");
        long group = group(natasha);
        long amazon = wish(natasha, group, "Chainsaw " + base + "/shop/amazon").get("id").asLong();
        long none = wish(natasha, group, "Socks " + base + "/shop/none").get("id").asLong();

        assertThat(picture(natasha, awaitView(natasha, amazon, v -> !v.get("pictureUrl").isNull()))).isEqualTo(ONE);

        JsonNode quiet = awaitView(natasha, none, v -> !v.get("picturePending").asBoolean());
        assertThat(quiet.get("pictureUrl").isNull()).isTrue();
    }

    @Test
    void lookingAtTheLinkWhileTypingPutsThePictureOnTheWishAtOnce() throws Exception {
        Member natasha = register("Natasha");
        long group = group(natasha);
        String link = base + "/shop/titled";

        JsonNode preview = bodyOf(mvc.perform(get("/api/link-preview").param("url", link).session(natasha.session()))
                .andExpect(status().isOk()));
        assertThat(preview.get("title").asText()).isEqualTo("Frying pan");
        assertThat(preview.get("site").asText()).isEqualTo("127.0.0.1");
        assertThat(picture(natasha, preview)).isEqualTo(TWO);

        JsonNode created = wish(natasha, group, link);
        assertThat(created.get("pictureUrl").asText()).isEqualTo(preview.get("pictureUrl").asText());
        assertThat(created.get("pictureFromLink").asBoolean()).isTrue();
        assertThat(created.get("picturePending").asBoolean()).isFalse();

        mvc.perform(get("/api/link-preview").param("url", "not a link").session(natasha.session()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/link-preview").param("url", link))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aPreviewWithoutAPictureIsNotKeptSoTheNextLookGetsPastARobotCheck() throws Exception {
        Member natasha = register("Natasha");
        busyChecksLeft.set(1);
        String link = base + "/shop/busy";

        JsonNode checked = bodyOf(mvc.perform(get("/api/link-preview").param("url", link).session(natasha.session())));
        assertThat(checked.get("pictureUrl").isNull()).isTrue();

        JsonNode product = bodyOf(mvc.perform(get("/api/link-preview").param("url", link).session(natasha.session())));
        assertThat(picture(natasha, product)).isEqualTo(ONE);
    }

    @Test
    void aLinkThatGaveNoPictureIsTriedAgainLaterAndAShopThatAlwaysRefusesIsLeftAlone() throws Exception {
        Member natasha = register("Natasha");
        long group = group(natasha);
        flakyChecksLeft.set(1);
        long flaky = wish(natasha, group, "Book " + base + "/shop/flaky").get("id").asLong();
        long refused = wish(natasha, group, "Perfume " + base + "/shop/refuses").get("id").asLong();

        // The first try met a robot check; the page stops waiting all the same.
        JsonNode first = awaitView(natasha, flaky, v -> !v.get("picturePending").asBoolean());
        assertThat(first.get("pictureUrl").isNull()).isTrue();
        awaitView(natasha, refused, v -> !v.get("picturePending").asBoolean());
        int refusalsAtFirst = refusals.get();

        // Not before the pause is over.
        pictures.retryFailed();
        assertThat(bodyOf(getAs(natasha, "/api/expressions/" + flaky)).get("pictureUrl").isNull()).isTrue();

        clock.advance(Duration.ofMinutes(16));
        pictures.retryFailed();
        JsonNode retried = bodyOf(getAs(natasha, "/api/expressions/" + flaky));
        assertThat(picture(natasha, retried)).isEqualTo(ONE);
        assertThat(retried.get("pictureFromLink").asBoolean()).isTrue();

        // Four tries in all for a shop that never gives a picture, then no more.
        for (Duration pause : List.of(Duration.ofHours(2), Duration.ofHours(5), Duration.ofDays(1), Duration.ofDays(7))) {
            clock.advance(pause);
            pictures.retryFailed();
        }
        assertThat(refusals.get() - refusalsAtFirst).isEqualTo(3);
    }

    // ---- helpers -------------------------------------------------------------------

    private long group(Member admin) throws Exception {
        return bodyOf(postAs(admin, "/api/groups", Map.of("name", "Family"))).get("id").asLong();
    }

    private JsonNode wish(Member creator, long group, String description) throws Exception {
        return bodyOf(postAs(creator, "/api/groups/" + group + "/expressions", Map.of("description", description))
                .andExpect(status().isCreated()));
    }

    private JsonNode awaitView(Member who, long id, Predicate<JsonNode> condition) {
        AtomicReference<JsonNode> last = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(200)).until(() -> {
            JsonNode view = bodyOf(getAs(who, "/api/expressions/" + id));
            last.set(view);
            return condition.test(view);
        });
        return last.get();
    }

    private byte[] picture(Member who, JsonNode view) throws Exception {
        return getAs(who, view.get("pictureUrl").asText()).andReturn().getResponse().getContentAsByteArray();
    }

    private static void page(String path, String html) {
        respond(path, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
    }

    private static void image(String path, byte[] bytes) {
        respond(path, "image/png", bytes);
    }

    private static void respond(String path, String contentType, byte[] body) {
        shop.createContext(path, exchange -> send(exchange, contentType, body));
    }

    /** A page that answers with a robot check while {@code checksLeft} lasts, then with the product. */
    private static void flaky(String path, AtomicInteger checksLeft) {
        shop.createContext(path, exchange -> {
            String html = checksLeft.getAndDecrement() > 0 ? ROBOT_CHECK : PRODUCT;
            send(exchange, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        });
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, String contentType, byte[] body)
            throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
