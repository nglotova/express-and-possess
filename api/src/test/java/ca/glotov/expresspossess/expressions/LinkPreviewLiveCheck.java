package ca.glotov.expresspossess.expressions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asks real shops over the internet. Not part of the normal build, since shops change their
 * pages and block robots without notice. Run it by hand:
 *
 * <pre>mvn -pl api test -Dtest=LinkPreviewLiveCheck -Dlive=true -Dsurefire.failIfNoSpecifiedTests=false</pre>
 */
@EnabledIfSystemProperty(named = "live", matches = "true")
class LinkPreviewLiveCheck {

    @ParameterizedTest
    @CsvSource({
            "https://www.amazon.ca/gp/product/B0CMSYQM49, Chainsaw",
            "https://www.amazon.ca/JEETEE-Nonstick-Cookware-Induction-Compatible/dp/B082W225F2?th=1, Frying Pan"})
    void amazonGivesTheProductTitleAndPicture(String url, String titleWord) {
        LinkPreviewFetcher fetcher = new LinkPreviewFetcher(new LinkPreviewProperties(true, false));

        var preview = fetcher.preview(url);

        assertThat(preview).isPresent();
        assertThat(preview.get().title()).contains(titleWord);
        assertThat(preview.get().image()).isNotNull();
        assertThat(preview.get().image().contentType()).isEqualTo("image/jpeg");
        assertThat(preview.get().image().bytes().length).isGreaterThan(10_000);
    }
}
