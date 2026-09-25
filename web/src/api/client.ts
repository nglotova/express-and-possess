/**
 * The one place the app talks HTTP. Sends the session cookie, echoes the CSRF cookie in
 * the header Spring Security expects, and turns problem responses into ApiError.
 */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

type Method = "GET" | "POST" | "PUT" | "DELETE";

function csrfToken(): string | undefined {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

export async function api<T>(path: string, method: Method = "GET", body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  const isForm = body instanceof FormData;
  if (body !== undefined && !isForm) {
    headers["Content-Type"] = "application/json";
  }
  if (method !== "GET") {
    let token = csrfToken();
    if (!token) {
      // The first request of a visit may be a write (registration). Any response sets the
      // cookie, so fetch something harmless first.
      await fetch("/api/auth/csrf", { credentials: "same-origin" }).catch(() => undefined);
      token = csrfToken();
    }
    if (token) {
      headers["X-XSRF-TOKEN"] = token;
    }
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
  });
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const problem = await response.json();
      if (typeof problem?.detail === "string") {
        detail = problem.detail;
      }
    } catch {
      // No JSON body; keep the status text.
    }
    throw new ApiError(response.status, detail);
  }
  // 202 Accepted and 204 No Content arrive without a body.
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
