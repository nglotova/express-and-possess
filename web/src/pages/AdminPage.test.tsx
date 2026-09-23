import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AdminPage } from "./AdminPage";
import { ConfirmProvider } from "../components/ConfirmDialog";

const users = [
  { id: 1, email: "natasha@example.com", name: "Natasha", role: "ADMIN", emailEnabled: true, enabled: true },
  { id: 2, email: "lev@example.com", name: "Lev", role: "MEMBER", emailEnabled: true, enabled: false },
];
const messages = [
  {
    id: 1,
    senderName: "Lev",
    senderEmail: "lev@example.com",
    topic: "PROBLEM",
    body: "The picture does not load.",
    page: "/expressions/7",
    createdAt: "2026-09-23T10:00:00Z",
  },
];
const groups = [{ id: 1, name: "Family", ownerName: "Natasha", status: "ARCHIVED", memberCount: 3 }];

describe("Administration page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists messages, users, groups including archived ones, and the site settings", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
      const url = String(input);
      const body = url.includes("/users")
        ? users
        : url.includes("/settings")
          ? { invitationsPerDay: 20 }
          : url.includes("/messages")
            ? messages
            : groups;
      return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
    });
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <MemoryRouter>
          <ConfirmProvider>
            <AdminPage />
          </ConfirmProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Lev")).toBeInTheDocument();
    expect(screen.getByText("disabled")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Enable" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Revoke admin" })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "Family" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Restore" })).toBeInTheDocument();
    expect(await screen.findByText("The picture does not load.")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Reply to lev@example.com" })).toHaveAttribute(
      "href",
      expect.stringContaining("mailto:lev@example.com"),
    );
    expect(await screen.findByDisplayValue("20")).toHaveAccessibleName(/Invitation emails per member per day/);
  });
});
