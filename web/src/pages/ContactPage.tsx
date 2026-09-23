import { useState, type FormEvent } from "react";
import { Link, useLocation } from "react-router";
import { useMutation } from "@tanstack/react-query";
import { api } from "../api/client";
import { useMe } from "../api/queries";
import type { ContactTopic } from "../api/types";
import { ErrorText, Field } from "../components/Form";
import { PageHeader } from "../components/Layout";

const TOPICS: { value: ContactTopic; label: string }[] = [
  { value: "PROBLEM", label: "Something doesn't work" },
  { value: "SUGGESTION", label: "A suggestion" },
  { value: "OTHER", label: "Something else" },
];

/** Contact us: a message to the site administrators, who answer by email. */
export function ContactPage() {
  const me = useMe();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? null;
  const [topic, setTopic] = useState<ContactTopic>("PROBLEM");
  const [body, setBody] = useState("");
  const send = useMutation({
    mutationFn: () => api<void>("/api/contact", "POST", { topic, body, page: from }),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    send.mutate();
  }

  return (
    <>
      <PageHeader title="Contact us" parent={{ to: from ?? "/", label: "Back" }} />
      {send.isSuccess ? (
        <div className="stack">
          <p>Thank you! Your message has reached the site administrator.</p>
          <p className="muted">The answer will come to your email address, {me.data?.email}.</p>
          <Link to={from ?? "/"}>Back to where you were</Link>
        </div>
      ) : (
        <form onSubmit={submit} className="stack">
          <p className="muted">
            Found a problem, or have an idea? Write to the site administrator. The answer will come to your email
            address, {me.data?.email}.
          </p>
          <fieldset className="choices">
            <legend className="field-label">What is it about?</legend>
            {TOPICS.map((t) => (
              <label key={t.value} className="check">
                <input
                  type="radio"
                  name="topic"
                  value={t.value}
                  checked={topic === t.value}
                  onChange={() => setTopic(t.value)}
                />
                {t.label}
              </label>
            ))}
          </fieldset>
          <Field
            label="Message"
            hint={topic === "PROBLEM" ? "What did you do, and what happened? The page you came from is sent along." : undefined}
          >
            <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={6} maxLength={2000} required />
          </Field>
          <ErrorText error={send.error} />
          <button type="submit" className="primary" disabled={send.isPending || body.trim() === ""}>
            Send
          </button>
        </form>
      )}
    </>
  );
}
