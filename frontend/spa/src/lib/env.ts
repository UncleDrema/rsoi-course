const origin = window.location.origin;

function joinUrl(base: string, path: string): string {
  if (/^https?:\/\//.test(path)) {
    return path;
  }

  const normalizedBase = base.endsWith("/") ? base.slice(0, -1) : base;
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

const redirectPath = import.meta.env.VITE_OIDC_REDIRECT_PATH ?? "/auth/callback";
const postLogoutPath = import.meta.env.VITE_OIDC_POST_LOGOUT_REDIRECT_PATH ?? "/login";

export const env = {
  oidcAuthority: import.meta.env.VITE_OIDC_AUTHORITY ?? "",
  oidcClientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? "",
  oidcScope: import.meta.env.VITE_OIDC_SCOPE ?? "openid profile email",
  apiBasePath: import.meta.env.VITE_API_BASE_PATH ?? "/api/v1",
  adminBaseUrl: import.meta.env.VITE_IDP_ADMIN_BASE_URL ?? "",
  adminUsersPath: import.meta.env.VITE_IDP_ADMIN_USERS_PATH ?? "/api/v1/users",
  adminReportPath: import.meta.env.VITE_IDP_ADMIN_REPORT_PATH ?? "/api/v1/statistics/report",
  adminEventsPath: import.meta.env.VITE_IDP_ADMIN_EVENTS_PATH ?? "/api/v1/statistics/events",
  redirectUri: joinUrl(origin, redirectPath),
  postLogoutRedirectUri: joinUrl(origin, postLogoutPath)
};

export function resolveAdminUrl(path: string): string {
  if (!env.adminBaseUrl) {
    return path;
  }

  return joinUrl(env.adminBaseUrl, path);
}

export function assertEnv(): string[] {
  const missing: string[] = [];
  if (!env.oidcAuthority) {
    missing.push("VITE_OIDC_AUTHORITY");
  }
  if (!env.oidcClientId) {
    missing.push("VITE_OIDC_CLIENT_ID");
  }
  return missing;
}
