const WEB_ADDRESS = /https?:\/\/[^\s<>"']+/g;

/** The description without its web addresses, and the addresses on their own. */
export function splitLinks(text: string): { text: string; urls: string[] } {
  const urls = text.match(WEB_ADDRESS) ?? [];
  const rest = text
    .replace(WEB_ADDRESS, "")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .join("\n");
  return { text: rest, urls: Array.from(new Set(urls)) };
}

/** A short label for a link: the site's name without "www.". */
export function hostOf(url: string): string {
  try {
    return new URL(url).hostname.replace(/^www\./, "");
  } catch {
    return url;
  }
}

/** Renders the description with its addresses as links that open in a new tab. */
export function linkify(text: string) {
  const parts = text.split(/(https?:\/\/[^\s<>"']+)/g);
  return parts.map((part, i) =>
    /^https?:\/\//.test(part) ? (
      <a key={i} href={part} target="_blank" rel="noopener noreferrer">
        {part}
      </a>
    ) : (
      <span key={i}>{part}</span>
    ),
  );
}
