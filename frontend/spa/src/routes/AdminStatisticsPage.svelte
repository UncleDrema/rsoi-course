<script lang="ts">
  import { createEventDispatcher } from "svelte";
  import {
    formatDateTime,
    formatNumber,
    getServiceLabel,
    getStatisticEventTypeLabel,
    getStatisticMetadataDetails,
    titleCase
  } from "../lib/format";
  import type { PageDto, StatisticEvent, StatisticsEventsQuery, StatisticsReport } from "../lib/types";

  export let report: StatisticsReport | null = null;
  export let eventsPage: PageDto<StatisticEvent> | null = null;
  export let filters: StatisticsEventsQuery;
  export let loading = false;
  export let error: string | null = null;

  const dispatch = createEventDispatcher<{
    applyFilters: Partial<StatisticsEventsQuery>;
    resetFilters: void;
    pageChange: number;
  }>();

  const eventTypes = [
    "TICKET_PURCHASED",
    "TICKET_CANCELED",
    "FLIGHT_CREATED",
    "AIRPORT_CREATED",
    "PRIVILEGE_DEPOSITED",
    "PRIVILEGE_WITHDRAWN",
    "PRIVILEGE_COMPENSATED"
  ];

  const services = ["tickets", "flights", "privileges", "statistics", "gateway"];

  let localFilters: StatisticsEventsQuery = { ...filters };

  $: localFilters = { ...filters };
  $: totalItems = eventsPage?.totalElements ?? 0;
  $: currentPage = eventsPage?.page ?? filters.page ?? 1;
  $: pageSize = eventsPage?.pageSize ?? filters.pageSize ?? 10;
  $: totalPages = eventsPage?.totalPages ?? Math.max(1, Math.ceil(totalItems / pageSize));
  $: countsByType = Object.entries(report?.countsByEventType ?? {});
  $: countsByService = Object.entries(report?.countsByService ?? {});

  function submitFilters(): void {
    dispatch("applyFilters", {
      ...localFilters,
      pageSize: Number(localFilters.pageSize ?? 10)
    });
  }

  function resetFilters(): void {
    dispatch("resetFilters");
  }

  function formatActor(event: StatisticEvent): string {
    return event.actorUsername || event.actorSub || "Системное событие";
  }

  function formatDateRange(from: string | null | undefined, to: string | null | undefined): string {
    if (from && to) {
      return `${formatDateTime(from)} - ${formatDateTime(to)}`;
    }
    if (from) {
      return `с ${formatDateTime(from)}`;
    }
    if (to) {
      return `до ${formatDateTime(to)}`;
    }
    return "За весь период";
  }
</script>

