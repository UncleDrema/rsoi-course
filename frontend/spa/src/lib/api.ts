import { env, resolveAdminUrl } from "./env";
import { getAccessToken } from "./auth";
import type {
  AdminCreateUserInput,
  AdminUser,
  Airport,
  ApiErrorPayload,
  BoughtTicket,
  CreateAirportInput,
  CreateFlightInput,
  Flight,
  PageDto,
  PrivilegeInfo,
  StatisticEvent,
  StatisticsReport,
  Ticket,
  UserInfo
} from "./types";

async function apiFetch<T>(input: string, init?: RequestInit, absolute = false): Promise<T> {
  const token = await getAccessToken();
  const headers = new Headers(init?.headers);
  headers.set("Accept", "application/json");

  if (!(init?.body instanceof FormData) && init?.body != null) {
    headers.set("Content-Type", "application/json");
  }

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(absolute ? input : `${env.apiBasePath}${input}`, {
    ...init,
    headers
  });

  if (!response.ok) {
    const payload = await parseJson<ApiErrorPayload>(response);
    throw new Error(payload?.message ?? payload?.error ?? `Request failed with ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await parseJson<T>(response)) as T;
}

async function parseJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }
  return JSON.parse(text) as T;
}

export function getFlights(page = 1, size = 12): Promise<PageDto<Flight>> {
  return apiFetch<PageDto<Flight>>(`/flights?page=${page}&size=${size}`);
}

export function getAirports(page = 1, size = 100): Promise<PageDto<Airport>> {
  return apiFetch<PageDto<Airport>>(`/airports?page=${page}&size=${size}`);
}

export function createAirport(payload: CreateAirportInput): Promise<Airport> {
  return apiFetch<Airport>("/airports", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function createFlight(payload: CreateFlightInput): Promise<Flight> {
  return apiFetch<Flight>("/flights", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function buyTicket(payload: { flightNumber: string; price: number; paidFromBalance: boolean }): Promise<BoughtTicket> {
  return apiFetch<BoughtTicket>("/tickets", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export function getTickets(): Promise<Ticket[]> {
  return apiFetch<Ticket[]>("/tickets");
}

export function cancelTicket(ticketUid: string): Promise<void> {
  return apiFetch<void>(`/tickets/${ticketUid}`, {
    method: "DELETE"
  });
}

export function getProfile(): Promise<UserInfo> {
  return apiFetch<UserInfo>("/me");
}

export function getPrivilege(): Promise<PrivilegeInfo> {
  return apiFetch<PrivilegeInfo>("/privilege");
}

export function getAdminUsers(): Promise<AdminUser[]> {
  return apiFetch<AdminUser[]>(resolveAdminUrl(env.adminUsersPath), undefined, Boolean(env.adminBaseUrl));
}

export function createAdminUser(payload: AdminCreateUserInput): Promise<AdminUser> {
  const body = {
    username: payload.username,
    email: payload.email,
    password: payload.password,
    name: payload.name
  };

  return apiFetch<AdminUser>(
    resolveAdminUrl(env.adminUsersPath),
    {
      method: "POST",
      body: JSON.stringify(body)
    },
    Boolean(env.adminBaseUrl)
  );
}

export function getStatisticsReport(): Promise<StatisticsReport> {
  return apiFetch<StatisticsReport>(
    resolveAdminUrl(env.adminReportPath),
    undefined,
    Boolean(env.adminBaseUrl)
  );
}

export function getStatisticEvents(): Promise<StatisticEvent[]> {
  return apiFetch<PageDto<StatisticEvent>>(
    resolveAdminUrl(env.adminEventsPath),
    undefined,
    Boolean(env.adminBaseUrl)
  ).then((page) => page.items);
}
