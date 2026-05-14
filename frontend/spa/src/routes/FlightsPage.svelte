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
  $: totalPages = pageData?.totalPages ?? 1;
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Каталог</div>
    <h1>Рейсы</h1>
  </div>
  <label class="toggle">
    <input
      type="checkbox"
      checked={payFromBalance}
      on:change={(event) => dispatch("payModeChange", (event.currentTarget as HTMLInputElement).checked)}
    />
    <span>Списывать бонусы, если они доступны</span>
  </label>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<section class="panel">
  <div class="panel-toolbar">
    <div>
      <strong>{formatNumber(totalItems)}</strong>
      <span class="muted"> доступно рейсов</span>
    </div>
    <div class="pager">
      <button class="secondary-button" type="button" disabled={currentPage <= 1 || loading} on:click={() => dispatch("pageChange", currentPage - 1)}>
        Назад
      </button>
      <span>Страница {currentPage} из {totalPages}</span>
      <button class="secondary-button" type="button" disabled={currentPage >= totalPages || loading} on:click={() => dispatch("pageChange", currentPage + 1)}>
        Вперед
      </button>
    </div>
  </div>

  <div class="cards-grid">
    {#if loading && !pageData}
      <div class="empty-state">Загружаем рейсы...</div>
    {:else if pageData?.items.length}
      {#each pageData.items as flight}
        <article class="flight-card">
          <div class="flight-meta">
            <strong>{flight.flightNumber}</strong>
            <span class="muted">{formatDateTime(flight.date)}</span>
          </div>
          <div class="route-line">
            <div>{flight.fromAirport}</div>
            <div class="route-arrow">куда</div>
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
              {buyingFlight === flight.flightNumber ? "Покупаем..." : "Купить"}
            </button>
          </div>
        </article>
      {/each}
    {:else}
      <div class="empty-state">Шлюз не вернул ни одного рейса.</div>
    {/if}
  </div>
</section>
