import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { PasswordInput } from "./PasswordInput";

describe("password box", () => {
  it("shows and hides what was typed", () => {
    render(<PasswordInput value="secret" onChange={() => {}} autoComplete="current-password" />);
    const input = screen.getByDisplayValue("secret");

    expect(input).toHaveAttribute("type", "password");
    fireEvent.click(screen.getByRole("button", { name: "Show password" }));
    expect(input).toHaveAttribute("type", "text");
    fireEvent.click(screen.getByRole("button", { name: "Hide password" }));
    expect(input).toHaveAttribute("type", "password");
  });
});