<section class="page-header">
  <div>
    <div class="eyebrow">Администрирование</div>
    <h1>Статистика</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<section class="panel">
  <div class="panel-header">
    <div>
      <h2>Фильтры событий</h2>
      <div class="muted">Отчет строится по периоду, журнал можно уточнить дополнительными параметрами.</div>
    </div>
  </div>

  <form class="statistics-filters" on:submit|preventDefault={submitFilters}>
    <label>
      <span>С</span>
      <input bind:value={localFilters.from} type="datetime-local" />
    </label>
    <label>
      <span>По</span>
      <input bind:value={localFilters.to} type="datetime-local" />
    </label>
    <label>
      <span>Тип события</span>
      <select bind:value={localFilters.eventType}>
        <option value="">Все события</option>
        {#each eventTypes as eventType}
          <option value={eventType}>{getStatisticEventTypeLabel(eventType)}</option>
        {/each}
      </select>
    </label>
    <label>
      <span>Сервис</span>
      <select bind:value={localFilters.service}>
        <option value="">Все сервисы</option>
        {#each services as service}
          <option value={service}>{getServiceLabel(service)}</option>
        {/each}
      </select>
    </label>
    <label>
      <span>Пользователь</span>
      <input bind:value={localFilters.actorUsername} type="text" placeholder="Логин пользователя" />
    </label>
    <label>
      <span>OIDC subject</span>
      <input bind:value={localFilters.actorSub} type="text" placeholder="sub из JWT" />
    </label>
    <label>
      <span>Тип сущности</span>
      <input bind:value={localFilters.entityType} type="text" placeholder="ticket, flight, airport" />
    </label>
    <label>
      <span>ID сущности</span>
      <input bind:value={localFilters.entityId} type="text" placeholder="UID билета, рейса или аэропорта" />
    </label>
    <label>
      <span>Поиск</span>
      <input bind:value={localFilters.query} type="search" placeholder="Пользователь, событие, сервис или ID" />
    </label>
    <label>
      <span>Размер страницы</span>
      <select bind:value={localFilters.pageSize}>
        <option value={10}>10</option>
        <option value={20}>20</option>
        <option value={50}>50</option>
      </select>
    </label>
    <div class="filter-actions">
      <button class="primary-button" type="submit" disabled={loading}>
        {loading ? "Применяем..." : "Применить"}
      </button>
      <button class="secondary-button" type="button" disabled={loading} on:click={resetFilters}>
        Сбросить
      </button>
    </div>
  </form>
</section>

<div class="stats-grid">
  <section class="panel stat-panel">
    <span class="metric-label">События</span>
    <strong class="metric-value">{formatNumber(report?.totalEvents ?? totalItems)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Покупки</span>
    <strong class="metric-value">{formatNumber(report?.ticketsPurchased ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Отмены</span>
    <strong class="metric-value">{formatNumber(report?.ticketsCanceled ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Период</span>
    <strong>{formatDateRange(report?.from ?? filters.from, report?.to ?? filters.to)}</strong>
  </section>
</div>

<div class="stats-grid">
  <section class="panel stat-panel">
    <span class="metric-label">Создано рейсов</span>
    <strong class="metric-value">{formatNumber(report?.flightsCreated ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Создано аэропортов</span>
    <strong class="metric-value">{formatNumber(report?.airportsCreated ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Начислено бонусов</span>
    <strong class="metric-value">{formatNumber(report?.privilegeDeposited ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Списано бонусов</span>
    <strong class="metric-value">{formatNumber(report?.privilegeWithdrawn ?? 0)}</strong>
  </section>
</div>

<div class="admin-grid wide">
  <section class="panel">
    <div class="panel-header">
      <h2>По типам событий</h2>
    </div>
    {#if loading && !report}
      <div class="empty-state">Загружаем отчет...</div>
    {:else if countsByType.length}
      <div class="summary-list">
        {#each countsByType as [key, value]}
          <div class="summary-row">
            <span>{getStatisticEventTypeLabel(key)}</span>
            <strong>{formatNumber(value)}</strong>
          </div>
        {/each}
      </div>
    {:else}
      <div class="empty-state">В выбранном периоде событий не найдено.</div>
    {/if}
  </section>

  <section class="panel">
    <div class="panel-header">
      <h2>По сервисам</h2>
    </div>
    {#if loading && !report}
      <div class="empty-state">Загружаем отчет...</div>
    {:else if countsByService.length}
      <div class="summary-list">
        {#each countsByService as [key, value]}
          <div class="summary-row">
            <span>{getServiceLabel(key)}</span>
            <strong>{formatNumber(value)}</strong>
          </div>
        {/each}
      </div>
    {:else}
      <div class="empty-state">Нет данных по сервисам.</div>
    {/if}
  </section>
</div>

<section class="panel">
  <div class="panel-header">
    <h2>Последние события из отчета</h2>
  </div>
  {#if report?.recentEvents?.length}
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Время</th>
            <th>Событие</th>
            <th>Сервис</th>
            <th>Пользователь</th>
            <th>Сущность</th>
          </tr>
        </thead>
        <tbody>
          {#each report.recentEvents as event}
            <tr>
              <td>{formatDateTime(event.occurredAt)}</td>
              <td>{getStatisticEventTypeLabel(event.eventType)}</td>
              <td>{getServiceLabel(event.service)}</td>
              <td>{formatActor(event)}</td>
              <td>{event.entityType} {event.entityId}</td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {:else}
    <div class="empty-state">В отчете нет последних событий.</div>
  {/if}
</section>

<section class="panel">
  <div class="panel-toolbar">
    <div>
      <h2>Журнал событий</h2>
      <div class="muted">{formatNumber(totalItems)} записей</div>
    </div>
    <div class="pager">
      <button class="secondary-button" type="button" disabled={!eventsPage?.hasPrevious || loading} on:click={() => dispatch("pageChange", currentPage - 1)}>
        Назад
      </button>
      <span>Страница {currentPage} из {totalPages}</span>
      <button class="secondary-button" type="button" disabled={!eventsPage?.hasNext || loading} on:click={() => dispatch("pageChange", currentPage + 1)}>
        Вперед
      </button>
    </div>
  </div>

  {#if loading && !eventsPage}
    <div class="empty-state">Загружаем события...</div>
  {:else if eventsPage?.items.length}
    <div class="table-wrap">
      <table class="events-table">
        <thead>
          <tr>
            <th>Время</th>
            <th>Событие</th>
            <th>Сервис</th>
            <th>Пользователь</th>
            <th>Сущность</th>
            <th>Детали</th>
          </tr>
        </thead>
        <tbody>
          {#each eventsPage.items as event}
            <tr>
              <td>{formatDateTime(event.occurredAt)}</td>
              <td>
                <div>{getStatisticEventTypeLabel(event.eventType)}</div>
                <div class="muted">{event.eventId}</div>
              </td>
              <td>{getServiceLabel(event.service)}</td>
              <td>
                <div>{formatActor(event)}</div>
                {#if event.actorRoles.length}
                  <div class="muted">{event.actorRoles.map(titleCase).join(", ")}</div>
                {/if}
              </td>
              <td>
                <div>{event.entityType || "Не указано"}</div>
                <div class="muted">{event.entityId || "Без ID"}</div>
              </td>
              <td>
                {#if Object.keys(event.metadata).length}
                  <dl class="event-details">
                    {#each getStatisticMetadataDetails(event.metadata) as detail}
                      <div>
                        <dt>{detail.label}</dt>
                        <dd>{detail.value}</dd>
                      </div>
                    {/each}
                  </dl>
                {:else}
                  <span class="muted">Без дополнительных данных</span>
                {/if}
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {:else}
    <div class="empty-state">По текущим фильтрам события не найдены.</div>
  {/if}
</section>
