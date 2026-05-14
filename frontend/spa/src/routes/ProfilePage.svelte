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
    <div class="eyebrow">Аккаунт</div>
    <h1>Профиль и привилегии</h1>
  </div>
</section>

{#if error}
  <div class="alert error">{error}</div>
{/if}

<div class="stats-grid">
  <section class="panel stat-panel">
    <span class="metric-label">Билеты</span>
    <strong class="metric-value">{formatNumber(profile?.tickets.length ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Баланс</span>
    <strong class="metric-value">{formatNumber(privilege?.balance ?? profile?.privilege?.balance ?? 0)}</strong>
  </section>
  <section class="panel stat-panel">
    <span class="metric-label">Уровень</span>
    <strong class="metric-value">{titleCase(privilege?.status ?? profile?.privilege?.status ?? null)}</strong>
  </section>
</div>

<section class="panel">
  <div class="panel-header">
    <h2>История бонусной программы</h2>
  </div>
  {#if loading}
    <div class="empty-state">Загружаем профиль...</div>
  {:else if privilege?.history?.length}
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Дата</th>
            <th>Билет</th>
            <th>Изменение</th>
            <th>Операция</th>
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
    <div class="empty-state">История бонусных операций пока пуста.</div>
  {/if}
</section>
