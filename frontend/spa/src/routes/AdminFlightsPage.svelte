<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import { formatDateTime, formatMoney } from "../lib/format";
  import type { Airport, CreateAirportInput, CreateFlightInput, Flight } from "../lib/types";

  export let airports: Airport[] = [];
  export let flights: Flight[] = [];
  export let loading = false;
  export let savingAirport = false;
  export let savingFlight = false;
  export let error: string | null = null;

  const dispatch = createEventDispatcher<{
    refresh: void;
    createAirport: CreateAirportInput;
    createFlight: CreateFlightInput;
  }>();

  let airportForm: CreateAirportInput = {
    name: "",
    city: "",
    country: ""
  };

  let flightForm = {
    flightNumber: "",
    date: "",
    time: "",
    fromAirportId: "",
    toAirportId: "",
    price: ""
  };

  function submitAirport(): void {
    dispatch("createAirport", airportForm);
    airportForm = {
      name: "",
      city: "",
      country: ""
    };
  }

  function submitFlight(): void {
    const payload: CreateFlightInput = {
      flightNumber: flightForm.flightNumber,
      datetime: `${flightForm.date} ${flightForm.time}`,
      fromAirportId: Number(flightForm.fromAirportId),
      toAirportId: Number(flightForm.toAirportId),
      price: Number(flightForm.price)
    };

    dispatch("createFlight", payload);
    flightForm = {
      flightNumber: "",
      date: "",
      time: "",
      fromAirportId: "",
      toAirportId: "",
      price: ""
    };
  }
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Администрирование</div>
    <h1>Рейсы</h1>
  </div>
  <button class="secondary-button" type="button" disabled={loading} on:click={() => dispatch("refresh")}>
    Обновить
  </button>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="admin-grid wide">
  <section class="panel">
    <div class="panel-header">
      <h2>Создать аэропорт</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submitAirport}>
      <label>
        <span>Название</span>
        <input bind:value={airportForm.name} type="text" required />
      </label>
      <label>
        <span>Город</span>
        <input bind:value={airportForm.city} type="text" required />
      </label>
      <label>
        <span>Страна</span>
        <input bind:value={airportForm.country} type="text" required />
      </label>
      <button class="primary-button" type="submit" disabled={savingAirport}>
        {savingAirport ? "Создаем..." : "Создать аэропорт"}
      </button>
    </form>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Создать рейс</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submitFlight}>
      <label>
        <span>Номер рейса</span>
        <input bind:value={flightForm.flightNumber} type="text" required />
      </label>
      <label>
        <span>Дата</span>
        <input bind:value={flightForm.date} type="date" required />
      </label>
      <label>
        <span>Время</span>
        <input bind:value={flightForm.time} type="time" required />
      </label>
      <label>
        <span>Аэропорт вылета</span>
        <select bind:value={flightForm.fromAirportId} required>
          <option value="" disabled>Выберите аэропорт</option>
          {#each airports as airport}
            <option value={String(airport.id)}>{airport.id} - {airport.city}, {airport.name}</option>
          {/each}
        </select>
      </label>
      <label>
        <span>Аэропорт прилета</span>
        <select bind:value={flightForm.toAirportId} required>
          <option value="" disabled>Выберите аэропорт</option>
          {#each airports as airport}
            <option value={String(airport.id)}>{airport.id} - {airport.city}, {airport.name}</option>
          {/each}
        </select>
      </label>
      <label>
        <span>Цена</span>
        <input bind:value={flightForm.price} type="number" min="1" step="1" required />
      </label>
      <button class="primary-button" type="submit" disabled={savingFlight || airports.length < 2}>
        {savingFlight ? "Создаем..." : "Создать рейс"}
      </button>
    </form>
  </section>
</div>

<div class="admin-grid wide">
  <section class="panel">
    <div class="panel-header">
      <h2>Аэропорты</h2>
    </div>
    {#if loading && !airports.length}
      <div class="empty-state">Загружаем аэропорты...</div>
    {:else if airports.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Название</th>
              <th>Город</th>
              <th>Страна</th>
            </tr>
          </thead>
          <tbody>
            {#each airports as airport}
              <tr>
                <td>{airport.id}</td>
                <td>{airport.name}</td>
                <td>{airport.city}</td>
                <td>{airport.country}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {:else}
      <div class="empty-state">API не вернул список аэропортов.</div>
    {/if}
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Последние рейсы</h2>
    </div>
    {#if loading && !flights.length}
      <div class="empty-state">Загружаем рейсы...</div>
    {:else if flights.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Рейс</th>
              <th>Откуда</th>
              <th>Куда</th>
              <th>Дата</th>
              <th>Цена</th>
            </tr>
          </thead>
          <tbody>
            {#each flights as flight}
              <tr>
                <td>{flight.flightNumber}</td>
                <td>{flight.fromAirport}</td>
                <td>{flight.toAirport}</td>
                <td>{formatDateTime(flight.date)}</td>
                <td>{formatMoney(flight.price)}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {:else}
      <div class="empty-state">API не вернул список рейсов.</div>
    {/if}
  </section>
</div>
