import { describe, expect, it } from "vitest";
import { findLinks, labelLinks, withoutLinks } from "./links";

describe("links in a description", () => {
  it("finds every address in order, once, without the sentence's punctuation", () => {
    const text =
      "Chainsaw like this: https://www.amazon.ca/gp/product/B0CMSYQM49. Or (https://shop.two/b), again https://www.amazon.ca/gp/product/B0CMSYQM49";
    expect(findLinks(text)).toEqual(["https://www.amazon.ca/gp/product/B0CMSYQM49", "https://shop.two/b"]);
    expect(findLinks("no link")).toEqual([]);
    expect(findLinks("half https://")).toEqual([]);
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
});
