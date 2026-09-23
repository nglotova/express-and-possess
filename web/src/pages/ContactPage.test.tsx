import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ContactPage } from "./ContactPage";
import type { User } from "../api/types";

const me: User = { id: 1, email: "lev@example.com", name: "Lev", role: "MEMBER", emailEnabled: true, enabled: true };

describe("Contact us", () => {
  afterEach(() => vi.restoreAllMocks());

  it("sends the topic, the message and the page the member came from", async () => {
    const fetch = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    client.setQueryData(["me"], me);
    render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[{ pathname: "/contact", state: { from: "/expressions/7" } }]}>
          <ContactPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    fireEvent.click(screen.getByLabelText("A suggestion"));
    fireEvent.change(screen.getByLabelText("Message"), { target: { value: "A dark theme, please" } });
    fireEvent.click(screen.getByRole("button", { name: "Send" }));

    expect(await screen.findByText("Thank you! Your message has reached the site administrator.")).toBeInTheDocument();
    const call = fetch.mock.calls.find(([input]) => String(input) === "/api/contact");
    await waitFor(() => expect(call).toBeDefined());
    expect(JSON.parse(String(call![1]!.body))).toEqual({
      topic: "SUGGESTION",
      body: "A dark theme, please",
      page: "/expressions/7",
    });
  });
});
