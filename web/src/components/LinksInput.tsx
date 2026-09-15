import { hostOf } from "./links";

/**
 * One text box per link, plus "Add another link". Empty boxes are dropped on save; the
 * server also checks that each is a web address.
 */
export function LinksInput({
  links,
  onChange,
  disabled,
}: {
  links: string[];
  onChange: (links: string[]) => void;
  disabled?: boolean;
}) {
  const rows = links.length === 0 ? [""] : links;
  function set(i: number, value: string) {
    const next = [...rows];
    next[i] = value;
    onChange(next);
  }
  function remove(i: number) {
    onChange(rows.filter((_, j) => j !== i));
  }
  return (
    <div className="field">
      <span className="field-label">Links</span>
      {rows.map((link, i) => (
        <div key={i} className="link-row">
          <input
            type="url"
            value={link}
            onChange={(e) => set(i, e.target.value)}
            placeholder="https://…"
            disabled={disabled}
            aria-label={`Link ${i + 1}`}
          />
          {!disabled && rows.length > 1 && (
            <button type="button" className="link" onClick={() => remove(i)} aria-label={`Remove link ${i + 1}`}>
              ✕
            </button>
          )}
        </div>
      ))}
      {!disabled && rows.length < 10 && (
        <button type="button" className="link" onClick={() => onChange([...rows, ""])}>
          + Add another link
        </button>
      )}
      <span className="field-hint">Paste the product page from the shop; each one opens in a new tab.</span>
    </div>
  );
}

/** The links as the others see them: one line per link, labelled with the shop's name. */
export function LinkList({ links }: { links: string[] }) {
  if (links.length === 0) return null;
  return (
    <ul className="link-list">
      {links.map((url) => (
        <li key={url}>
          <a href={url} target="_blank" rel="noopener noreferrer">
            ↗ {hostOf(url)}
          </a>
          <span className="muted small"> {url}</span>
        </li>
      ))}
    </ul>
  );
}
