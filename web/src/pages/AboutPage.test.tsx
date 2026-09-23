import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AboutPage } from "./AboutPage";

describe("About page", () => {
  afterEach(() => vi.restoreAllMocks());

  it("explains the uses and invites a visitor to register", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <MemoryRouter>
          <AboutPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );

    for (const use of ["Family and friends", "Weddings and baby showers", "At work", "Shared to-do lists"]) {
      expect(screen.getByRole("heading", { name: use })).toBeInTheDocument();
    }
    expect(await screen.findByRole("link", { name: "Create an account" })).toHaveAttribute("href", "/register");
  });
});
