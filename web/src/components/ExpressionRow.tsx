import { Link } from "react-router";
import type { ExpressionView } from "../api/types";
import { StatusChip } from "./StatusChip";
import { formatDate } from "./format";
import { hostOf, splitLinks } from "./links";

/**
 * One row of a wish list. The text opens the wish; any web address in the description
 * becomes its own link under the text that opens the shop in a new tab, since a link
 * cannot sit inside another link.
 */
export function ExpressionRow({ expression, showCreator }: { expression: ExpressionView; showCreator?: boolean }) {
  const { text, urls: inText } = splitLinks(expression.description);
  const urls = Array.from(new Set([...expression.links, ...inText]));
  const meta: string[] = [];
  if (expression.wantedBy) meta.push(`by ${formatDate(expression.wantedBy)}`);
  if (expression.providingBy) meta.push(`providing by ${formatDate(expression.providingBy)}`);
  if (expression.implementer) meta.push(expression.implementer.name);
  const to = `/expressions/${expression.id}`;
  return (
    <div className="row">
      <Link to={to} className="thumb" aria-hidden="true" tabIndex={-1}>
        {expression.pictureUrl ? <img src={expression.pictureUrl} alt="" /> : <span>🎁</span>}
      </Link>
      <div className="row-body">
        <Link to={to} className="row-title">
          {showCreator && <span className="row-creator">{expression.creator.name} · </span>}
          {text || expression.description}
        </Link>
        <div className="row-meta">{meta.length ? meta.join(" · ") : "no date"}</div>
        {urls.length > 0 && (
          <div className="row-links">
            {urls.map((url) => (
              <a key={url} href={url} target="_blank" rel="noopener noreferrer">
                ↗ {hostOf(url)}
              </a>
            ))}
          </div>
        )}
      </div>
      <Link to={to} className="row-side" aria-label="Open">
        {expression.status === "EXPRESSED" && showCreator ? (
          <span className="bang" aria-label="Not taken yet">
            !
          </span>
        ) : (
          <StatusChip status={expression.status} />
        )}
      </Link>
    </div>
  );
}

export function ExpressionList({
  title,
  items,
  showCreator,
  empty,
  action,
}: {
  title: string;
  items: ExpressionView[];
  showCreator?: boolean;
  empty: string;
  action?: React.ReactNode;
}) {
  return (
    <section className="section">
      <h2>{title}</h2>
      {items.length === 0 ? (
        <p className="muted">{empty}</p>
      ) : (
        <div className="rows">
          {items.map((e) => (
            <ExpressionRow key={e.id} expression={e} showCreator={showCreator} />
          ))}
        </div>
      )}
      {action}
    </section>
  );
}
