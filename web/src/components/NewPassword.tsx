import { Field } from "./Form";
import { PasswordInput } from "./PasswordInput";

/** The same rule the server checks when a password is set. */
const RULES: { label: string; passes: (password: string) => boolean }[] = [
  { label: "At least 8 characters", passes: (p) => p.length >= 8 },
  { label: "A capital letter", passes: (p) => /\p{Lu}/u.test(p) },
  { label: "A number", passes: (p) => /\p{Nd}/u.test(p) },
  { label: "A special symbol, such as ! or -", passes: (p) => /[^\p{L}\p{Nd}\s]/u.test(p) },
];

/** Why the pair can't be saved yet, or null when it can. */
export function newPasswordProblem(password: string, repeat: string): string | null {
  if (!RULES.every((r) => r.passes(password))) return "The password doesn't meet every rule below it.";
  if (password !== repeat) return "The two passwords differ.";
  return null;
}

/** A new password, its repeat, and the rule with each part ticked off as it is met. */
export function NewPasswordFields({
  label,
  password,
  repeat,
  onPassword,
  onRepeat,
}: {
  label: string;
  password: string;
  repeat: string;
  onPassword: (value: string) => void;
  onRepeat: (value: string) => void;
}) {
  return (
    <>
      <Field label={label}>
        <PasswordInput value={password} onChange={onPassword} autoComplete="new-password" describedBy="password-rules" />
      </Field>
      <ul id="password-rules" className="password-rules">
        {RULES.map((r) => {
          const met = r.passes(password);
          return (
            <li key={r.label} className={met ? "met" : undefined}>
              <span aria-hidden="true">{met ? "✓" : "○"}</span> {r.label}
              <span className="visually-hidden">{met ? ", done" : ", not yet"}</span>
            </li>
          );
        })}
      </ul>
      <Field label="Repeat password">
        <PasswordInput value={repeat} onChange={onRepeat} autoComplete="new-password" />
      </Field>
    </>
  );
}
