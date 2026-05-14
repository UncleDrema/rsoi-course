<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { formatDateTime, formatMoney, titleCase } from "../lib/format";
  import type { Ticket } from "../lib/types";

  export let tickets: Ticket[] = [];
  export let loading = false;
  export let cancelingTicket = "";
  export let error: string | null = null;

  const dispatch = createEventDispatcher<{ cancel: string }>();
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Заказы</div>
    <h1>Билеты</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<section class="panel">
  {#if loading}
    <div class="empty-state">Загружаем билеты...</div>
  {:else if tickets.length}
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Рейс</th>
            <th>Маршрут</th>
            <th>Дата</th>
            <th>Цена</th>
            <th>Статус</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {#each tickets as ticket}
            <tr>
              <td>{ticket.flightNumber}</td>
              <td>{ticket.fromAirport} - {ticket.toAirport}</td>
              <td>{formatDateTime(ticket.date)}</td>
              <td>{formatMoney(ticket.price)}</td>
              <td><span class="badge">{titleCase(ticket.status)}</span></td>
              <td class="table-action">
                <button
                  class="secondary-button"
                  type="button"
                  disabled={cancelingTicket === ticket.ticketUid}
                  on:click={() => dispatch("cancel", ticket.ticketUid)}
                >
                  {cancelingTicket === ticket.ticketUid ? "Отменяем..." : "Отменить"}
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {:else}
    <div class="empty-state">У вас пока нет купленных билетов.</div>
  {/if}
</section>
