import { describe, expect, it } from "vitest";
import { findLinks, labelLinks, nameFromAddress, splitLinks, wishTitle, withoutLinks } from "./links";

describe("links in a description", () => {
  it("finds every address in order, once, without the sentence's punctuation", () => {
    const text =
      "Chainsaw like this: https://www.amazon.ca/gp/product/B0CMSYQM49. Or (https://shop.two/b), again https://www.amazon.ca/gp/product/B0CMSYQM49";
    expect(findLinks(text)).toEqual(["https://www.amazon.ca/gp/product/B0CMSYQM49", "https://shop.two/b"]);
    expect(findLinks("no link")).toEqual([]);
    expect(findLinks("half https://")).toEqual([]);
  });

  it("cuts a comment into text and addresses, leaving the punctuation in the text", () => {
    expect(splitLinks("Look: https://shop.two/b. Or https://x.ca/a?c=1")).toEqual([
      { text: "Look: " },
      { text: "https://shop.two/b", url: "https://shop.two/b" },
      { text: ". Or " },
      { text: "https://x.ca/a?c=1", url: "https://x.ca/a?c=1" },
    ]);
    expect(splitLinks("no link, half https://")).toEqual([{ text: "no link, half https://" }]);
    expect(splitLinks("")).toEqual([]);
  });

  it("gives the text without its addresses", () => {
    expect(withoutLinks("Toner\nhttps://www.sephora.com/p  \n  soft pink")).toBe("Toner\nsoft pink");
  });

  it("numbers links to the same site", () => {
    expect(labelLinks(["https://www.amazon.ca/a", "https://shop.two/b", "https://amazon.ca/c"])).toEqual([
      { url: "https://www.amazon.ca/a", label: "amazon.ca 1" },
      { url: "https://shop.two/b", label: "shop.two" },
      { url: "https://amazon.ca/c", label: "amazon.ca 2" },
    ]);
  });

  it("reads the product's name from the address, ignoring the tracking codes", () => {
    expect(
      nameFromAddress(
        "https://www.amazon.ca/Angel-Kiss-Crossbody-Hobo-Tote-Hobo-Shoulder/dp/B09PZXRZ68/ref=mp_s_a_1_2_sspa?crid=34GB&sp_csd=d2lk",
      ),
    ).toBe("Angel Kiss Crossbody Hobo Tote Hobo Shoulder");
    expect(nameFromAddress("https://www.sephora.com/ca/en/product/glow-toner-P123456.html")).toBe("glow toner P123456");
    expect(nameFromAddress("https://www.amazon.ca/gp/product/B0CMSYQM49")).toBeNull();
    expect(nameFromAddress("https://shop.two/b")).toBeNull();
  });

  it("titles a wish by its text, or else by what its first address names", () => {
    expect(wishTitle("Toner\nsoft pink https://shop.two/b")).toBe("Toner");
    expect(wishTitle("https://www.amazon.ca/Angel-Kiss-Tote/dp/B09PZXRZ68")).toBe("Angel Kiss Tote");
    expect(wishTitle("https://shop.two/b")).toBe("shop.two");
  });
});
