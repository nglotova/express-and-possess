import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import { useGroup, useInvalidateGroups, useMe } from "../api/queries";
import type { GroupDetail } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { GroupStatusChip } from "../components/StatusChip";

/**
 * Create a group, or edit one. The admin manages members here: invitations by email, the
 * share link, removal, and handing over. Other members see the list and a Leave button.
 */
export function GroupEditPage() {
  const params = useParams();
  const id = params.id ? Number(params.id) : undefined;
  return id === undefined ? <CreateGroup /> : <EditGroup id={id} />;
}

function CreateGroup() {
  const navigate = useNavigate();
  const invalidate = useInvalidateGroups();
  const [name, setName] = useState("");
  const create = useMutation({
    mutationFn: () => api<GroupDetail>("/api/groups", "POST", { name }),
    onSuccess: (group) => {
      invalidate();
      navigate(`/groups/${group.id}/edit`, { replace: true });
    },
  });
  function submit(e: FormEvent) {
    e.preventDefault();
    create.mutate();
  }
  return (
    <>
      <PageHeader title="Create group" parent={{ to: "/", label: "My Groups" }} />
      <form onSubmit={submit} className="stack">
        <Field label="Name">
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} autoFocus />
        </Field>
        <ErrorText error={create.error} />
        <button type="submit" className="primary" disabled={create.isPending}>
          Save
        </button>
      </form>
      <p className="muted">You can invite members on the next screen.</p>
    </>
  );
}

function EditGroup({ id }: { id: number }) {
  const group = useGroup(id);
  const me = useMe();
  if (group.isPending) return <p className="muted">Loading…</p>;
  if (group.isError || !group.data) return <p className="error">This group is not available.</p>;
  const g = group.data;
  const admin = g.myRole === "ADMIN";
  return (
    <>
      <PageHeader title={admin ? "Edit group" : g.name} parent={{ to: "/", label: "My Groups" }} />
      {admin ? <NameForm group={g} /> : <p className="muted">Owner: {g.members.find((m) => m.role === "ADMIN")?.name}</p>}
      <p>
        <GroupStatusChip status={g.status} />
      </p>
      <Members group={g} meId={me.data!.id} />
      {admin && g.status !== "CLOSED" && (
        <>
          <Invite group={g} />
          <ShareLink group={g} />
        </>
      )}
      <Lifecycle group={g} />
    </>
  );
}

function NameForm({ group }: { group: GroupDetail }) {
  const invalidate = useInvalidateGroups();
  const [name, setName] = useState(group.name);
  useEffect(() => setName(group.name), [group.name]);
  const rename = useMutation({
    mutationFn: () => api<GroupDetail>(`/api/groups/${group.id}`, "PUT", { name }),
    onSuccess: invalidate,
  });
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        rename.mutate();
      }}
      className="inline-form"
    >
      <Field label="Name">
        <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
      </Field>
      <button type="submit" disabled={rename.isPending || name === group.name}>
        Save
      </button>
      <ErrorText error={rename.error} />
    </form>
  );
}

function Members({ group, meId }: { group: GroupDetail; meId: number }) {
  const navigate = useNavigate();
  const invalidate = useInvalidateGroups();
  const admin = group.myRole === "ADMIN";
  const call = useMutation({
    mutationFn: ({ path, method }: { path: string; method: "POST" | "DELETE" }) => api<void>(path, method),
    onSuccess: invalidate,
  });
  const leave = useMutation({
    mutationFn: () => api<void>(`/api/groups/${group.id}/leave`, "POST"),
    onSuccess: () => {
      invalidate();
      navigate("/", { replace: true });
    },
  });
  return (
    <section className="section">
      <h2>Members</h2>
      <ul className="list">
        {group.members.map((m) => (
          <li key={m.userId} className="list-item">
            <span>
              {m.name} <span className="muted">{m.role === "ADMIN" ? "admin" : "member"}</span>
            </span>
            {admin && m.userId !== meId && (
              <span className="list-actions">
                <button
                  className="link"
                  onClick={() => call.mutate({ path: `/api/groups/${group.id}/members/${m.userId}/make-admin`, method: "POST" })}
                >
                  Make admin
                </button>
                <button
                  className="link danger"
                  onClick={() => {
                    if (confirm(`Remove ${m.name} from ${group.name}?`)) {
                      call.mutate({ path: `/api/groups/${group.id}/members/${m.userId}`, method: "DELETE" });
                    }
                  }}
                >
                  Remove
                </button>
              </span>
            )}
          </li>
        ))}
        {group.invitations.map((i) => (
          <li key={`i${i.id}`} className="list-item">
            <span>
              {i.email} <span className="muted">invited, not joined</span>
            </span>
            <span className="list-actions">
              <button
                className="link"
                onClick={() => call.mutate({ path: `/api/groups/${group.id}/invitations/${i.id}`, method: "DELETE" })}
              >
                Cancel
              </button>
            </span>
          </li>
        ))}
      </ul>
      <ErrorText error={call.error ?? leave.error} />
      {!admin && (
        <button
          className="danger"
          onClick={() => {
            if (confirm(`Leave ${group.name}?`)) leave.mutate();
          }}
        >
          Leave group
        </button>
      )}
    </section>
  );
}

