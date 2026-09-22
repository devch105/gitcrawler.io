import { redirect } from "next/navigation";
import { GitBranch, Lock, ShieldCheck } from "lucide-react";
import { GithubSignInButton } from "@/components/auth/github-sign-in-button";
import { getSession } from "@/lib/session";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ next?: string }>;
}) {
  const [session, params] = await Promise.all([getSession(), searchParams]);
  if (session) redirect("/dashboard");

  return (
    <main className="flex min-h-dvh items-center justify-center bg-neutral-950 px-4">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex size-12 items-center justify-center
                          rounded-xl bg-neutral-900 ring-1 ring-white/10">
            <GitBranch className="size-6 text-neutral-200" />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-white">
            Connect your repositories
          </h1>
          <p className="mt-2 text-sm text-neutral-400">
            Index your codebase so you can ask questions about it in plain English.
          </p>
        </div>

        <div className="rounded-2xl bg-neutral-900/70 p-6 ring-1 ring-white/10 backdrop-blur">
          <GithubSignInButton redirectTo={params.next ?? "/dashboard"} />

          <ul className="mt-6 space-y-3 border-t border-white/5 pt-6">
            <PermissionRow
              icon={<ShieldCheck className="size-4 text-emerald-400" />}
              title="Read-only access"
              body="We clone and read your repositories. We never push, open PRs, or modify anything."
            />
            <PermissionRow
              icon={<Lock className="size-4 text-emerald-400" />}
              title="Private repos are optional"
              body="Grant the repo scope only if you want private repositories indexed."
            />
          </ul>
        </div>

        <p className="mt-6 text-center text-xs text-neutral-500">
          You can revoke access at any time from your GitHub settings.
        </p>
      </div>
    </main>
  );
}

function PermissionRow({
  icon, title, body,
}: { icon: React.ReactNode; title: string; body: string }) {
  return (
    <li className="flex gap-3">
      <span className="mt-0.5 shrink-0">{icon}</span>
      <div>
        <p className="text-sm font-medium text-neutral-200">{title}</p>
        <p className="text-xs leading-relaxed text-neutral-400">{body}</p>
      </div>
    </li>
  );
}