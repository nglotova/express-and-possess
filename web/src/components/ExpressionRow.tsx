import { Link } from "react-router";
import type { ExpressionView } from "../api/types";
import { StatusChip } from "./StatusChip";
import { formatDate } from "./format";

/**
 * One row of a wish list. Shows the creator when the list mixes creators, and the
 * implementer when someone has taken care.
 */
export function ExpressionRow({ expression, showCreator }: { expression: ExpressionView; showCreator?: boolean }) {
  const meta: string[] = [];
  if (expression.wantedBy) meta.push(`by ${formatDate(expression.wantedBy)}`);
  if (expression.providingBy) meta.push(`providing by ${formatDate(expression.providingBy)}`);
  if (expression.implementer) meta.push(expression.implementer.name);
  return (
    <Link to={`/expressions/${expression.id}`} className="row">
      <div className="thumb" aria-hidden="true">
        {expression.pictureUrl ? <img src={expression.pictureUrl} alt="" /> : <span>🎁</span>}
      </div>
      <div className="row-body">
        <div className="row-title">
          {showCreator && <span className="row-creator">{expression.creator.name} · </span>}
          {firstLine(expression.description)}
        </div>
        <div className="row-meta">{meta.length ? meta.join(" · ") : "no date"}</div>
      </div>
      <div className="row-side">
        {expression.status === "EXPRESSED" && showCreator ? (
          <span className="bang" aria-label="Not taken yet">
            !
          </span>
        ) : (
          <StatusChip status={expression.status} />
        )}
      </div>
    </Link>
  );
}

function firstLine(text: string) {
  const line = text.trim().split("\n")[0];
  return line.length > 80 ? line.slice(0, 77) + "…" : line;
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
