import { useState } from "react";

/** A password box with a button that shows what was typed. */
export function PasswordInput({
  value,
  onChange,
  autoComplete,
  describedBy,
}: {
  value: string;
  onChange: (value: string) => void;
  autoComplete: "current-password" | "new-password";
  describedBy?: string;
}) {
  const [shown, setShown] = useState(false);
  return (
    <span className="password-box">
      <input
        type={shown ? "text" : "password"}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        aria-describedby={describedBy}
        required
      />
      <button
        type="button"
        className="reveal"
        onClick={() => setShown(!shown)}
        aria-label={shown ? "Hide password" : "Show password"}
        aria-pressed={shown}
        title={shown ? "Hide password" : "Show password"}
      >
        <span aria-hidden="true">{shown ? "🙈" : "👁"}</span>
      </button>
    </span>
  );
}
