import { writable } from "svelte/store";

export type RoutePath =
  | "/"
  | "/login"
  | "/auth/callback"
  | "/flights"
  | "/tickets"
  | "/profile"
  | "/admin/users"
  | "/admin/flights"
  | "/admin/statistics";

export const route = writable<RoutePath>(getRoute(window.location.pathname));

function getRoute(pathname: string): RoutePath {
  switch (pathname) {
    case "/":
    case "/login":
    case "/auth/callback":
    case "/flights":
    case "/tickets":
    case "/profile":
    case "/admin/users":
    case "/admin/flights":
    case "/admin/statistics":
      return pathname;
    default:
      return "/";
  }
}

window.addEventListener("popstate", () => {
  route.set(getRoute(window.location.pathname));
});

export function navigate(path: RoutePath, replace = false): void {
  const method = replace ? "replaceState" : "pushState";
  window.history[method]({}, "", path);
  route.set(path);
}
