<script lang="ts">
  import type { User } from "oidc-client-ts";
  import { createEventDispatcher } from "svelte";
  import type { RoutePath } from "../router";

  export let currentPath: RoutePath;
  export let user: User | null = null;
  export let authError: string | null = null;
  export let isAdmin = false;

  const dispatch = createEventDispatcher<{
    navigate: RoutePath;
    logout: void;
  }>();

  const links: Array<{ label: string; path: RoutePath; adminOnly?: boolean }> = [
    { label: "Рейсы", path: "/flights" },
    { label: "Билеты", path: "/tickets" },
    { label: "Профиль", path: "/profile" },
    { label: "Пользователи", path: "/admin/users", adminOnly: true },
    { label: "Управление рейсами", path: "/admin/flights", adminOnly: true },
    { label: "Статистика", path: "/admin/statistics", adminOnly: true }
  ];

  $: visibleLinks = links.filter((link) => !link.adminOnly || isAdmin);

  function triggerNavigate(path: RoutePath): void {
    dispatch("navigate", path);
  }
</script>

<div class="layout">
  <aside class="sidebar">
    <div class="brand">
      <span class="brand-mark">RS</span>
      <div>
        <strong>RSOI</strong>
        <div class="muted">Управление авиабронированием</div>
      </div>
    </div>

    <nav class="nav">
      {#each visibleLinks as link}
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
        <div class="muted">Вы вошли как</div>
        <strong>{user?.profile.name ?? user?.profile.email ?? "Неизвестный пользователь"}</strong>
      </div>
      <button class="secondary-button" type="button" on:click={() => dispatch("logout")}>
        Выйти
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
