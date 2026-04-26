<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { formatDateTime, formatMoney, formatNumber } from "../lib/format";
  import type { Flight, PageDto } from "../lib/types";

  export let pageData: PageDto<Flight> | null = null;
  export let loading = false;
  export let buyingFlight = "";
  export let error: string | null = null;
  export let payFromBalance = true;

  const dispatch = createEventDispatcher<{
    pageChange: number;
    buy: { flightNumber: string; price: number; paidFromBalance: boolean };
    payModeChange: boolean;
  }>();

  $: currentPage = pageData?.page ?? 1;
  $: totalItems = pageData?.totalElements ?? 0;
  $: pageSize = pageData?.pageSize ?? 12;
  $: totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Catalog</div>
    <h1>Flights</h1>
  </div>
  <label class="toggle">
    <input
      type="checkbox"
      checked={payFromBalance}
      on:change={(event) => dispatch("payModeChange", (event.currentTarget as HTMLInputElement).checked)}
    />
    <span>Pay from balance when available</span>
  </label>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<section class="panel">
  <div class="panel-toolbar">
    <div>
      <strong>{formatNumber(totalItems)}</strong>
      <span class="muted"> available flights</span>
    </div>
    <div class="pager">
      <button class="secondary-button" type="button" disabled={currentPage <= 1 || loading} on:click={() => dispatch("pageChange", currentPage - 1)}>
        Previous
      </button>
      <span>Page {currentPage} / {totalPages}</span>
      <button class="secondary-button" type="button" disabled={currentPage >= totalPages || loading} on:click={() => dispatch("pageChange", currentPage + 1)}>
        Next
      </button>
    </div>
  </div>

  <div class="cards-grid">
    {#if loading && !pageData}
      <div class="empty-state">Loading flights...</div>
    {:else if pageData?.items.length}
      {#each pageData.items as flight}
        <article class="flight-card">
          <div class="flight-meta">
            <strong>{flight.flightNumber}</strong>
            <span class="muted">{formatDateTime(flight.date)}</span>
          </div>
          <div class="route-line">
            <div>{flight.fromAirport}</div>
            <div class="route-arrow">to</div>
            <div>{flight.toAirport}</div>
          </div>
          <div class="flight-footer">
            <strong>{formatMoney(flight.price)}</strong>
            <button
              class="primary-button"
              type="button"
              disabled={buyingFlight === flight.flightNumber}
              on:click={() =>
                dispatch("buy", {
                  flightNumber: flight.flightNumber,
                  price: flight.price,
                  paidFromBalance: payFromBalance
                })}
            >
              {buyingFlight === flight.flightNumber ? "Buying..." : "Buy"}
            </button>
          </div>
        </article>
      {/each}
    {:else}
      <div class="empty-state">No flights returned by the gateway.</div>
    {/if}
  </div>
</section>
