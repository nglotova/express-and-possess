import { describe, expect, it, vi, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { GroupsPage } from "./GroupsPage";
import type { GroupSummary } from "../api/types";

const groups: GroupSummary[] = [
  { id: 1, name: "Family", ownerName: "Natasha", status: "WORKING", myRole: "ADMIN", hasUntaken: true, implementing: true },
  { id: 2, name: "Grandparents", ownerName: "Natasha", status: "NEW", myRole: "MEMBER", hasUntaken: false, implementing: false },
];

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <GroupsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("My Groups", () => {
  afterEach(() => vi.restoreAllMocks());

  it("lists the groups with their status and attention marks", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(groups), { status: 200, headers: { "Content-Type": "application/json" } }),
    );
    renderPage();

    expect(await screen.findByText("Family")).toBeInTheDocument();
    expect(screen.getByText("Grandparents")).toBeInTheDocument();
    expect(screen.getByText("Working")).toBeInTheDocument();
    expect(screen.getByText("New")).toBeInTheDocument();
    expect(screen.getAllByLabelText("Untaken wishes")).toHaveLength(1);
    expect(screen.getAllByLabelText("You are implementing")).toHaveLength(1);
    expect(screen.getByRole("link", { name: "Create group" })).toHaveAttribute("href", "/groups/new");
  });

  it("explains an empty list", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response("[]", { status: 200, headers: { "Content-Type": "application/json" } }),
    );
    renderPage();
    expect(await screen.findByText(/not in any group yet/)).toBeInTheDocument();
  });
});
