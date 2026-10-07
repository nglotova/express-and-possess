import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import type { ExpressionStatus, ExpressionView } from "../api/types";
import { useConfirm } from "./ConfirmDialog";
import { ErrorText, Field } from "./Form";
import { EXPRESSION_LABELS } from "./StatusChip";

const STATUSES = Object.keys(EXPRESSION_LABELS) as ExpressionStatus[];

export interface Person {
  id: number;
  name: string;
}

/**
 * For the site administrator anywhere and the group admin in their group: any status, or
 * delete, with a reason that goes into the comments and to those concerned. A wish moved on
 * from Expressed needs a provider, chosen from the group's members. Give it a key that
 * changes with the status, so the choices start again after each change.
 */
export function ManageWish({
  expression: e,
  members,
  onChanged,
  onDeleted,
}: {
  expression: ExpressionView;
  members: Person[];
  onChanged: (view: ExpressionView) => void;
  onDeleted: () => void;
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
      }),
    onSuccess: onChanged,
  });
  const remove = useMutation({
    mutationFn: () => api<void>(`/api/expressions/${e.id}/manage/delete`, "POST", { reason }),
    onSuccess: onDeleted,
  });

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
      <ErrorText error={change.error ?? remove.error} />
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
