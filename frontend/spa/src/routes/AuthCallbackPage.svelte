<script lang="ts">
  export let error: string | null = null;
</script>

<section class="center-state">
  <div class="state-card">
    <h1>Завершаем вход</h1>
    {#if error}
      <div class="alert error">{error}</div>
    {:else}
      <p>Обрабатываем ответ сервиса авторизации.</p>
    {/if}
  </div>
</section>
