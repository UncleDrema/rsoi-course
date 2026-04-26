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
    <div class="eyebrow">Identity</div>
    <h1>Flight booking operations</h1>
    <p>
      Use the external identity provider to access bookings, loyalty state, and administrative
      tools.
    </p>

    {#if error}
      <div class="alert error">{error}</div>
    {/if}

    <button class="primary-button auth-button" type="button" disabled={loading} on:click={handleLogin}>
      {loading ? "Redirecting..." : "Sign in with OIDC"}
    </button>
  </div>
</section>
