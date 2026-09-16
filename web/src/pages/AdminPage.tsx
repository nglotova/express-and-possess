import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import type { ExpressionStatus, ExpressionView, MemberView, Role, User } from "../api/types";
import { ErrorText } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { StatusChip } from "../components/StatusChip";
import { useConfirm } from "../components/ConfirmDialog";

interface AdminGroupRow {
  id: number;
  name: string;
  ownerName: string;
  status: "ACTIVE" | "CLOSED" | "ARCHIVED";
  memberCount: number;
}

interface AdminGroupDetail {
  id: number;
  name: string;
  status: "ACTIVE" | "CLOSED" | "ARCHIVED";
  members: MemberView[];
  expressions: ExpressionView[];
}

const STATUSES: ExpressionStatus[] = ["EXPRESSED", "IN_PROCESS", "PROVIDED", "IN_POSSESSION"];

/** Section 11 of the spec: users, groups including archived ones, stuck expressions. */
export function AdminPage() {
  return (
    <>
      <PageHeader title="Administration" parent={{ to: "/", label: "My Groups" }} />
      <Users />
      <Groups />
    </>
  );
}

function Users() {
  const [q, setQ] = useState("");
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();
  const users = useQuery({
    queryKey: ["admin", "users", search],
    queryFn: () => api<User[]>(`/api/admin/users?q=${encodeURIComponent(search)}`),
  });
  const update = useMutation({
    mutationFn: ({ id, enabled, role }: { id: number; enabled: boolean; role: Role }) =>
      api<User>(`/api/admin/users/${id}`, "PUT", { enabled, role }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "users"] }),
  });
  function submit(e: FormEvent) {
    e.preventDefault();
    setSearch(q);
  }
  return (
    <section className="section">
      <h2>Users</h2>
      <form onSubmit={submit} className="inline-form">
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Search by name or email" aria-label="Search users" />
        <button type="submit">Search</button>
      </form>
      <ErrorText error={update.error} />
      <ul className="list">
        {users.data?.map((u) => (
          <li key={u.id} className="list-item">
            <span>
              {u.name} <span className="muted small">{u.email}</span>
              {!u.enabled && <span className="chip chip-group-closed"> disabled</span>}
              {u.role === "ADMIN" && <span className="chip chip-expressed"> admin</span>}
            </span>
            <span className="list-actions">
              <button className="link" onClick={() => update.mutate({ id: u.id, enabled: !u.enabled, role: u.role })}>
                {u.enabled ? "Disable" : "Enable"}
              </button>
              <button
                className="link"
                onClick={() => update.mutate({ id: u.id, enabled: u.enabled, role: u.role === "ADMIN" ? "MEMBER" : "ADMIN" })}
              >
                {u.role === "ADMIN" ? "Revoke admin" : "Make admin"}
              </button>
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Groups() {
  const queryClient = useQueryClient();
  const ask = useConfirm();
  const groups = useQuery({ queryKey: ["admin", "groups"], queryFn: () => api<AdminGroupRow[]>("/api/admin/groups") });
  const [open, setOpen] = useState<number | null>(null);
  const act = useMutation({
    mutationFn: ({ path, method }: { path: string; method: "POST" | "DELETE" }) => api<void>(path, method),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "groups"] }),
  });
  return (
    <section className="section">
      <h2>Groups</h2>
      <ErrorText error={act.error} />
      <ul className="list">
        {groups.data?.map((g) => (
          <li key={g.id} className="list-item stackable">
            <div className="list-row">
              <button className="link" onClick={() => setOpen(open === g.id ? null : g.id)}>
                {g.name}
              </button>{" "}
              <span className="muted small">
                {g.ownerName} · {g.memberCount} member{g.memberCount === 1 ? "" : "s"} · {g.status.toLowerCase()}
              </span>
              <span className="list-actions">
                {g.status === "ARCHIVED" && (
                  <button className="link" onClick={() => act.mutate({ path: `/api/admin/groups/${g.id}/restore`, method: "POST" })}>
                    Restore
                  </button>
                )}
                <button
                  className="link danger"
                  onClick={async () => {
                    const ok = await ask({
                      title: `Delete ${g.name}?`,
                      message: "Every wish and comment in it goes too. This can't be undone.",
                      confirmLabel: "Delete group",
                      danger: true,
                    });
                    if (ok) act.mutate({ path: `/api/admin/groups/${g.id}`, method: "DELETE" });
                  }}
                >
                  Delete
                </button>
              </span>
            </div>
            {open === g.id && <GroupExpressions id={g.id} />}
          </li>
        ))}
      </ul>
    </section>
  );
}

function GroupExpressions({ id }: { id: number }) {
  const detail = useQuery({ queryKey: ["admin", "groups", id], queryFn: () => api<AdminGroupDetail>(`/api/admin/groups/${id}`) });
  if (detail.isPending) return <p className="muted">Loading…</p>;
  if (!detail.data) return <p className="error">Not available.</p>;
  return (
    <div className="admin-expressions">
      <p className="muted small">Members: {detail.data.members.map((m) => m.name).join(", ")}</p>
      {detail.data.expressions.length === 0 && <p className="muted small">No expressions.</p>}
      {detail.data.expressions.map((e) => (
        <ForceStatus key={e.id} expression={e} groupId={id} />
      ))}
    </div>
  );
}

function ForceStatus({ expression: e, groupId }: { expression: ExpressionView; groupId: number }) {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<ExpressionStatus>(e.status);
  const [reason, setReason] = useState("");
  const force = useMutation({
    mutationFn: () => api<ExpressionView>(`/api/admin/expressions/${e.id}/status`, "PUT", { status, reason }),
    onSuccess: () => {
      setReason("");
      queryClient.invalidateQueries({ queryKey: ["admin", "groups", groupId] });
    },
  });
  return (
    <form
      onSubmit={(ev) => {
        ev.preventDefault();
        force.mutate();
      }}
      className="force-status"
    >
      <div>
        <strong>{e.description.split("\n")[0]}</strong> <span className="muted small">by {e.creator.name}</span>
        {e.implementer && <span className="muted small"> · care: {e.implementer.name}</span>} <StatusChip status={e.status} />
      </div>
      <div className="inline-form three">
        <select value={status} onChange={(ev) => setStatus(ev.target.value as ExpressionStatus)} aria-label="New status">
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <input value={reason} onChange={(ev) => setReason(ev.target.value)} placeholder="Reason (required)" required maxLength={500} aria-label="Reason" />
        <button type="submit" disabled={force.isPending || status === e.status}>
          Force
        </button>
      </div>
      <ErrorText error={force.error} />
    </form>
  );
}