function Invite({ group }: { group: GroupDetail }) {
  const invalidate = useInvalidateGroups();
  const [email, setEmail] = useState("");
  const invite = useMutation({
    mutationFn: () => api<{ outcome: "ADDED" | "INVITED" }>(`/api/groups/${group.id}/invitations`, "POST", { email }),
    onSuccess: () => {
      setEmail("");
      invalidate();
    },
  });
  return (
    <section className="section">
      <h2>Invite by email</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          invite.mutate();
        }}
        className="inline-form"
      >
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="name@example.com"
          required
          aria-label="Email to invite"
        />
        <button type="submit" disabled={invite.isPending}>
          Send
        </button>
      </form>
      {invite.data?.outcome === "ADDED" && <p className="muted">Added: they already had an account.</p>}
      {invite.data?.outcome === "INVITED" && <p className="muted">Invitation sent. It expires in 7 days.</p>}
      <ErrorText error={invite.error} />
    </section>
  );
}

function ShareLink({ group }: { group: GroupDetail }) {
  const invalidate = useInvalidateGroups();
  const [link, setLink] = useState<string | null>(null);
  const toggle = useMutation({
    mutationFn: (enabled: boolean) =>
      api<{ enabled: boolean; link: string | null }>(`/api/groups/${group.id}/share-link`, "PUT", { enabled }),
    onSuccess: (r) => {
      setLink(r.link);
      invalidate();
    },
  });
  return (
    <section className="section">
      <label className="check">
        <input
          type="checkbox"
          checked={group.shareLinkEnabled}
          onChange={(e) => toggle.mutate(e.target.checked)}
          disabled={toggle.isPending}
        />
        Anyone with the link can join
      </label>
      {group.shareLinkEnabled && link && (
        <p className="share">
          <code>{link}</code>
          <button className="link" onClick={() => navigator.clipboard?.writeText(link)}>
            Copy
          </button>
        </p>
      )}
      {group.shareLinkEnabled && !link && (
        <p className="muted">The link was created earlier. Untick and tick again to make a new one.</p>
      )}
      <ErrorText error={toggle.error} />
    </section>
  );
}

function Lifecycle({ group }: { group: GroupDetail }) {
  const navigate = useNavigate();
  const invalidate = useInvalidateGroups();
  const act = useMutation({
    mutationFn: ({ path, method }: { path: string; method: "POST" | "DELETE" }) => api<void>(path, method),
    onSuccess: (_r, vars) => {
      invalidate();
      if (vars.path.endsWith("/archive") || vars.method === "DELETE") navigate("/", { replace: true });
    },
  });
  if (group.myRole !== "ADMIN") return null;
  return (
    <section className="section">
      {group.status !== "CLOSED" ? (
        <button
          className="danger"
          onClick={() => {
            if (confirm(`Close ${group.name}? Nobody will be able to add or change wishes.`)) {
              act.mutate({ path: `/api/groups/${group.id}/close`, method: "POST" });
            }
          }}
        >
          Close group
        </button>
      ) : (
        <div className="button-row">
          <button onClick={() => act.mutate({ path: `/api/groups/${group.id}/archive`, method: "POST" })}>Archive group</button>
          <button
            className="danger"
            onClick={() => {
              if (confirm(`Delete ${group.name} and everything in it? This cannot be undone.`)) {
                act.mutate({ path: `/api/groups/${group.id}`, method: "DELETE" });
              }
            }}
          >
            Delete group
          </button>
        </div>
      )}
      <ErrorText error={act.error} />
    </section>
  );
}
