<script lang="ts">
  export let error: string | null = null;
</script>

<section class="center-state">
  <div class="state-card">
    <h1>Completing sign-in</h1>
    {#if error}
      <div class="alert error">{error}</div>
    {:else}
      <p>Processing the authorization response.</p>
    {/if}
  </div>
</section>
