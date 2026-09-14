import { useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import { useMe } from "../api/queries";
import type { User } from "../api/types";
import { ErrorText, Field } from "../components/Form";

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
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
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
    </main>
  );
}
