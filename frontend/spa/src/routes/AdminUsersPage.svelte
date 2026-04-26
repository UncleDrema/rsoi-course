<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import type { AdminCreateUserInput, AdminUser } from "../lib/types";

  export let users: AdminUser[] = [];
  export let loading = false;
  export let saving = false;
  export let error: string | null = null;

  const dispatch = createEventDispatcher<{ create: AdminCreateUserInput }>();

  let form: AdminCreateUserInput = {
    username: "",
    email: "",
    password: "",
    name: ""
  };

  function submit(): void {
    dispatch("create", form);
    form = {
      username: "",
      email: "",
      password: "",
      name: ""
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
        <span>Username</span>
        <input bind:value={form.username} type="text" required />
      </label>
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
              <th>Username</th>
              <th>Roles</th>
            </tr>
          </thead>
          <tbody>
            {#each users as user}
              <tr>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.username}</td>
                <td>{user.roles.join(", ")}</td>
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
