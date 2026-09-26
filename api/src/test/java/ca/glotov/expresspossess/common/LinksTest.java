package ca.glotov.expresspossess.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinksTest {

    @Test
    void findsEveryAddressInOrderWithoutTheSentencesPunctuation() {
        String text = "Chainsaw like this: https://www.amazon.ca/gp/product/B0CMSYQM49. "
                + "Or this one (https://shop.two/b), and again https://www.amazon.ca/gp/product/B0CMSYQM49";

        assertThat(Links.all(text)).containsExactly(
                "https://www.amazon.ca/gp/product/B0CMSYQM49",
                "https://shop.two/b");
        assertThat(Links.first(text)).isEqualTo("https://www.amazon.ca/gp/product/B0CMSYQM49");
        assertThat(Links.first("No link at all")).isNull();
        assertThat(Links.all("half an address https://")).isEmpty();
    }

    @Test
    void theTextWithoutItsAddresses() {
        assertThat(Links.withoutLinks("Toner\nhttps://www.sephora.com/ca/en/product/p428819  \n  soft pink"))
                .isEqualTo("Toner\nsoft pink");
        assertThat(Links.withoutLinks("https://only.a/link")).isEmpty();
    }

    @Test
    void theSiteALinkPointsTo() {
        assertThat(Links.site("https://www.amazon.ca/dp/B07XXDNZJ9")).isEqualTo("amazon.ca");
        assertThat(Links.site("https://shop.two/b")).isEqualTo("shop.two");
    }

    @Test
    void theShopsNameWithoutTheEndingThatMailProgramsWouldTurnIntoALink() {
        assertThat(Links.shopName("https://www.amazon.ca/dp/B07XXDNZJ9")).isEqualTo("Amazon");
        assertThat(Links.shopName("https://www.sephora.com/ca/en/product/p428819")).isEqualTo("Sephora");
        assertThat(Links.shopName("https://www.shop.co.uk/item")).isEqualTo("Shop");
        assertThat(Links.shopName("https://smile.amazon.ca/dp/1")).isEqualTo("Amazon");
    }
}
