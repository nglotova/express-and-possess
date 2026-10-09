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

/** The text cut into plain pieces and addresses, in order, so the addresses can be shown as links. */
export function splitLinks(text: string): { text: string; url?: string }[] {
  const parts: { text: string; url?: string }[] = [];
  let rest = 0;
  for (const match of text.matchAll(WEB_ADDRESS)) {
    const url = match[0].replace(TRAILING, "");
    if (url.indexOf("://") + 3 >= url.length) continue;
    if (match.index > rest) parts.push({ text: text.slice(rest, match.index) });
    parts.push({ text: url, url });
    rest = match.index + url.length;
  }
  if (rest < text.length) parts.push({ text: text.slice(rest) });
  return parts;
}

/** Text with every address in it turned into a link that opens in a new tab. */
export function LinkedText({ text }: { text: string }) {
  return (
    <>
      {splitLinks(text).map((part, i) =>
        part.url ? (
          <a key={i} href={part.url} target="_blank" rel="noopener noreferrer">
            {part.text}
          </a>
        ) : (
          part.text
        ),
      )}
    </>
  );
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

/**
 * The product's name as the address spells it, such as "Angel Kiss Crossbody Hobo Tote" from
 * amazon.ca/Angel-Kiss-Crossbody-Hobo-Tote/dp/B09PZXRZ68: the longest part of the path made of
 * words joined by dashes or underscores. Null when the address has no such part.
 */
export function nameFromAddress(url: string): string | null {
  let path: string;
  try {
    path = new URL(url).pathname;
  } catch {
    return null;
  }
  const names = path
    .split("/")
    .map((part) => {
      try {
        return decodeURIComponent(part);
      } catch {
        return part;
      }
    })
    .map((part) => part.replace(/\.[a-z0-9]{2,5}$/i, ""))
    .filter((part) => !part.includes("=") && /\p{L}{2,}[-_+]+\p{L}{2,}/u.test(part))
    .map((part) => part.replace(/[-_+]+/g, " ").trim());
  if (names.length === 0) return null;
  const name = names.reduce((longest, n) => (n.length > longest.length ? n : longest));
  return name.length > 80 ? name.slice(0, 79) + "…" : name;
}

/** A wish's title: its first line without addresses, or else what its first address names. */
export function wishTitle(description: string): string {
  const text = withoutLinks(description);
  if (text) return text.split("\n")[0];
  const link = findLinks(description)[0];
  return link ? (nameFromAddress(link) ?? hostOf(link)) : description;
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

/**
 * The links under a description: one line each, labelled with the site and opening in a new tab,
 * with the product's name from the address beside it. The full address, often long with a
 * shop's tracking codes, shows when the pointer rests on the link.
 */
export function LinkList({ links }: { links: string[] }) {
  if (links.length === 0) return null;
  return (
    <ul className="link-list" aria-label="Links">
      {labelLinks(links).map(({ url, label }) => {
        const name = nameFromAddress(url);
        return (
          <li key={url}>
            <a href={url} target="_blank" rel="noopener noreferrer" title={url}>
              ↗ {label}
            </a>
            {name && <span className="link-name"> · {name}</span>}
          </li>
        );
      })}
    </ul>
  );
}
