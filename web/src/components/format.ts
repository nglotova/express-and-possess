const dateFormat = new Intl.DateTimeFormat(undefined, { day: "numeric", month: "short", year: "numeric" });
const timeFormat = new Intl.DateTimeFormat(undefined, { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

/** An ISO date (2026-10-20) as the reader's locale writes it. */
export function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-").map(Number);
  return dateFormat.format(new Date(y, m - 1, d));
}

/** An instant as a short local timestamp. */
export function formatTime(iso: string): string {
  return timeFormat.format(new Date(iso));
}
