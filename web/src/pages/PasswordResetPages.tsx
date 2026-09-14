import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import { ErrorText, Field } from "../components/Form";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const request = useMutation({
    mutationFn: () => api<void>("/api/auth/password-reset/request", "POST", { email }),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    request.mutate();
  }

  return (
    <main className="page narrow">
      <h1 className="brand-title">Reset your password</h1>
      {request.isSuccess ? (
        <p>If that address has an account, an email with a reset link is on its way. The link works for one hour.</p>
      ) : (
        <form onSubmit={submit} className="stack">
          <Field label="Email">
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          </Field>
          <ErrorText error={request.error} />
          <button type="submit" className="primary" disabled={request.isPending}>
            Send reset link
          </button>
        </form>
      )}
      <p className="links">
        <Link to="/login">Back to log in</Link>
      </p>
    </main>
  );
}

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const [password, setPassword] = useState("");
  const confirm = useMutation({
    mutationFn: () => api<void>("/api/auth/password-reset/confirm", "POST", { token, newPassword: password }),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    confirm.mutate();
  }

  return (
    <main className="page narrow">
      <h1 className="brand-title">Choose a new password</h1>
      {confirm.isSuccess ? (
        <p>
          Done. <Link to="/login">Log in</Link> with your new password.
        </p>
      ) : (
        <form onSubmit={submit} className="stack">
          <Field label="New password" hint="At least 8 characters">
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              minLength={8}
              required
            />
          </Field>
          <ErrorText error={confirm.error} />
          <button type="submit" className="primary" disabled={confirm.isPending || !token}>
            Save password
          </button>
        </form>
      )}
    </main>
  );
}
