// Web addresses inside a wish's description. The server finds links with the same rules in
// api/.../common/Links.java; keep the two in step.

const WEB_ADDRESS = /https?:\/\/[^\s<>"']+/gi;
/** Punctuation that ends a sentence rather than the address: "like this: https://x.ca/a." */
const TRAILING = /[.,;:!?)\]}]+$/;

/** Every address in the text, in order, each once. */
export function findLinks(text: string): string[] {
  const found = (text.match(WEB_ADDRESS) ?? [])
    .map((url) => url.replace(TRAILING, ""))
    .filter((url) => url.indexOf("://") + 3 < url.length);
  return Array.from(new Set(found));
}

/** The text with its addresses taken out and the empty lines that leaves dropped. */
export function withoutLinks(text: string): string {
  return text
    .replace(WEB_ADDRESS, "")
    .split("\n")
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter((line) => line.length > 0)
    .join("\n");
}

/** A short label for a link: the site's name without "www.". */
export function hostOf(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return url;
  }
}

/** Labels for a list of links: the site's name, numbered when two links share it. */
export function labelLinks(urls: string[]): { url: string; label: string }[] {
  const totals = new Map<string, number>();
  urls.forEach((url) => totals.set(hostOf(url), (totals.get(hostOf(url)) ?? 0) + 1));
  const seen = new Map<string, number>();
  return urls.map((url) => {
    const host = hostOf(url);
    if ((totals.get(host) ?? 0) < 2) return { url, label: host };
    const n = (seen.get(host) ?? 0) + 1;
    seen.set(host, n);
    return { url, label: `${host} ${n}` };
  });
}

/** The links under a description: one line each, labelled with the site, opening in a new tab. */
export function LinkList({ links }: { links: string[] }) {
  if (links.length === 0) return null;
  return (
    <ul className="link-list" aria-label="Links">
      {labelLinks(links).map(({ url, label }) => (
        <li key={url}>
          <a href={url} target="_blank" rel="noopener noreferrer">
            ↗ {label}
          </a>
          <span className="link-url">{url}</span>
        </li>
      ))}
    </ul>
  );
}
