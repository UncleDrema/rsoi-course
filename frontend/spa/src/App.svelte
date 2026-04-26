<script lang="ts">
  import { onMount } from "svelte";
  import { get } from "svelte/store";
  import AppShell from "./lib/components/AppShell.svelte";
  import { authError, authReady, authUser, completeLogin, hasUserRole, initAuth, isAuthenticated, logout } from "./lib/auth";
  import {
    buyTicket,
    cancelTicket,
    createAirport,
    createAdminUser,
    createFlight,
    getAirports,
    getAdminUsers,
    getFlights,
    getPrivilege,
    getProfile,
    getStatisticEvents,
    getStatisticsReport,
    getTickets
  } from "./lib/api";
  import { navigate, route, type RoutePath } from "./lib/router";
  import type {
    AdminCreateUserInput,
    AdminUser,
    Airport,
    CreateAirportInput,
    CreateFlightInput,
    PageDto,
    Flight,
    PrivilegeInfo,
    StatisticEvent,
    StatisticsReport,
    Ticket,
    UserInfo
  } from "./lib/types";
  import AuthCallbackPage from "./routes/AuthCallbackPage.svelte";
  import FlightsPage from "./routes/FlightsPage.svelte";
  import LoginPage from "./routes/LoginPage.svelte";
  import TicketsPage from "./routes/TicketsPage.svelte";
  import ProfilePage from "./routes/ProfilePage.svelte";
  import AdminUsersPage from "./routes/AdminUsersPage.svelte";
  import AdminFlightsPage from "./routes/AdminFlightsPage.svelte";
  import AdminStatisticsPage from "./routes/AdminStatisticsPage.svelte";

  let flights: PageDto<Flight> | null = null;
  let flightPage = 1;
  let flightsLoading = false;
  let flightsError: string | null = null;
  let buyingFlight = "";
  let payFromBalance = true;

  let tickets: Ticket[] = [];
  let ticketsLoading = false;
  let ticketsError: string | null = null;
  let cancelingTicket = "";

  let profile: UserInfo | null = null;
  let privilege: PrivilegeInfo | null = null;
  let profileLoading = false;
  let profileError: string | null = null;

  let adminUsers: AdminUser[] = [];
  let adminUsersLoading = false;
  let adminUsersSaving = false;
  let adminUsersError: string | null = null;

  let adminAirports: Airport[] = [];
  let adminFlights: Flight[] = [];
  let adminFlightsLoading = false;
  let airportSaving = false;
  let flightSaving = false;
  let adminFlightsError: string | null = null;

  let statisticsReport: StatisticsReport | null = null;
  let statisticEvents: StatisticEvent[] = [];
  let statisticsLoading = false;
  let statisticsError: string | null = null;

  let callbackError: string | null = null;

  const protectedRoutes: RoutePath[] = [
    "/flights",
    "/tickets",
    "/profile",
    "/admin/users",
    "/admin/flights",
    "/admin/statistics"
  ];

  onMount(async () => {
    await initAuth();
  });

  $: currentRoute = $route;
  $: ready = $authReady;
  $: user = $authUser;
  $: sharedAuthError = $authError;
  $: isAdmin = hasUserRole(user, "ADMIN");

  $: if (ready) {
    void handleRoute(currentRoute);
  }

  async function handleRoute(path: RoutePath): Promise<void> {
    if (path === "/") {
      navigate(isAuthenticated() ? "/flights" : "/login", true);
      return;
    }

    if (path === "/auth/callback") {
      await processCallback();
      return;
    }

    if (protectedRoutes.includes(path) && !isAuthenticated()) {
      navigate("/login", true);
      return;
    }

    if (path.startsWith("/admin/") && !isAdmin) {
      navigate("/flights", true);
      return;
    }

    switch (path) {
      case "/flights":
        if (!flights && !flightsLoading) {
          await loadFlights(flightPage);
        }
        break;
      case "/tickets":
        if (!tickets.length && !ticketsLoading) {
          await loadTickets();
        }
        break;
      case "/profile":
        if (!profile && !profileLoading) {
          await loadProfile();
        }
        break;
      case "/admin/users":
        if (!adminUsers.length && !adminUsersLoading) {
          await loadAdminUsers();
        }
        break;
      case "/admin/flights":
        if (!adminAirports.length && !adminFlightsLoading) {
          await loadAdminFlights();
        }
        break;
      case "/admin/statistics":
        if (!statisticsReport && !statisticsLoading) {
          await loadStatistics();
        }
        break;
      default:
        break;
    }
  }

  async function processCallback(): Promise<void> {
    try {
      const returnPath = await completeLogin();
      callbackError = null;
      navigate((returnPath as RoutePath) || "/flights", true);
    } catch (error) {
      callbackError = error instanceof Error ? error.message : "Sign-in callback failed.";
    }
  }

  async function loadFlights(page: number): Promise<void> {
    flightsLoading = true;
    flightsError = null;
    try {
      flights = await getFlights(page);
      flightPage = flights.page;
    } catch (error) {
      flightsError = error instanceof Error ? error.message : "Unable to load flights.";
    } finally {
      flightsLoading = false;
    }
  }

  async function loadTickets(): Promise<void> {
    ticketsLoading = true;
    ticketsError = null;
    try {
      tickets = await getTickets();
    } catch (error) {
      ticketsError = error instanceof Error ? error.message : "Unable to load tickets.";
    } finally {
      ticketsLoading = false;
    }
  }

  async function loadProfile(): Promise<void> {
    profileLoading = true;
    profileError = null;
    try {
      const [profileData, privilegeData] = await Promise.all([getProfile(), getPrivilege()]);
      profile = profileData;
      privilege = privilegeData;
    } catch (error) {
      profileError = error instanceof Error ? error.message : "Unable to load profile.";
    } finally {
      profileLoading = false;
    }
  }

  async function loadAdminUsers(): Promise<void> {
    adminUsersLoading = true;
    adminUsersError = null;
    try {
      adminUsers = await getAdminUsers();
    } catch (error) {
      adminUsersError = error instanceof Error ? error.message : "Unable to load users.";
    } finally {
      adminUsersLoading = false;
    }
  }

  async function loadAdminFlights(): Promise<void> {
    adminFlightsLoading = true;
    adminFlightsError = null;
    try {
      const [airports, flightPageData] = await Promise.all([getAirports(), getFlights(1, 20)]);
      adminAirports = airports.items;
      adminFlights = flightPageData.items;
    } catch (error) {
      adminFlightsError = error instanceof Error ? error.message : "Unable to load flight administration data.";
    } finally {
      adminFlightsLoading = false;
    }
  }

  async function loadStatistics(): Promise<void> {
    statisticsLoading = true;
    statisticsError = null;
    try {
      const [report, events] = await Promise.all([getStatisticsReport(), getStatisticEvents()]);
      statisticsReport = report;
      statisticEvents = events;
    } catch (error) {
      statisticsError = error instanceof Error ? error.message : "Unable to load statistics.";
    } finally {
      statisticsLoading = false;
    }
  }

  async function handleBuy(event: CustomEvent<{ flightNumber: string; price: number; paidFromBalance: boolean }>): Promise<void> {
    buyingFlight = event.detail.flightNumber;
    flightsError = null;
    try {
      await buyTicket(event.detail);
      await Promise.all([loadTickets(), loadProfile()]);
      navigate("/tickets");
    } catch (error) {
      flightsError = error instanceof Error ? error.message : "Unable to buy ticket.";
    } finally {
      buyingFlight = "";
    }
  }

  async function handleCancel(event: CustomEvent<string>): Promise<void> {
    cancelingTicket = event.detail;
    ticketsError = null;
    try {
      await cancelTicket(event.detail);
      await Promise.all([loadTickets(), loadProfile()]);
    } catch (error) {
      ticketsError = error instanceof Error ? error.message : "Unable to cancel ticket.";
    } finally {
      cancelingTicket = "";
    }
  }

  async function handleCreateUser(event: CustomEvent<AdminCreateUserInput>): Promise<void> {
    adminUsersSaving = true;
    adminUsersError = null;
    try {
      const created = await createAdminUser(event.detail);
      adminUsers = [created, ...adminUsers];
    } catch (error) {
      adminUsersError = error instanceof Error ? error.message : "Unable to create user.";
    } finally {
      adminUsersSaving = false;
    }
  }

  async function handleCreateAirport(event: CustomEvent<CreateAirportInput>): Promise<void> {
    airportSaving = true;
    adminFlightsError = null;
    try {
      const created = await createAirport(event.detail);
      adminAirports = [created, ...adminAirports];
    } catch (error) {
      adminFlightsError = error instanceof Error ? error.message : "Unable to create airport.";
    } finally {
      airportSaving = false;
    }
  }

  async function handleCreateFlight(event: CustomEvent<CreateFlightInput>): Promise<void> {
    flightSaving = true;
    adminFlightsError = null;
    try {
      const created = await createFlight(event.detail);
      adminFlights = [created, ...adminFlights];
      flights = null;
    } catch (error) {
      adminFlightsError = error instanceof Error ? error.message : "Unable to create flight.";
    } finally {
      flightSaving = false;
    }
  }

  function handleNavigate(event: CustomEvent<RoutePath>): void {
    navigate(event.detail);
  }

  async function handleLogout(): Promise<void> {
    await logout();
  }
