<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type { AdminCreateUserInput, AdminUser } from "../lib/types";

  export let users: AdminUser[] = [];
  export let loading = false;
  export let saving = false;
  export let error: string | null = null;

  const dispatch = createEventDispatcher<{ create: AdminCreateUserInput }>();

  let form: AdminCreateUserInput = {
    email: "",
    password: "",
    name: "",
    role: "user"
  };

  function submit(): void {
    dispatch("create", form);
    form = {
      email: "",
      password: "",
      name: "",
      role: "user"
    };
  }
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Administration</div>
    <h1>Users</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="admin-grid">
  <section class="panel">
    <div class="panel-header">
      <h2>Create user</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submit}>
      <label>
        <span>Email</span>
        <input bind:value={form.email} type="email" required />
      </label>
      <label>
        <span>Name</span>
        <input bind:value={form.name} type="text" required />
      </label>
      <label>
        <span>Password</span>
        <input bind:value={form.password} type="password" minlength="8" required />
      </label>
      <label>
        <span>Role</span>
        <select bind:value={form.role}>
          <option value="user">user</option>
          <option value="admin">admin</option>
        </select>
      </label>
      <button class="primary-button" type="submit" disabled={saving}>
        {saving ? "Creating..." : "Create user"}
      </button>
    </form>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Directory</h2>
    </div>
    {#if loading}
      <div class="empty-state">Loading users...</div>
    {:else if users.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>User ID</th>
              <th>Role</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {#each users as user}
              <tr>
                <td>{user.name ?? user.nickname ?? "-"}</td>
                <td>{user.email ?? "-"}</td>
                <td>{user.user_id ?? "-"}</td>
                <td>{String(user.app_metadata?.role ?? "-")}</td>
                <td>{user.blocked ? "Blocked" : "Active"}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {:else}
      <div class="empty-state">No users returned by the admin API.</div>
    {/if}
  </section>
</div>
