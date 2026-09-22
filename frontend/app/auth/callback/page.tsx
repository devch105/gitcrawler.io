"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { apiFetch } from "@/lib/api";
import { POST_LOGIN_REDIRECT_KEY } from "@/lib/constants";
import type { AuthUser } from "@/types/auth";

export default function AuthCallbackPage() {
  const router = useRouter();
  const params = useSearchParams();

  const ran = useRef(false);

  const backendError = params.get("error");

  const errorMessage =
    backendError === "access_denied"
      ? "You cancelled the GitHub authorization."
      : backendError === "session_verification_failed"
        ? "Sign-in completed but your session couldn't be verified."
        : backendError
          ? "We couldn't complete sign-in. Please try again."
          : null;

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    // Backend/GitHub returned an error.
    if (backendError) {
      return;
    }

    (async () => {
      try {
        // The cookie is already set by the backend's redirect response.
        // This call confirms that the session is valid.
        const user =   await apiFetch<AuthUser>("/auth/me");
        console.log("User : ",user);
        const dest =
          sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY) ?? "/dashboard";

        sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);

        router.replace(dest);
        router.refresh();
      } catch (error) {

        console.error("AUTH ME FAILED:", error);
        router.replace("/auth/login?error=session_verification_failed");
      }
    })();
  }, [backendError, router]);

  if (errorMessage) {
    return (
      <main className="flex min-h-dvh flex-col items-center justify-center gap-4 bg-neutral-950 px-4">
        <p className="max-w-sm text-center text-sm text-neutral-300">
          {errorMessage}
        </p>

        <a
          href="/login"
          className="text-sm font-medium text-blue-400 hover:underline"
        >
          Back to sign in
        </a>
      </main>
    );
  }

  return (
    <main className="flex min-h-dvh items-center justify-center bg-neutral-950">
      <div className="flex items-center gap-3 text-sm text-neutral-400">
        <Loader2 className="size-4 animate-spin" />
        Finishing sign-in…
      </div>
    </main>
  );
}