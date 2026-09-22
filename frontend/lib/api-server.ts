import { cookies } from "next/headers";
import { INTERNAL_API_BASE } from "./constants";
import { ApiError } from "./api";

/**
 * Server components have no ambient cookie jar — the incoming request's
 * cookies must be forwarded explicitly.
 */
export async function serverFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (cookieHeader) headers.set("Cookie", cookieHeader);

  const res = await fetch(`${INTERNAL_API_BASE}${path}`, {
    ...init,
    headers,
    cache: "no-store",
  });

  if (res.status === 204) return undefined as T;

  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, `Request failed: ${res.status}`, body);
  }
  return body as T;
}