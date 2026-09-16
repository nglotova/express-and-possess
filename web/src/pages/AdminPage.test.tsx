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
const groups = [{ id: 1, name: "Family", ownerName: "Natasha", status: "ARCHIVED", memberCount: 3 }];

describe("Administration page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists users with their state and groups including archived ones", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
      const body = String(input).includes("/users") ? users : groups;
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
  });
});
