<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { getRoleLabel } from "../lib/format";
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
    <div class="eyebrow">Администрирование</div>
    <h1>Пользователи</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="admin-grid">
  <section class="panel">
    <div class="panel-header">
      <h2>Создать пользователя</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submit}>
      <label>
        <span>Логин</span>
        <input bind:value={form.username} type="text" required />
      </label>
      <label>
        <span>Email</span>
        <input bind:value={form.email} type="email" required />
      </label>
      <label>
        <span>Имя</span>
        <input bind:value={form.name} type="text" required />
      </label>
      <label>
        <span>Пароль</span>
        <input bind:value={form.password} type="password" minlength="8" required />
      </label>
      <button class="primary-button" type="submit" disabled={saving}>
        {saving ? "Создаем..." : "Создать"}
      </button>
    </form>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Список пользователей</h2>
    </div>
    {#if loading}
      <div class="empty-state">Загружаем пользователей...</div>
    {:else if users.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Имя</th>
              <th>Email</th>
              <th>Логин</th>
              <th>Роли</th>
            </tr>
          </thead>
          <tbody>
            {#each users as user}
              <tr>
                <td>{user.name}</td>
                <td>{user.email}</td>
                <td>{user.username}</td>
                <td>{user.roles.map(getRoleLabel).join(", ")}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {:else}
      <div class="empty-state">Административный API не вернул пользователей.</div>
    {/if}
  </section>
</div>
