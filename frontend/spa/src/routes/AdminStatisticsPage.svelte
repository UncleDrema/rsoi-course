<script lang="ts">
  import type { StatisticEvent, StatisticsReport } from "../lib/types";

  export let report: StatisticsReport | null = null;
  export let events: StatisticEvent[] = [];
  export let loading = false;
  export let error: string | null = null;

  $: reportEntries = Object.entries(report ?? {});
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Administration</div>
    <h1>Statistics</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="admin-grid">
  <section class="panel">
    <div class="panel-header">
      <h2>Report</h2>
    </div>
    {#if loading && !report}
      <div class="empty-state">Loading report...</div>
    {:else if reportEntries.length}
      <dl class="key-value-list">
        {#each reportEntries as [key, value]}
          <div>
            <dt>{key}</dt>
            <dd>{typeof value === "object" ? JSON.stringify(value) : String(value)}</dd>
          </div>
        {/each}
      </dl>
    {:else}
      <div class="empty-state">No report payload returned by the admin API.</div>
    {/if}
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Events</h2>
    </div>
    {#if loading && !events.length}
      <div class="empty-state">Loading events...</div>
    {:else if events.length}
      <div class="events-list">
        {#each events as event, index}
          <article class="event-card">
            <div class="event-index">#{index + 1}</div>
            <pre>{JSON.stringify(event, null, 2)}</pre>
          </article>
        {/each}
      </div>
    {:else}
      <div class="empty-state">No events returned by the admin API.</div>
    {/if}
  </section>
</div>
