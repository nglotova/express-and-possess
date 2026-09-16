package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.common.Links;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The preview card the page shows while a member types a link. Members only. */
@RestController
class LinkPreviewController {

    private static final int MAX_URL = 2000;

    private final LinkPreviews previews;
    private final LinkPreviewProperties properties;

    LinkPreviewController(LinkPreviews previews, LinkPreviewProperties properties) {
        this.previews = previews;
        this.properties = properties;
    }

    @GetMapping("/api/link-preview")
    LinkPreviews.LinkPreview preview(@RequestParam String url) {
        if (!properties.enabled()) {
            throw ApiException.notFound("Link previews are switched off");
        }
        String link = Links.first(url);
        if (link == null || !link.equals(url.strip()) || link.length() > MAX_URL) {
            throw ApiException.badRequest("Not a web address");
        }
        return previews.lookup(link);
    }
}
