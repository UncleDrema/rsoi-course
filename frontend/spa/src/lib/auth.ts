import { User, UserManager, WebStorageStateStore, type UserManagerSettings } from "oidc-client-ts";
import { get, writable } from "svelte/store";
import { assertEnv, env } from "./env";
import { navigate } from "./router";

export const authUser = writable<User | null>(null);
export const authReady = writable(false);
export const authError = writable<string | null>(null);

const missingEnv = assertEnv();

const settings: UserManagerSettings = {
  authority: env.oidcAuthority,
  client_id: env.oidcClientId,
  redirect_uri: env.redirectUri,
  post_logout_redirect_uri: env.postLogoutRedirectUri,
  response_type: "code",
  scope: env.oidcScope,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  stateStore: new WebStorageStateStore({ store: window.sessionStorage }),
  automaticSilentRenew: false,
  monitorSession: false,
  loadUserInfo: true
};

export const userManager = new UserManager(settings);

userManager.events.addUserLoaded((user) => {
  authUser.set(user);
  authError.set(null);
});

userManager.events.addUserUnloaded(() => {
  authUser.set(null);
});

userManager.events.addAccessTokenExpired(() => {
  authError.set("Your session expired. Sign in again.");
  authUser.set(null);
  navigate("/login", true);
});

export async function initAuth(): Promise<void> {
  if (missingEnv.length > 0) {
    authError.set(`Missing environment variables: ${missingEnv.join(", ")}`);
    authReady.set(true);
    return;
  }

  try {
    const user = await userManager.getUser();
    authUser.set(user && !user.expired ? user : null);
  } catch (error) {
    authError.set(normalizeError(error));
    authUser.set(null);
  } finally {
    authReady.set(true);
  }
}

export async function beginLogin(returnPath = "/flights"): Promise<void> {
  window.sessionStorage.setItem("post_login_path", returnPath);
  await userManager.signinRedirect({
    state: { returnPath }
  });
}

export async function completeLogin(): Promise<string> {
  const user = await userManager.signinCallback();
  authUser.set(user ?? null);
  authError.set(null);

  const state = (user?.state ?? {}) as { returnPath?: string };
  const stateReturnPath = state.returnPath;
  const storedReturnPath = window.sessionStorage.getItem("post_login_path");
  window.sessionStorage.removeItem("post_login_path");

  return stateReturnPath ?? storedReturnPath ?? "/flights";
}

export async function logout(): Promise<void> {
  const user = get(authUser);
  authUser.set(null);

  if (!user) {
    navigate("/login", true);
    return;
  }

  await userManager.signoutRedirect({
    id_token_hint: user.id_token
  });
}

export async function getAccessToken(): Promise<string | null> {
  const user = await userManager.getUser();
  if (!user || user.expired) {
    authUser.set(null);
    return null;
  }

  authUser.set(user);
  return user.access_token;
}

export function isAuthenticated(): boolean {
  const user = get(authUser);
  return Boolean(user && !user.expired);
}

export function getUserRoles(): string[] {
  const user = get(authUser);
  return getRolesFromUser(user);
}

export function getRolesFromUser(user: User | null): string[] {
  const profile = (user?.profile ?? {}) as Record<string, unknown>;
  const roleClaims = [
    profile["role"],
    profile["roles"],
    profile["https://schemas.quickstarts.com/roles"],
    profile["https://schemas.openid.net/roles"]
  ];

  const roles = roleClaims.flatMap((claim) => {
    if (Array.isArray(claim)) {
      return claim.map(String);
    }
    if (typeof claim === "string") {
      return [claim];
    }
    return [];
  });

  return Array.from(new Set(roles));
}

export function hasUserRole(user: User | null, role: string): boolean {
  const expectedRole = normalizeRole(role);
  return getRolesFromUser(user).some((currentRole) => normalizeRole(currentRole) === expectedRole);
}

function normalizeRole(role: string): string {
  return role.replace(/^ROLE_/i, "").toUpperCase();
}

function normalizeError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "Authentication failed.";
}
