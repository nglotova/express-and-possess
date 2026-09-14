import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "./client";

function respond(status: number, body?: unknown) {
  return Promise.resolve(
    new Response(body === undefined ? null : JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/problem+json" },
    }),
  );
}

describe("api client", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  });

  it("echoes the CSRF cookie in the header on writes but not on reads", async () => {
    document.cookie = "XSRF-TOKEN=abc123";
    const fetchMock = vi.spyOn(globalThis, "fetch").mockImplementation(() => respond(200, { ok: true }));

    await api("/api/me");
    await api("/api/groups", "POST", { name: "Family" });

    const [, readInit] = fetchMock.mock.calls[0];
    const [, writeInit] = fetchMock.mock.calls[1];
    expect((readInit!.headers as Record<string, string>)["X-XSRF-TOKEN"]).toBeUndefined();
    expect((writeInit!.headers as Record<string, string>)["X-XSRF-TOKEN"]).toBe("abc123");
    expect(writeInit!.body).toBe(JSON.stringify({ name: "Family" }));
    expect(writeInit!.credentials).toBe("same-origin");
  });

  it("turns a problem response into an ApiError with the detail", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() =>
      respond(409, { detail: "Someone else has already taken care of this wish" }),
    );

    await expect(api("/api/expressions/1/take-care", "POST")).rejects.toMatchObject(
      new ApiError(409, "Someone else has already taken care of this wish"),
    );
  });

  it("returns undefined for 204", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() => respond(204));
    await expect(api("/api/auth/logout", "POST")).resolves.toBeUndefined();
  });
});
