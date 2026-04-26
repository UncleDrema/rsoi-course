<script lang="ts">
  import type { User } from "oidc-client-ts";
  import { createEventDispatcher } from "svelte";
  import type { RoutePath } from "../router";

  export let currentPath: RoutePath;
  export let user: User | null = null;
  export let authError: string | null = null;

  const dispatch = createEventDispatcher<{
    navigate: RoutePath;
    logout: void;
  }>();

  const links: Array<{ label: string; path: RoutePath }> = [
    { label: "Flights", path: "/flights" },
    { label: "Tickets", path: "/tickets" },
    { label: "Profile", path: "/profile" },
    { label: "Users", path: "/admin/users" },
    { label: "Statistics", path: "/admin/statistics" }
  ];

  function triggerNavigate(path: RoutePath): void {
    dispatch("navigate", path);
  }
</script>

<div class="layout">
  <aside class="sidebar">
    <div class="brand">
      <span class="brand-mark">RS</span>
      <div>
        <strong>RSOI Operations</strong>
        <div class="muted">Flight booking control</div>
      </div>
    </div>

    <nav class="nav">
      {#each links as link}
        <button
          class:active={currentPath === link.path}
          class="nav-link"
          type="button"
          on:click={() => triggerNavigate(link.path)}
        >
          {link.label}
        </button>
      {/each}
    </nav>

    <div class="sidebar-footer">
      <div class="user-block">
        <div class="muted">Signed in</div>
        <strong>{user?.profile.name ?? user?.profile.email ?? "Unknown user"}</strong>
      </div>
      <button class="secondary-button" type="button" on:click={() => dispatch("logout")}>
        Sign out
      </button>
    </div>
  </aside>

  <main class="content">
    {#if authError}
      <div class="alert error">{authError}</div>
    {/if}
    <slot />
  </main>
</div>