</script>

{#if !ready}
  <section class="center-state">
    <div class="state-card">
      <h1>Loading session</h1>
      <p>Checking the current OIDC session.</p>
    </div>
  </section>
{:else if currentRoute === "/login" && !user}
  <LoginPage error={sharedAuthError} />
{:else if currentRoute === "/auth/callback"}
  <AuthCallbackPage error={callbackError} />
{:else if user}
  <AppShell currentPath={currentRoute} user={user} authError={sharedAuthError} isAdmin={isAdmin} on:navigate={handleNavigate} on:logout={handleLogout}>
    {#if currentRoute === "/flights"}
      <FlightsPage
        pageData={flights}
        loading={flightsLoading}
        buyingFlight={buyingFlight}
        error={flightsError}
        payFromBalance={payFromBalance}
        on:pageChange={(event) => loadFlights(event.detail)}
        on:buy={handleBuy}
        on:payModeChange={(event) => (payFromBalance = event.detail)}
      />
    {:else if currentRoute === "/tickets"}
      <TicketsPage
        tickets={tickets}
        loading={ticketsLoading}
        cancelingTicket={cancelingTicket}
        error={ticketsError}
        on:cancel={handleCancel}
      />
    {:else if currentRoute === "/profile"}
      <ProfilePage profile={profile} privilege={privilege} loading={profileLoading} error={profileError} />
    {:else if currentRoute === "/admin/users" && isAdmin}
      <AdminUsersPage
        users={adminUsers}
        loading={adminUsersLoading}
        saving={adminUsersSaving}
        error={adminUsersError}
        on:create={handleCreateUser}
      />
    {:else if currentRoute === "/admin/flights" && isAdmin}
      <AdminFlightsPage
        airports={adminAirports}
        flights={adminFlights}
        loading={adminFlightsLoading}
        savingAirport={airportSaving}
        savingFlight={flightSaving}
        error={adminFlightsError}
        on:refresh={loadAdminFlights}
        on:createAirport={handleCreateAirport}
        on:createFlight={handleCreateFlight}
      />
    {:else if currentRoute === "/admin/statistics" && isAdmin}
      <AdminStatisticsPage
        report={statisticsReport}
        events={statisticEvents}
        loading={statisticsLoading}
        error={statisticsError}
      />
    {/if}
  </AppShell>
{:else}
  <LoginPage error={sharedAuthError} />
{/if}
