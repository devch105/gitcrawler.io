export interface AuthUser {
  id: string;
  githubId: number;
  githubUsername: string;
  displayName: string | null;
  avatarUrl: string | null;
}

export type SessionState =
  | { status: "loading" }
  | { status: "authenticated"; user: AuthUser }
  | { status: "unauthenticated" };

export interface GitOAuthURL {
  url : string
}