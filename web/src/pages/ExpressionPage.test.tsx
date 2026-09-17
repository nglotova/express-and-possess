import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ExpressionPage } from "./ExpressionPage";
import { ConfirmProvider } from "../components/ConfirmDialog";
import type { ExpressionView } from "../api/types";

const base: ExpressionView = {
  id: 7,
  groupId: 1,
  creator: { id: 1, name: "Natasha" },
  implementer: null,
  status: "EXPRESSED",
  description: "Kindle case, like this: https://example.com/case",
  pictureUrl: null,
  pictureFromLink: false,
  picturePending: false,
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
    const body = url.includes("/api/groups/")
      ? { id: 1, name: "Family", members: [] }
      : url.includes("/api/link-preview")
        ? { url: "", site: "", title: "Preview title", pictureUrl: null }
        : view;
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
  });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/expressions/7"]}>
        <ConfirmProvider>
          <Routes>
            <Route path="/expressions/:id" element={<ExpressionPage />} />
          </Routes>
        </ConfirmProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Expression page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows the links found in the description and follows them as the creator types", async () => {
    renderWith({
      ...base,
      canTakeCare: false,
      canEditWish: true,
      canDelete: true,
      description: "Mini chainsaw https://www.amazon.ca/gp/product/B0CMSYQM49",
    });
    expect(await screen.findByRole("link", { name: "↗ amazon.ca" })).toHaveAttribute(
      "href",
      "https://www.amazon.ca/gp/product/B0CMSYQM49",
    );
    expect(await screen.findByText("Preview title")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/^Description/), {
      target: { value: "Mini chainsaw https://shop.one/a or this one: https://shop.two/b." },
    });

    expect(screen.queryByRole("link", { name: "↗ amazon.ca" })).not.toBeInTheDocument();
    expect(await screen.findByRole("link", { name: "↗ shop.one" })).toHaveAttribute("href", "https://shop.one/a");
    expect(screen.getByRole("link", { name: "↗ shop.two" })).toHaveAttribute("href", "https://shop.two/b");
  });

  it("asks in the app's own dialog before deleting a wish", async () => {
    renderWith({ ...base, canTakeCare: false, canEditWish: true, canDelete: true });
    const deleteRequests = () =>
      vi.mocked(globalThis.fetch).mock.calls.filter(([, init]) => init?.method === "DELETE").length;

    fireEvent.click(await screen.findByRole("button", { name: "Delete" }));
    expect(screen.getByRole("alertdialog", { name: "Delete this wish?" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(deleteRequests()).toBe(0);

    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    fireEvent.click(within(screen.getByRole("alertdialog")).getByRole("button", { name: "Delete" }));
    await waitFor(() => expect(deleteRequests()).toBe(1));
  });

  it("offers to take care, with the incognito choice, to a member who is not the creator", async () => {
    renderWith(base);
    expect(await screen.findByRole("button", { name: "I'll take care of it" })).toBeInTheDocument();
    expect(screen.getByLabelText(/Incognito/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "↗ example.com" })).toHaveAttribute("href", "https://example.com/case");
    expect(screen.getByRole("link", { name: "↗ example.com" })).toHaveAttribute("target", "_blank");
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
