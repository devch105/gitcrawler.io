import "server-only";
import { cache } from "react";
import type { AuthUser } from "@/types/auth";
import { serverFetch } from "./api-server";
import { ApiError } from "./api";

/**
 * React.cache dedupes this across a single render pass, so a layout and
 * three child components calling getSession() produce one backend request.
 */
export const getSession = cache(async (): Promise<AuthUser | null> => {
  try {
    return await serverFetch<AuthUser>("/api/auth/me");
  } catch (err) {
    if (err instanceof ApiError && err.status === 401) return null;
    throw err; // backend down is not the same as logged out — don't swallow it
  }
});