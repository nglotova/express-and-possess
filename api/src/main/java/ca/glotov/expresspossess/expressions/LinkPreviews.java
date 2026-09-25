package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.common.Links;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Previews of shop links, kept in memory for an hour (see {@code spring.cache} in
 * application.yml). The page asks for a preview while the member types, so by the time the
 * wish is saved its picture is usually already known and goes on at once.
 *
 * <p>Only previews with a picture are kept. A shop that answers without one may have sent a
 * robot check instead of the product page, which the next request often gets past.
 *
 * <p>A preview the member never uses leaves its picture file behind; for a family-sized
 * instance that is a few kilobytes and not worth a clean-up job.
 */
@Component
public class LinkPreviews {

    static final String CACHE = "linkPreviews";

    /**
     * @param site       the shop's host name without "www."
     * @param title      the product name, or null
     * @param pictureUrl the stored picture, or null when the shop gave none
     */
    public record LinkPreview(String url, String site, String title, String pictureUrl) {
    }

    private static final Logger log = LoggerFactory.getLogger(LinkPreviews.class);

    private final LinkPreviewFetcher fetcher;
    private final FileStorage files;
    private final CacheManager caches;

    LinkPreviews(LinkPreviewFetcher fetcher, FileStorage files, CacheManager caches) {
        this.fetcher = fetcher;
        this.files = files;
        this.caches = caches;
    }

    /** Fetches a preview with a picture once per hour per address; one without is fetched again each time. */
    @Cacheable(cacheNames = CACHE, key = "#url", unless = "#result.pictureUrl() == null")
    public LinkPreview lookup(String url) {
        Optional<LinkPreviewFetcher.Preview> page = fetcher.preview(url);
        if (page.isEmpty()) {
            return new LinkPreview(url, Links.site(url), null, null);
        }
        String pictureUrl = null;
        LinkPreviewFetcher.Image image = page.get().image();
        if (image != null) {
            try {
                pictureUrl = files.store(image.bytes(), image.contentType());
            } catch (RuntimeException e) {
                log.info("Could not keep the picture from {}: {}", url, e.getMessage());
            }
        }
        return new LinkPreview(url, Links.site(url), page.get().title(), pictureUrl);
    }

    /** A preview already fetched for this address, without fetching. */
    public Optional<LinkPreview> cached(String url) {
        Cache cache = caches.getCache(CACHE);
        return cache == null ? Optional.empty() : Optional.ofNullable(cache.get(url, LinkPreview.class));
    }
}
