import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useLogout, useMe } from "../api/queries";
import type { User } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";

export function ProfilePage() {
  const me = useMe();
  const user = me.data!;
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const logout = useLogout();
  const [name, setName] = useState(user.name);
  const [emailEnabled, setEmailEnabled] = useState(user.emailEnabled);
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");

  const save = useMutation({
    mutationFn: () => api<User>("/api/me", "PUT", { name, emailEnabled }),
    onSuccess: (u) => queryClient.setQueryData(["me"], u),
  });
  const password = useMutation({
    mutationFn: () => api<void>("/api/me/password", "PUT", { currentPassword: current, newPassword: next }),
    onSuccess: () => {
      setCurrent("");
      setNext("");
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    save.mutate();
  }

  return (
    <>
      <PageHeader title="Profile" parent={{ to: "/", label: "My Groups" }} />
      <form onSubmit={submit} className="stack">
        <Field label="Name">
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={100} />
        </Field>
        <Field label="Email">
          <input value={user.email} disabled />
        </Field>
        <label className="check">
          <input type="checkbox" checked={emailEnabled} onChange={(e) => setEmailEnabled(e.target.checked)} />
          Send me notifications by email
        </label>
        <ErrorText error={save.error} />
        <button type="submit" className="primary" disabled={save.isPending}>
          Save
        </button>
        {save.isSuccess && <p className="muted">Saved.</p>}
      </form>

      <section className="section">
        <h2>Change password</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            password.mutate();
          }}
          className="stack"
        >
          <Field label="Current password">
            <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" required />
          </Field>
          <Field label="New password" hint="At least 8 characters">
            <input type="password" value={next} onChange={(e) => setNext(e.target.value)} autoComplete="new-password" minLength={8} required />
          </Field>
          <ErrorText error={password.error} />
          <button type="submit" disabled={password.isPending}>
            Change password
          </button>
          {password.isSuccess && <p className="muted">Password changed.</p>}
        </form>
      </section>

      <section className="section">
        <button
          className="danger"
          onClick={() => logout.mutate(undefined, { onSuccess: () => navigate("/login", { replace: true }) })}
        >
          Log out
        </button>
      </section>
    </>
  );
}
