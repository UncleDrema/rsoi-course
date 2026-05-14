<script lang="ts">
  import { beginLogin } from "../lib/auth";

  export let loading = false;
  export let error: string | null = null;

  async function handleLogin(): Promise<void> {
    await beginLogin("/flights");
  }
</script>

<section class="auth-screen">
  <div class="auth-panel">
    <div class="eyebrow">Авторизация</div>
    <h1>Система бронирования рейсов</h1>
    <p>Войдите через внешний провайдер, чтобы управлять билетами, бонусным счетом и административными разделами.</p>

    {#if error}
      <div class="alert error">{error}</div>
    {/if}

    <button class="primary-button auth-button" type="button" disabled={loading} on:click={handleLogin}>
      {loading ? "Переход..." : "Войти через провайдера"}
    </button>
  </div>
</section>
