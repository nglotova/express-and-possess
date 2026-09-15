import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ExpressionPage } from "./ExpressionPage";
import type { ExpressionView } from "../api/types";

const base: ExpressionView = {
  id: 7,
  groupId: 1,
  creator: { id: 1, name: "Natasha" },
  implementer: null,
  status: "EXPRESSED",
  description: "Kindle case, like this: https://example.com/case",
  links: ["https://example.com/case-2"],
  pictureUrl: null,
  wantedBy: "2026-12-01",
  providingBy: null,
  incognito: false,
  version: 0,
  canEditWish: false,
  canEditCare: false,
  canTakeCare: true,
  canRelease: false,
  canDelete: false,
  canMarkReceived: false,
  commentsOpen: true,
  comments: [],
};

function renderWith(view: ExpressionView) {
  vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
    const url = String(input);
    const body = url.includes("/api/groups/") ? { id: 1, name: "Family", members: [] } : view;
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
  });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/expressions/7"]}>
        <Routes>
          <Route path="/expressions/:id" element={<ExpressionPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Expression page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("offers Take Care with the incognito choice to a member who is not the creator", async () => {
    renderWith(base);
    expect(await screen.findByRole("button", { name: "Take care" })).toBeInTheDocument();
    expect(screen.getByLabelText(/Incognito/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "https://example.com/case" })).toHaveAttribute("target", "_blank");
    expect(screen.getByRole("link", { name: "↗ example.com" })).toHaveAttribute("href", "https://example.com/case-2");
    expect(screen.queryByRole("button", { name: "Submit" })).not.toBeInTheDocument();
  });

  it("shows a hidden helper as Incognito and locks the description for the creator", async () => {
    renderWith({
      ...base,
      status: "IN_PROCESS",
      implementer: { id: null, name: "Incognito" },
      canTakeCare: false,
      canEditWish: true,
      canDelete: true,
      comments: [{ id: 1, author: { id: null, name: "Anonymous helper" }, body: "Blue?", systemNote: false, createdAt: "2026-09-14T10:00:00Z" }],
    });
    expect(await screen.findByText("Incognito")).toBeInTheDocument();
    expect(screen.getByLabelText(/^Description/)).toBeDisabled();
    expect(screen.getByText(/Anonymous helper/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Delete" })).toBeInTheDocument();
  });

  it("is read-only once the wish is in possession", async () => {
    renderWith({ ...base, status: "IN_POSSESSION", implementer: { id: 2, name: "Andrei" }, canTakeCare: false, commentsOpen: false });
    expect(await screen.findByText("In Possession")).toBeInTheDocument();
    expect(screen.getByText("Comments are closed.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });
});
