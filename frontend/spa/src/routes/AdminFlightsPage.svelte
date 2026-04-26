<script lang="ts">
  import { createEventDispatcher } from "svelte";
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
    <div class="eyebrow">Administration</div>
    <h1>Flights</h1>
  </div>
  <button class="secondary-button" type="button" disabled={loading} on:click={() => dispatch("refresh")}>
    Refresh
  </button>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="admin-grid wide">
  <section class="panel">
    <div class="panel-header">
      <h2>Create airport</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submitAirport}>
      <label>
        <span>Name</span>
        <input bind:value={airportForm.name} type="text" required />
      </label>
      <label>
        <span>City</span>
        <input bind:value={airportForm.city} type="text" required />
      </label>
      <label>
        <span>Country</span>
        <input bind:value={airportForm.country} type="text" required />
      </label>
      <button class="primary-button" type="submit" disabled={savingAirport}>
        {savingAirport ? "Creating..." : "Create airport"}
      </button>
    </form>
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Create flight</h2>
    </div>
    <form class="form-grid" on:submit|preventDefault={submitFlight}>
      <label>
        <span>Flight number</span>
        <input bind:value={flightForm.flightNumber} type="text" required />
      </label>
      <label>
        <span>Date</span>
        <input bind:value={flightForm.date} type="date" required />
      </label>
      <label>
        <span>Time</span>
        <input bind:value={flightForm.time} type="time" required />
      </label>
      <label>
        <span>From airport</span>
        <select bind:value={flightForm.fromAirportId} required>
          <option value="" disabled>Select airport</option>
          {#each airports as airport}
            <option value={String(airport.id)}>{airport.id} - {airport.city}, {airport.name}</option>
          {/each}
        </select>
      </label>
      <label>
        <span>To airport</span>
        <select bind:value={flightForm.toAirportId} required>
          <option value="" disabled>Select airport</option>
          {#each airports as airport}
            <option value={String(airport.id)}>{airport.id} - {airport.city}, {airport.name}</option>
          {/each}
        </select>
      </label>
      <label>
        <span>Price</span>
        <input bind:value={flightForm.price} type="number" min="1" step="1" required />
      </label>
      <button class="primary-button" type="submit" disabled={savingFlight || airports.length < 2}>
        {savingFlight ? "Creating..." : "Create flight"}
      </button>
    </form>
  </section>
</div>

<div class="admin-grid wide">
  <section class="panel">
    <div class="panel-header">
      <h2>Airports</h2>
    </div>
    {#if loading && !airports.length}
      <div class="empty-state">Loading airports...</div>
    {:else if airports.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>City</th>
              <th>Country</th>
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
      <div class="empty-state">No airports returned by the API.</div>
    {/if}
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>Recent flights</h2>
    </div>
    {#if loading && !flights.length}
      <div class="empty-state">Loading flights...</div>
    {:else if flights.length}
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Flight</th>
              <th>From</th>
              <th>To</th>
              <th>Date</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            {#each flights as flight}
              <tr>
                <td>{flight.flightNumber}</td>
                <td>{flight.fromAirport}</td>
                <td>{flight.toAirport}</td>
                <td>{flight.date}</td>
                <td>{flight.price}</td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {:else}
      <div class="empty-state">No flights returned by the API.</div>
    {/if}
  </section>
</div>
