import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ProfilePage } from "./ProfilePage";
import type { User } from "../api/types";

const me: User = { id: 1, email: "natasha@example.com", name: "Natasha", role: "MEMBER", emailEnabled: true, enabled: true };

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  client.setQueryData(["me"], me);
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function fill(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

const passwordCalls = (calls: unknown[][]) => calls.filter(([input]) => String(input).includes("/api/me/password"));

describe("Profile page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("sends a new password only when it meets the rule and both fields match", async () => {
    const fetch = vi.spyOn(globalThis, "fetch").mockImplementation(() => Promise.resolve(new Response(null, { status: 204 })));
    renderPage();
    const change = screen.getByRole("button", { name: "Change password" });

    fill("Current password", "old");
    fill("New password", "longenough");
    expect(screen.getByText("At least 8 characters").closest("li")).toHaveClass("met");
    expect(screen.getByText("A capital letter").closest("li")).not.toHaveClass("met");
    fill("Repeat password", "longenough");
    fireEvent.click(change);
    expect(screen.getByRole("alert")).toHaveTextContent("The password doesn't meet every rule below it.");

    fill("New password", "Longenough-1");
    fill("Repeat password", "Longenough-2");
    fireEvent.click(change);
    expect(screen.getByRole("alert")).toHaveTextContent("The two passwords differ.");
    expect(passwordCalls(fetch.mock.calls)).toHaveLength(0);

    fill("Repeat password", "Longenough-1");
    fireEvent.click(change);
    await waitFor(() => expect(passwordCalls(fetch.mock.calls)).toHaveLength(1));
    expect(await screen.findByText("Password changed.")).toBeInTheDocument();
    expect(screen.getByLabelText("Repeat password")).toHaveValue("");
  });
});
