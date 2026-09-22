"use client";

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { POST_LOGIN_REDIRECT_KEY } from "@/lib/constants";
import { apiFetch } from "@/lib/api";
import { GitOAuthURL } from "@/types/auth";
import { GITHUB_OAUTH_URL } from '../../lib/constants';

function GithubMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 16 16"
      aria-hidden
      className={className}
      fill="currentColor"
    >
      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.4 7.4 0 0 1 2-.27c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
    </svg>
  );
}

export function GithubSignInButton({
  redirectTo = "/dashboard",
}: {
  redirectTo?: string;
}) {
  const [pending, setPending] = useState(false);

  // async function handleSignIn() {
  //   try {
  //     setPending(true);

  //     // Remember where the user should go after login
  //     sessionStorage.setItem(
  //       POST_LOGIN_REDIRECT_KEY,
  //       redirectTo
  //     );

  //     const getGithubOAuthUrl = await apiFetch<GitOAuthURL>(
  //       "/auth/login"
  //     );

  //     console.log( "env auth url ", GITHUB_OAUTH_URL);
  //     console.log("Fetched Auth Url : ",getGithubOAuthUrl.url)
      
  //     // Navigate to GitHub
  //     window.location.href =  GITHUB_OAUTH_URL;
  //   } catch (error) {
  //     console.error("Failed to get GitHub OAuth URL:", error);
  //     setPending(false);
  //   }
  // }

  async function handleSignIn() {
  setPending(true);

  sessionStorage.setItem(
    POST_LOGIN_REDIRECT_KEY,
    redirectTo
  );

  window.location.href = GITHUB_OAUTH_URL;
}

  return (
    <button
      type="button"
      onClick={handleSignIn}
      disabled={pending}
      className="group inline-flex w-full items-center justify-center gap-3 rounded-lg
                 bg-white px-5 py-3 text-sm font-semibold text-neutral-900
                 shadow-sm ring-1 ring-black/5 transition
                 hover:bg-neutral-100 focus-visible:outline-2
                 focus-visible:outline-offset-2 focus-visible:outline-white
                 disabled:cursor-not-allowed disabled:opacity-60"
    >
      {pending ? (
        <Loader2 className="size-4 animate-spin" />
      ) : (
        <GithubMark className="size-4" />
      )}

      {pending
        ? "Redirecting to GitHub…"
        : "Continue with GitHub"}
    </button>
  );
}