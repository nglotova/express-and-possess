import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NewExpressionPage } from "./NewExpressionPage";

const PAN = "https://www.amazon.ca/JEETEE-Nonstick-Cookware-Induction-Compatible/dp/B082W225F2?th=1";
const TITLE = "JEETEE 8 inch Nonstick Frying Pan";

function renderPage() {
  const calls: string[] = [];
  vi.spyOn(globalThis, "fetch").mockImplementation((input) => {
    const url = String(input);
    calls.push(url);
    const body = url.includes("/api/link-preview")
      ? { url: PAN, site: "amazon.ca", title: TITLE, pictureUrl: "/api/files/pan.jpg" }
      : { id: 1, name: "Family", members: [] };
    return Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { "Content-Type": "application/json" } }));
  });
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={["/groups/1/expressions/new"]}>
        <Routes>
          <Route path="/groups/:id/expressions/new" element={<NewExpressionPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return calls;
}

describe("New wish", () => {
  afterEach(() => vi.restoreAllMocks());

  it("previews a pasted link with its picture and offers the product's title", async () => {
    const calls = renderPage();

    fireEvent.change(screen.getByLabelText(/^Description/), { target: { value: PAN } });

    expect(await screen.findByText(TITLE)).toBeInTheDocument();
    expect(calls).toContain(`/api/link-preview?url=${encodeURIComponent(PAN)}`);
    expect(screen.getByRole("link", { name: "↗ amazon.ca" })).toHaveAttribute("href", PAN);
    expect(document.querySelectorAll('img[src="/api/files/pan.jpg"]').length).toBe(2);
    expect(screen.getByText("Use my own picture")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Add this title to the description" }));

    expect(screen.getByLabelText(/^Description/)).toHaveValue(`${TITLE}\n${PAN}`);
    expect(screen.queryByRole("button", { name: "Add this title to the description" })).not.toBeInTheDocument();
  });

  it("takes a picture from the clipboard, by the button or by pasting, and says so when there is none", async () => {
    // jsdom has no object URLs.
    Object.assign(URL, { createObjectURL: () => "blob:pasted", revokeObjectURL: () => {} });
    renderPage();
    const picture = new Blob(["png"], { type: "image/png" });
    const read = vi.fn().mockResolvedValueOnce([]).mockResolvedValueOnce([
      { types: ["text/html", "image/png"], getType: () => Promise.resolve(picture) },
    ]);
    Object.defineProperty(navigator, "clipboard", { value: { read }, configurable: true });

    fireEvent.click(screen.getByRole("button", { name: "Paste a picture" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("There is no picture on the clipboard");

    fireEvent.click(screen.getByRole("button", { name: "Paste a picture" }));
    await waitFor(() => expect(document.querySelector('img[src="blob:pasted"]')).not.toBeNull());
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("Choose another")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Remove" }));
    const file = new File(["png"], "copied.png", { type: "image/png" });
    fireEvent.paste(document, { clipboardData: { files: [file] } });
    await waitFor(() => expect(document.querySelector('img[src="blob:pasted"]')).not.toBeNull());
  });
});
