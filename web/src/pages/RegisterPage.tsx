import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../api/client";
import type { User } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { NewPasswordFields, newPasswordProblem } from "../components/NewPassword";

/**
 * Registration. An invitation link arrives here with the email filled in and a `next`
 * that returns to the invitation once the account exists.
 */
export function RegisterPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const next = params.get("next") ?? "/";
  const [email, setEmail] = useState(params.get("email") ?? "");
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [repeat, setRepeat] = useState("");
  const [problem, setProblem] = useState<string | null>(null);

  const register = useMutation({
    mutationFn: () => api<User>("/api/auth/register", "POST", { email, password, name }),
    onSuccess: (user) => {
      queryClient.setQueryData(["me"], user);
      navigate(next, { replace: true });
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    const found = newPasswordProblem(password, repeat);
    setProblem(found);
    if (!found) register.mutate();
  }

  return (
    <main className="page narrow">
      <h1 className="brand-title">Create your account</h1>
      <form onSubmit={submit} className="stack">
        <Field label="Email">
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </Field>
        <Field label="Name" hint="First and last name, or a nickname your group knows you by">
          <input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required maxLength={100} />
        </Field>
        <NewPasswordFields label="Password" password={password} repeat={repeat} onPassword={setPassword} onRepeat={setRepeat} />
        {problem && (
          <p className="error" role="alert">
            {problem}
          </p>
        )}
        <ErrorText error={register.error} />
        <button type="submit" className="primary" disabled={register.isPending}>
          Register
        </button>
      </form>
      <p className="links">
        <Link to="/login">Already have an account? Log in</Link>
      </p>
    </main>
  );
}
