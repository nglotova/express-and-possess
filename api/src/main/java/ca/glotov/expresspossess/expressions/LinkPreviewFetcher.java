package ca.glotov.expresspossess.expressions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Reads a shop's page for its product title and picture, and downloads the picture.
 *
 * <p>Where the picture is looked for, in order: the Open Graph and Twitter preview tags,
 * schema.org product data, and Amazon's main product image. Some shops refuse automated
 * requests altogether; those simply give no preview.
 *
 * <p>The server fetches addresses that members typed, so every request, including each
 * redirect, is refused unless the host resolves only to public addresses. A host whose DNS
 * answer changes between that check and the connection could still get through; for a
 * family-sized deployment that remaining risk is accepted and written down here.
 */
@Component
class LinkPreviewFetcher {

    record Image(byte[] bytes, String contentType) {
    }

    /** What a page offers; either part may be missing. */
    record Preview(String title, Image image) {
    }

    private record Fetched(URI uri, String contentType, byte[] body) {
    }

    /** Amazon's product image sits about 400 KB into a 2.5 MB page. */
    static final int MAX_PAGE_BYTES = 1_500_000;
    static final int MAX_IMAGE_BYTES = 5_000_000;
    private static final int MAX_TITLE = 300;
    private static final int MAX_REDIRECTS = 5;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ExpressPossess/1.0; link preview)";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(LinkPreviewFetcher.class);

    private final LinkPreviewProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    LinkPreviewFetcher(LinkPreviewProperties properties) {
        this.properties = properties;
    }

    /** The title and picture of a shop page, or empty when the page cannot be read at all. */
    Optional<Preview> preview(String pageUrl) {
        try {
            Fetched page = fetch(URI.create(pageUrl), MAX_PAGE_BYTES, true, "text/html,application/xhtml+xml");
            if (page == null || !page.contentType().contains("html")) {
                return Optional.empty();
            }
            Document document = Jsoup.parse(new ByteArrayInputStream(page.body()), null, page.uri().toString());
            Image image = imageUrlIn(document).flatMap(this::download).orElse(null);
            return Optional.of(new Preview(titleIn(document), image));
        } catch (IOException | IllegalArgumentException e) {
            log.debug("No preview from {}: {}", pageUrl, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /** The product's name as the page gives it, or null. */
    static String titleIn(Document document) {
        for (String candidate : List.of(
                text(document.selectFirst("#productTitle")),
                attribute(document.selectFirst("meta[property=og:title]"), "content"),
                attribute(document.selectFirst("meta[name=twitter:title]"), "content"),
                document.title())) {
            String title = candidate.replaceAll("\\s+", " ").strip();
            if (!title.isEmpty()) {
                return title.length() > MAX_TITLE ? title.substring(0, MAX_TITLE - 1) + "…" : title;
            }
        }
        return null;
    }

    /** Where a page says its picture is, as an absolute address. */
    static Optional<String> imageUrlIn(Document document) {
        for (String selector : List.of(
                "meta[property=og:image:secure_url]",
                "meta[property=og:image]",
                "meta[name=og:image]",
                "meta[name=twitter:image]",
                "meta[property=twitter:image]")) {
            String source = attribute(document.selectFirst(selector), "abs:content");
            if (!source.isBlank()) {
                return Optional.of(source);
            }
        }
        Optional<String> structured = imageFromStructuredData(document);
        if (structured.isPresent()) {
            return structured;
        }
        Element amazon = document.selectFirst("#landingImage");
        if (amazon != null) {
            String source = amazon.attr("abs:data-old-hires");
            if (source.isBlank()) {
                source = amazon.attr("abs:src");
            }
            if (!source.isBlank()) {
                return Optional.of(source);
            }
        }
        String imageSource = attribute(document.selectFirst("link[rel=image_src]"), "abs:href");
        return imageSource.isBlank() ? Optional.empty() : Optional.of(imageSource);
    }

    /** Refuses anything but web addresses on hosts that resolve only to public addresses. */
    void requireAllowed(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) || uri.getHost() == null) {
            throw new IllegalArgumentException("Not a web address: " + uri);
        }
        if (properties.allowPrivateAddresses()) {
            return;
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (!isPublic(address)) {
                throw new IllegalArgumentException("Refusing to fetch from a private address: " + uri.getHost());
            }
        }
    }

    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            if (first == 0 || first >= 240) {
                return false;
            }
            if (first == 100 && second >= 64 && second <= 127) {
                return false; // carrier-grade NAT
            }
            return !(first == 198 && (second == 18 || second == 19)); // benchmarking networks
        }
        return (bytes[0] & 0xfe) != 0xfc; // IPv6 unique local addresses, fc00::/7
    }

    private Optional<Image> download(String source) {
        try {
            Fetched image = fetch(URI.create(source), MAX_IMAGE_BYTES, false, "image/*");
            if (image == null || !image.contentType().startsWith("image/")) {
                return Optional.empty();
            }
            String type = image.contentType().equals("image/jpg") ? "image/jpeg" : image.contentType();
            return Optional.of(new Image(image.body(), type));
        } catch (IOException | IllegalArgumentException e) {
            log.debug("No picture from {}: {}", source, e.toString());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Fetched fetch(URI start, int maxBytes, boolean truncate, String accept)
            throws IOException, InterruptedException {
        URI current = start;
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            requireAllowed(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", accept)
                    .header("Accept-Language", "en-CA,en;q=0.8")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (status >= 300 && status < 400) {
                    Optional<String> location = response.headers().firstValue("location");
                    if (location.isEmpty()) {
                        return null;
                    }
                    current = current.resolve(location.get());
                    continue;
                }
                if (status != 200) {
                    return null;
                }
                byte[] bytes = body.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    if (!truncate) {
                        return null;
                    }
                    bytes = Arrays.copyOf(bytes, maxBytes);
                }
                String type = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
                int parameters = type.indexOf(';');
                return new Fetched(current, (parameters >= 0 ? type.substring(0, parameters) : type).trim(), bytes);
            }
        }
        return null;
    }

    private static String text(Element element) {
        return element == null ? "" : element.text();
    }

    private static String attribute(Element element, String name) {
        return element == null ? "" : element.attr(name);
    }

    private static Optional<String> imageFromStructuredData(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                Optional<String> image = findImage(JSON.readTree(script.data()), 0);
                if (image.isPresent()) {
                    return Optional.of(URI.create(document.location()).resolve(image.get().trim()).toString());
                }
            } catch (IOException | IllegalArgumentException e) {
                // Not usable; try the next block.
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findImage(JsonNode node, int depth) {
        if (node == null || depth > 6) {
            return Optional.empty();
        }
        if (node.isObject()) {
            JsonNode image = node.get("image");
            if (image != null) {
                JsonNode first = image.isArray() && !image.isEmpty() ? image.get(0) : image;
                if (first.isTextual() && !first.asText().isBlank()) {
                    return Optional.of(first.asText());
                }
                if (first.isObject() && first.hasNonNull("url")) {
                    return Optional.of(first.get("url").asText());
                }
            }
        }
        if (node.isContainerNode()) {
            for (JsonNode child : node) {
                Optional<String> found = findImage(child, depth + 1);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
}
