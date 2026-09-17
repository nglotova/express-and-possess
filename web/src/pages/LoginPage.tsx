import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useMe } from "../api/queries";
import type { User } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PasswordInput } from "../components/PasswordInput";

export function LoginPage() {
  const me = useMe();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const next = (location.state as { next?: string } | null)?.next ?? "/";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const login = useMutation({
    mutationFn: () => api<User>("/api/auth/login", "POST", { email, password }),
    onSuccess: (user) => {
      queryClient.setQueryData(["me"], user);
      navigate(next, { replace: true });
    },
  });
  // On the public demo the API offers ready-made personas; elsewhere this says disabled.
  const demo = useQuery({
    queryKey: ["demo"],
    queryFn: () => api<{ enabled: boolean; personas: string[] }>("/api/auth/demo"),
    staleTime: Infinity,
  });
  const loginAs = useMutation({
    mutationFn: (persona: string) => api<User>(`/api/auth/demo/${persona}`, "POST"),
    onSuccess: (user) => {
      queryClient.setQueryData(["me"], user);
      navigate("/", { replace: true });
    },
  });

  if (me.data) {
    return <Navigate to="/" replace />;
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    login.mutate();
  }

  return (
    <main className="page narrow">
      <h1 className="brand-title">Express &amp; Possess</h1>
      <form onSubmit={submit} className="stack">
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </Field>
        <Field label="Password">
          <PasswordInput value={password} onChange={setPassword} autoComplete="current-password" />
        </Field>
        <ErrorText error={login.error} />
        <button type="submit" className="primary" disabled={login.isPending}>
          Log in
        </button>
      </form>
      <p className="links">
        <Link to="/register">New user?</Link>
        <Link to="/forgot-password">Forgot your password?</Link>
      </p>
      {demo.data?.enabled && (
        <section className="demo">
          <h2>This is the demo</h2>
          <p className="muted small">
            A fictional family, reset every night. Pick someone and look around; open a second browser as another
            member to see the notifications arrive.
          </p>
          <div className="button-row">
            {demo.data.personas.map((p) => (
              <button key={p} onClick={() => loginAs.mutate(p)} disabled={loginAs.isPending}>
                Log in as {p}
              </button>
            ))}
          </div>
          <ErrorText error={loginAs.error} />
        </section>
      )}
    </main>
  );
}
