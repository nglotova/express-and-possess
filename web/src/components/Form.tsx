import type { ReactNode } from "react";
import { ApiError } from "../api/client";

/** A label above its control, the only form layout the app uses. */
export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {hint && <span className="field-hint">{hint}</span>}
    </label>
  );
}

/** The message of a failed request, or nothing. */
export function ErrorText({ error }: { error: unknown }) {
  if (!error) return null;
  const message = error instanceof ApiError ? error.message : "Something went wrong. Try again.";
  return (
    <p className="error" role="alert">
      {message}
    </p>
  );
}
