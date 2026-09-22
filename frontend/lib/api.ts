// export type User = {
//   id: string;
//   githubId: number;
//   githubUsername: string;
//   displayName: string;
//   avatarUrl: string | null;
// };




import { PUBLIC_API_BASE } from "./constants";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly body?: unknown,
  ) {
    super(message);
    this.name = "ApiError";
  }

  get isUnauthorized() {
    return this.status === 401;
  }

  /** Session is valid but GitHub scopes are insufficient. */
  get isForbidden() {
    return this.status === 403;
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${name}=([^;]*)`),
  );
  return match ? decodeURIComponent(match[1]) : null;
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  // Spring Security CSRF: with CookieCsrfTokenRepository.withHttpOnlyFalse(),
  // the token is readable from JS and must be echoed on every mutating request.
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = readCookie("XSRF-TOKEN");
    if (csrf) headers.set("X-XSRF-TOKEN", csrf);
  }

  const res = await fetch(`${PUBLIC_API_BASE}${path}`, {
    ...init,
    method,
    headers,
    credentials: "include", // <- without this, no cookie is sent. Ever.
    cache: "no-store",
  });

  if (res.status === 204) return undefined as T;

  const isJson = res.headers
    .get("content-type")
    ?.includes("application/json");
  const body = isJson ? await res.json().catch(() => null) : null;

  if (!res.ok) {
    const message =
      (body as { message?: string } | null)?.message ??
      `Request failed: ${res.status} ${res.statusText}`;
    throw new ApiError(res.status, message, body);
  }

  return body as T;
}