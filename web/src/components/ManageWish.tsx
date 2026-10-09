import { useState } from "react";
import { Link } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { api, ApiError } from "../api/client";
import type { ExpressionStatus, ExpressionView } from "../api/types";
import { useConfirm } from "./ConfirmDialog";
import { ErrorText, Field } from "./Form";
import { EXPRESSION_LABELS, StatusChip } from "./StatusChip";
import { wishTitle } from "./links";

const STATUSES = Object.keys(EXPRESSION_LABELS) as ExpressionStatus[];

export interface Person {
  id: number;
  name: string;
}

/**
 * A group's wishes as an admin sees them, on the Administration page and on the group admin's
 * Edit group page. Manage opens the controls for one wish at a time. When someone else changed
 * the wish after the list loaded, the server refuses; the list loads again and says so.
 *
 * @param linkToWish the titles open the wish; the site administrator is usually not a member
 *                   and cannot open it
 * @param onChange   called after a change or a delete, to load the list again
 */
export function ManagedWishes({
  wishes,
  members,
  linkToWish,
  onChange,
}: {
  wishes: ExpressionView[];
  members: Person[];
  linkToWish: boolean;
  onChange: () => void;
}) {
  const [open, setOpen] = useState<number | null>(null);
  const [changedMeanwhile, setChangedMeanwhile] = useState(false);
  function toggle(id: number) {
    setChangedMeanwhile(false);
    setOpen(open === id ? null : id);
  }
  if (wishes.length === 0) return <p className="muted">No wishes yet.</p>;
  return (
    <ul className="list">
      {wishes.map((e) => {
        const title = wishTitle(e.description);
        return (
          <li key={e.id} className="list-item stackable">
            <div className="managed-row">
              <span className="managed-wish">
                {linkToWish ? <Link to={`/expressions/${e.id}`}>{title}</Link> : <strong>{title}</strong>}{" "}
                <span className="muted small">
                  by {e.creator.name}
                  {e.implementer && <> · provided by {e.implementer.name}</>}
                </span>{" "}
                <StatusChip status={e.status} />
              </span>
              <span className="list-actions">
                <button className="link" onClick={() => toggle(e.id)} aria-expanded={open === e.id}>
                  {open === e.id ? "Close" : "Manage"}
                </button>
              </span>
            </div>
            {open === e.id && (
              <>
                {changedMeanwhile && (
                  <p className="error" role="alert">
                    Someone else changed this wish meanwhile. It now shows as it is; check it and try again.
                  </p>
                )}
                <ManageWish
                  key={`${e.id}:${e.version}`}
                  expression={e}
                  members={members}
                  onChanged={() => {
                    setChangedMeanwhile(false);
                    onChange();
                  }}
                  onDeleted={() => {
                    setOpen(null);
                    onChange();
                  }}
                  onChangedMeanwhile={() => {
                    setChangedMeanwhile(true);
                    onChange();
                  }}
                />
              </>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/**
 * For the site administrator anywhere and the group admin in their group: any status, or
 * delete, with a reason that goes into the comments and to those concerned. A wish moved on
 * from Expressed needs a provider, chosen from the group's members. Both send the version the
 * admin saw; a refusal because the wish changed meanwhile goes to onChangedMeanwhile. Give it a
 * key that changes with the version, so the choices start again after each change.
 */
export function ManageWish({
  expression: e,
  members,
  onChanged,
  onDeleted,
  onChangedMeanwhile,
}: {
  expression: ExpressionView;
  members: Person[];
  onChanged: () => void;
  onDeleted: () => void;
  onChangedMeanwhile: () => void;
}) {
  const ask = useConfirm();
  const [status, setStatus] = useState<ExpressionStatus>(e.status);
  const [providerId, setProviderId] = useState("");
  const [reason, setReason] = useState("");
  const needsProvider = status !== "EXPRESSED" && !e.implementer;
  const candidates = members.filter((m) => m.id !== e.creator.id);

  const change = useMutation({
    mutationFn: () =>
      api<ExpressionView>(`/api/expressions/${e.id}/manage/status`, "PUT", {
        status,
        providerId: needsProvider ? Number(providerId) : null,
        reason,
        version: e.version,
      }),
    onSuccess: onChanged,
    onError: (error) => {
      if (isChangedMeanwhile(error)) onChangedMeanwhile();
    },
  });
  const remove = useMutation({
    mutationFn: () => api<void>(`/api/expressions/${e.id}/manage/delete`, "POST", { reason, version: e.version }),
    onSuccess: onDeleted,
    onError: (error) => {
      if (isChangedMeanwhile(error)) onChangedMeanwhile();
    },
  });
  const error = change.error ?? remove.error;

  const hasReason = reason.trim().length > 0;
  const busy = change.isPending || remove.isPending;
  return (
    <form
      onSubmit={(ev) => {
        ev.preventDefault();
        change.mutate();
      }}
      className="stack"
    >
      <Field label="Status">
        <select value={status} onChange={(ev) => setStatus(ev.target.value as ExpressionStatus)}>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {EXPRESSION_LABELS[s]}
            </option>
          ))}
        </select>
      </Field>
      {needsProvider && (
        <Field label="Provided by" hint="Nobody has taken care of this wish yet">
          <select value={providerId} onChange={(ev) => setProviderId(ev.target.value)} required>
            <option value="">Choose a member</option>
            {candidates.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name}
              </option>
            ))}
          </select>
        </Field>
      )}
      <Field label="Reason" hint="Added to the comments and sent to the people involved">
        <input value={reason} onChange={(ev) => setReason(ev.target.value)} required maxLength={280} />
      </Field>
      <ErrorText error={isChangedMeanwhile(error) ? null : error} />
      <div className="button-row">
        <button type="submit" className="primary" disabled={busy || status === e.status || !hasReason}>
          Change status
        </button>
        <button
          type="button"
          className="link danger"
          disabled={busy || !hasReason}
          onClick={async () => {
            const ok = await ask({
              title: "Delete this wish?",
              message: "Its comments go too. This can't be undone.",
              confirmLabel: "Delete wish",
              danger: true,
            });
            if (ok) remove.mutate();
          }}
        >
          Delete wish
        </button>
      </div>
    </form>
  );
}

/** 409: the wish is no longer as the page showed it. The list reloads and says so. */
function isChangedMeanwhile(error: unknown): boolean {
  return error instanceof ApiError && error.status === 409;
}
