<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { formatDateTime, formatMoney, formatNumber } from "../lib/format";
  import type { Flight, PageDto } from "../lib/types";

  export let pageData: PageDto<Flight> | null = null;
  export let loading = false;
  export let buyingFlight = "";
  export let error: string | null = null;
  export let balance = 0;

  const dispatch = createEventDispatcher<{
    pageChange: number;
    buy: { flightNumber: string; price: number; paidFromBalance: boolean };
  }>();

  let selectedFlight: Flight | null = null;
  let payFromBalance = false;

  $: currentPage = pageData?.page ?? 1;
  $: totalItems = pageData?.totalElements ?? 0;
  $: totalPages = pageData?.totalPages ?? 1;
  $: availableBalance = Math.max(balance, 0);
  $: bonusPayment = selectedFlight && payFromBalance ? Math.min(availableBalance, selectedFlight.price) : 0;
  $: finalPrice = selectedFlight ? Math.max(selectedFlight.price - bonusPayment, 0) : 0;

  function openPurchase(flight: Flight): void {
    selectedFlight = flight;
    payFromBalance = availableBalance > 0;
  }

  function closePurchase(): void {
    selectedFlight = null;
    payFromBalance = false;
  }

  function confirmPurchase(): void {
    if (!selectedFlight) {
      return;
    }

    dispatch("buy", {
      flightNumber: selectedFlight.flightNumber,
      price: selectedFlight.price,
      paidFromBalance: payFromBalance && availableBalance > 0
    });
  }

  function closePurchaseFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      closePurchase();
    }
  }
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Каталог</div>
    <h1>Рейсы</h1>
  </div>
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
              on:click={() => openPurchase(flight)}
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

{#if selectedFlight}
  <div class="modal-backdrop" role="presentation" on:click={closePurchaseFromBackdrop}>
    <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="purchase-title">
      <div class="panel-header">
        <div>
          <div class="eyebrow">Покупка билета</div>
          <h2 id="purchase-title">Рейс {selectedFlight.flightNumber}</h2>
        </div>
        <button class="icon-button" type="button" aria-label="Закрыть" on:click={closePurchase}>x</button>
      </div>

      <div class="purchase-summary">
        <div>
          <span class="metric-label">Маршрут</span>
          <strong>{selectedFlight.fromAirport} - {selectedFlight.toAirport}</strong>
        </div>
        <div>
          <span class="metric-label">Дата</span>
          <strong>{formatDateTime(selectedFlight.date)}</strong>
        </div>
        <div>
          <span class="metric-label">Баланс бонусов</span>
          <strong>{formatNumber(availableBalance)}</strong>
        </div>
      </div>

      {#if availableBalance > 0}
        <label class="switch-row">
          <span>
            <strong>Оплатить бонусами</strong>
            <span class="muted">Будет списано {formatNumber(bonusPayment)} бонусов</span>
          </span>
          <input type="checkbox" bind:checked={payFromBalance} />
          <span class="switch-track"></span>
        </label>
      {:else}
        <div class="empty-state compact">На балансе нет бонусов для списания.</div>
      {/if}

      <div class="price-preview">
        {#if bonusPayment > 0}
          <span class="old-price">{formatMoney(selectedFlight.price)}</span>
        {/if}
        <strong>{formatMoney(finalPrice)}</strong>
      </div>

      <div class="modal-actions">
        <button class="secondary-button" type="button" on:click={closePurchase}>Отмена</button>
        <button class="primary-button" type="button" disabled={buyingFlight === selectedFlight.flightNumber} on:click={confirmPurchase}>
          {buyingFlight === selectedFlight.flightNumber ? "Покупаем..." : "Подтвердить покупку"}
        </button>
      </div>
    </div>
  </div>
{/if}
