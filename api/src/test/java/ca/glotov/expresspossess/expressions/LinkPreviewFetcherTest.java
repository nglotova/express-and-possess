package ca.glotov.expresspossess.expressions;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LinkPreviewFetcherTest {

    private static final String PAGE = "https://shop.example/p/1";

    @Test
    void localAndPrivateAddressesAreNeverFetched() throws Exception {
        for (String address : List.of("127.0.0.1", "10.1.2.3", "172.16.0.5", "192.168.1.20", "169.254.169.254",
                "100.64.0.1", "0.0.0.0", "::1", "fd12:3456::1")) {
            assertThat(LinkPreviewFetcher.isPublic(InetAddress.getByName(address))).as(address).isFalse();
        }
        for (String address : List.of("8.8.8.8", "151.101.1.69", "2606:4700::1111")) {
            assertThat(LinkPreviewFetcher.isPublic(InetAddress.getByName(address))).as(address).isTrue();
        }

        LinkPreviewFetcher fetcher = new LinkPreviewFetcher(new LinkPreviewProperties(true, false));
        assertThat(fetcher.preview("http://127.0.0.1:9/anything")).isEmpty();
        assertThat(fetcher.preview("file:///etc/passwd")).isEmpty();
    }

    @Test
    void thePictureIsFoundWhereShopsPutIt() {
        assertThat(image("<meta property=\"og:image\" content=\"/a.jpg\">"))
                .contains("https://shop.example/a.jpg");
        assertThat(image("<meta name=\"twitter:image\" content=\"https://cdn.example/b.jpg\">"))
                .contains("https://cdn.example/b.jpg");
        assertThat(image("<script type=\"application/ld+json\">"
                + "{\"@context\":\"https://schema.org\",\"@type\":\"Product\",\"name\":\"X\","
                + "\"image\":[\"https://cdn.example/c.jpg\"]}</script>"))
                .contains("https://cdn.example/c.jpg");
        assertThat(image("<div id=\"imgTagWrapperId\"><img id=\"landingImage\" src=\"/small.jpg\" "
                + "data-old-hires=\"https://m.media-amazon.com/images/I/61W8RAK1LYL._AC_SL1500_.jpg\"></div>"))
                .contains("https://m.media-amazon.com/images/I/61W8RAK1LYL._AC_SL1500_.jpg");
        assertThat(image("<title>A page without a picture</title>")).isEmpty();
    }

    @Test
    void theTitleIsTheProductNameWhereThereIsOne() {
        assertThat(title("<title>Shop: Pan</title><meta property=\"og:title\" content=\"Frying pan\">"))
                .isEqualTo("Frying pan");
        // Amazon repeats the product title element; the first one counts, spaces tidied.
        assertThat(title("<title>Amazon.ca: Pan</title>"
                + "<span id=\"productTitle\">\n   JEETEE 8 inch   Frying Pan  </span>"
                + "<span id=\"productTitle\">JEETEE 8 inch Frying Pan</span>"))
                .isEqualTo("JEETEE 8 inch Frying Pan");
        assertThat(title("<title>Just a title</title>")).isEqualTo("Just a title");
        assertThat(title("<p>nothing</p>")).isNull();
    }

    private static Optional<String> image(String html) {
        return LinkPreviewFetcher.imageUrlIn(Jsoup.parse(html, PAGE));
    }

    private static String title(String html) {
        return LinkPreviewFetcher.titleIn(Jsoup.parse(html, PAGE));
    }
}
