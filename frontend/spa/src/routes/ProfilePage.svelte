<script lang="ts">
  import { formatDateTime, formatNumber, titleCase } from "../lib/format";
  import type { PrivilegeInfo, UserInfo } from "../lib/types";

  export let profile: UserInfo | null = null;
  export let privilege: PrivilegeInfo | null = null;
  export let loading = false;
  export let error: string | null = null;
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Account</div>
    <h1>Profile and privileges</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="stats-grid">
  <section class="panel stat-panel">
    <span class="metric-label">Tickets</span>
    <strong class="metric-value">{formatNumber(profile?.tickets.length ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Balance</span>
    <strong class="metric-value">{formatNumber(privilege?.balance ?? profile?.privilege?.balance ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Tier</span>
    <strong class="metric-value">{titleCase(privilege?.status ?? profile?.privilege?.status ?? null)}</strong>
  </section>
</div>

<section class="panel">
  <div class="panel-header">
    <h2>Loyalty activity</h2>
  </div>
  {#if loading}
    <div class="empty-state">Loading profile...</div>
  {:else if privilege?.history?.length}
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Ticket</th>
            <th>Change</th>
            <th>Operation</th>
          </tr>
        </thead>
        <tbody>
          {#each privilege.history as entry}
            <tr>
              <td>{formatDateTime(entry.date)}</td>
              <td>{entry.ticketUid}</td>
              <td>{formatNumber(entry.balanceDiff)}</td>
              <td>{titleCase(entry.operationType)}</td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {:else}
    <div class="empty-state">No privilege history available.</div>
  {/if}
</section>
