export const PUBLIC_API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL;
export const INTERNAL_API_BASE = process.env.INTERNAL_API_BASE_URL || PUBLIC_API_BASE;

export const GITHUB_OAUTH_URL = `${PUBLIC_API_BASE}/oauth2/authorization/github`;
export const SESSION_COOKIE = "GITCRAWLER_SESSION";

export const  POST_LOGIN_REDIRECT_KEY = "gc:postLoginRedirect";