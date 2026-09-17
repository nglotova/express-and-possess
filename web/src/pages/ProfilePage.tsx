import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useLogout, useMe } from "../api/queries";
import type { User } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";
import { NewPasswordFields, newPasswordProblem } from "../components/NewPassword";

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
  const [repeat, setRepeat] = useState("");
  const [problem, setProblem] = useState<string | null>(null);

  const save = useMutation({
    mutationFn: () => api<User>("/api/me", "PUT", { name, emailEnabled }),
    onSuccess: (u) => queryClient.setQueryData(["me"], u),
  });
  const password = useMutation({
    mutationFn: () => api<void>("/api/me/password", "PUT", { currentPassword: current, newPassword: next }),
    onSuccess: () => {
      setCurrent("");
      setNext("");
      setRepeat("");
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
            const found = newPasswordProblem(next, repeat);
            setProblem(found);
            if (!found) password.mutate();
          }}
          className="stack"
        >
          <Field label="Current password">
            <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)} autoComplete="current-password" required />
          </Field>
          <NewPasswordFields label="New password" password={next} repeat={repeat} onPassword={setNext} onRepeat={setRepeat} />
          {problem && (
            <p className="error" role="alert">
              {problem}
            </p>
          )}
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
