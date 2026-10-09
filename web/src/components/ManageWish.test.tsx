import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ManagedWishes } from "./ManageWish";
import { ConfirmProvider } from "./ConfirmDialog";
import type { ExpressionView } from "../api/types";

const wish: ExpressionView = {
  id: 7,
  groupId: 1,
  creator: { id: 1, name: "Natasha" },
  implementer: null,
  status: "EXPRESSED",
  description: "Kindle case",
  pictureUrl: null,
  pictureFromLink: false,
  picturePending: false,
  wantedBy: null,
  providingBy: null,
  incognito: false,
  version: 0,
  canEditWish: false,
  canEditCare: false,
  canTakeCare: false,
  canRelease: false,
  canDelete: false,
  canMarkReceived: false,
  commentsOpen: true,
  comments: [],
};

function renderList(onChange = () => {}) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <ConfirmProvider>
          <ManagedWishes
            wishes={[wish]}
            members={[
              { id: 1, name: "Natasha" },
              { id: 2, name: "Andrei" },
            ]}
            linkToWish
            onChange={onChange}
          />
        </ConfirmProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("Managed wishes", () => {
  afterEach(() => vi.restoreAllMocks());

  it("moves a wish nobody took to Provided, naming who provided it", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ...wish, status: "PROVIDED" }), { status: 200, headers: { "Content-Type": "application/json" } }),
    );
    const onChange = vi.fn();
    renderList(onChange);
    expect(screen.getByRole("link", { name: "Kindle case" })).toHaveAttribute("href", "/expressions/7");
    expect(screen.queryByLabelText(/^Status/)).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Manage" }));
    const change = screen.getByRole("button", { name: "Change status" });
    expect(change).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/^Status/), { target: { value: "PROVIDED" } });
    const provider = screen.getByLabelText(/^Provided by/);
    // The creator cannot provide their own wish.
    expect(within(provider).queryByRole("option", { name: "Natasha" })).not.toBeInTheDocument();
    fireEvent.change(provider, { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText(/^Reason/), { target: { value: "Bought in a shop" } });
    fireEvent.click(change);

    await waitFor(() => expect(onChange).toHaveBeenCalled());
    expect(fetch).toHaveBeenCalledWith(
      "/api/expressions/7/manage/status",
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ status: "PROVIDED", providerId: 2, reason: "Bought in a shop", version: 0 }),
      }),
    );
  });

  it("says so and loads the list again when someone changed the wish meanwhile", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ detail: "This wish was changed by someone else; reload and try again" }), {
        status: 409,
        headers: { "Content-Type": "application/problem+json" },
      }),
    );
    const onChange = vi.fn();
    renderList(onChange);
    fireEvent.click(screen.getByRole("button", { name: "Manage" }));
    fireEvent.change(screen.getByLabelText(/^Reason/), { target: { value: "Posted twice" } });
    fireEvent.click(screen.getByRole("button", { name: "Delete wish" }));
    fireEvent.click(within(await screen.findByRole("alertdialog")).getByRole("button", { name: "Delete wish" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Someone else changed this wish meanwhile");
    expect(onChange).toHaveBeenCalled();
  });
});
