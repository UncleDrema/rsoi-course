import { env, resolveAdminUrl } from "./env";
import { getAccessToken } from "./auth";
import type {
  AdminCreateUserInput,
  AdminUser,
  Airport,
  ApiErrorPayload,
  ApiScalar,
  BoughtTicket,
  CreateAirportInput,
  CreateFlightInput,
  Flight,
  PageDto,
  PrivilegeInfo,
  StatisticEvent,
  StatisticMetadataRecord,
  StatisticsEventsQuery,
  StatisticsFilters,
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
    throw new Error(payload?.message ?? payload?.error ?? `Ошибка запроса: ${response.status}`);
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

type JsonObject = Record<string, ApiScalar | ApiScalar[] | Record<string, ApiScalar>>;

interface PageDtoResponse<T> {
  page: number;
  pageSize?: number;
  size?: number;
  totalElements: number;
  totalPages?: number;
  hasNext?: boolean;
  hasPrevious?: boolean;
  items: T[];
}

interface StatisticEventResponse {
  eventId: string;
  eventType: string;
  service: string;
  actorSub?: string | null;
  actorUsername?: string | null;
  actorRoles?: string[] | null;
  entityType: string;
  entityId: string;
  metadata?: JsonObject | null;
  occurredAt: string;
}

interface StatisticsReportResponse {
  from?: string | null;
  to?: string | null;
  totalEvents?: number;
  ticketsPurchased?: number;
  ticketsCanceled?: number;
  flightsCreated?: number;
  airportsCreated?: number;
  privilegeDeposited?: number;
  privilegeWithdrawn?: number;
  privilegeCompensated?: number;
  countsByEventType?: Record<string, number> | null;
  countsByService?: Record<string, number> | null;
  recentEvents?: StatisticEventResponse[] | null;
}

function buildQueryString(params: Record<string, string | number | null | undefined>): string {
  const search = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value == null || value === "") {
      continue;
    }
    search.set(key, String(value));
  }

  const query = search.toString();
  return query ? `?${query}` : "";
}

function adaptPageDto<T>(page: PageDtoResponse<T>): PageDto<T> {
  const pageSize = page.pageSize ?? page.size ?? page.items.length;
  const totalPages = page.totalPages ?? Math.max(1, Math.ceil(page.totalElements / Math.max(pageSize, 1)));

  return {
    page: page.page,
    pageSize,
    totalElements: page.totalElements,
    totalPages,
    hasNext: page.hasNext ?? page.page < totalPages,
    hasPrevious: page.hasPrevious ?? page.page > 1,
    items: page.items
  };
}

function toScalarMetadataRecord(value: JsonObject | null | undefined): StatisticMetadataRecord {
  if (!value) {
    return {};
  }

  const normalized: StatisticMetadataRecord = {};
  for (const [key, entry] of Object.entries(value)) {
    if (entry == null || typeof entry === "string" || typeof entry === "number" || typeof entry === "boolean") {
      normalized[key] = entry;
      continue;
    }

    if (Array.isArray(entry)) {
      normalized[key] = entry.map((item) => String(item)).join(", ");
      continue;
    }

    normalized[key] = JSON.stringify(entry);
  }
  return normalized;
}

function readString(record: StatisticMetadataRecord, key: string, fallback = ""): string {
  const value = record[key];
  return typeof value === "string" ? value : fallback;
}

function readNumber(record: StatisticMetadataRecord, key: string, fallback = 0): number {
  const value = record[key];
  return typeof value === "number" ? value : fallback;
}

function readBoolean(record: StatisticMetadataRecord, key: string, fallback = false): boolean {
  const value = record[key];
  return typeof value === "boolean" ? value : fallback;
}

function adaptStatisticEvent(event: StatisticEventResponse): StatisticEvent {
  const metadata = toScalarMetadataRecord(event.metadata);
  const base = {
    eventId: event.eventId,
    eventType: event.eventType,
    service: event.service,
    actorSub: event.actorSub ?? null,
    actorUsername: event.actorUsername ?? null,
    actorRoles: Array.isArray(event.actorRoles) ? event.actorRoles.filter((role): role is string => typeof role === "string") : [],
    entityType: event.entityType,
    entityId: event.entityId,
    occurredAt: event.occurredAt
  };

  switch (event.eventType) {
    case "FLIGHTS_VIEWED":
      return {
        ...base,
        eventType: "FLIGHTS_VIEWED",
        metadata: {
          ...metadata,
          page: readNumber(metadata, "page"),
          size: readNumber(metadata, "size"),
          totalElements: readNumber(metadata, "totalElements")
        }
      };
    case "FLIGHT_VIEWED":
      return { ...base, eventType: "FLIGHT_VIEWED", metadata: { ...metadata, flightNumber: readString(metadata, "flightNumber") } };
    case "FLIGHT_CREATED":
      return {
        ...base,
        eventType: "FLIGHT_CREATED",
        metadata: {
          ...metadata,
          flightNumber: readString(metadata, "flightNumber"),
          fromAirportId: readNumber(metadata, "fromAirportId"),
          toAirportId: readNumber(metadata, "toAirportId"),
          price: readNumber(metadata, "price")
        }
      };
    case "AIRPORT_CREATED":
      return {
        ...base,
        eventType: "AIRPORT_CREATED",
        metadata: {
          ...metadata,
          name: readString(metadata, "name"),
          city: readString(metadata, "city"),
          country: readString(metadata, "country")
        }
      };
    case "TICKET_PURCHASED":
      return {
        ...base,
        eventType: "TICKET_PURCHASED",
        metadata: {
          ...metadata,
          flightNumber: readString(metadata, "flightNumber"),
          price: readNumber(metadata, "price"),
          paidFromBalance: readBoolean(metadata, "paidFromBalance"),
          paidByMoney: readNumber(metadata, "paidByMoney"),
          paidByBonuses: readNumber(metadata, "paidByBonuses"),
          privilegeBalance: readNumber(metadata, "privilegeBalance"),
          privilegeStatus: readString(metadata, "privilegeStatus")
        }
      };
    case "TICKET_CANCELED":
      return {
        ...base,
        eventType: "TICKET_CANCELED",
        metadata: {
          ...metadata,
          flightNumber: readString(metadata, "flightNumber"),
          price: readNumber(metadata, "price"),
          status: readString(metadata, "status"),
          privilegeBalance: readNumber(metadata, "privilegeBalance"),
          privilegeStatus: readString(metadata, "privilegeStatus")
        }
      };
    case "PRIVILEGE_WITHDRAWN":
      return { ...base, eventType: "PRIVILEGE_WITHDRAWN", metadata };
    case "PRIVILEGE_DEPOSITED":
      return { ...base, eventType: "PRIVILEGE_DEPOSITED", metadata };
    case "PRIVILEGE_COMPENSATED":
      return { ...base, eventType: "PRIVILEGE_COMPENSATED", metadata };
    case "TICKET_CREATED":
      return { ...base, eventType: "TICKET_CREATED", metadata };
    default:
      return { ...base, metadata };
  }
}

function adaptStatisticsReport(report: StatisticsReportResponse): StatisticsReport {
  const countsByEventType = report.countsByEventType ?? {};

  return {
    from: report.from ?? null,
    to: report.to ?? null,
    totalEvents: report.totalEvents ?? 0,
    ticketsPurchased: report.ticketsPurchased ?? countsByEventType.TICKET_PURCHASED ?? 0,
    ticketsCanceled: report.ticketsCanceled ?? countsByEventType.TICKET_CANCELED ?? 0,
    flightsCreated: report.flightsCreated ?? countsByEventType.FLIGHT_CREATED ?? 0,
    airportsCreated: report.airportsCreated ?? countsByEventType.AIRPORT_CREATED ?? 0,
    privilegeDeposited: report.privilegeDeposited ?? countsByEventType.PRIVILEGE_DEPOSITED ?? 0,
    privilegeWithdrawn: report.privilegeWithdrawn ?? countsByEventType.PRIVILEGE_WITHDRAWN ?? 0,
    privilegeCompensated: report.privilegeCompensated ?? countsByEventType.PRIVILEGE_COMPENSATED ?? 0,
    countsByEventType,
    countsByService: report.countsByService ?? {},
    recentEvents: (report.recentEvents ?? []).map(adaptStatisticEvent)
  };
}

export function getFlights(page = 1, size = 12): Promise<PageDto<Flight>> {
  return apiFetch<PageDtoResponse<Flight>>(`/flights${buildQueryString({ page, size })}`).then(adaptPageDto);
}

export function getAirports(page = 1, size = 100): Promise<PageDto<Airport>> {
  return apiFetch<PageDtoResponse<Airport>>(`/airports${buildQueryString({ page, size })}`).then(adaptPageDto);
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

export function getStatisticsReport(filters?: StatisticsFilters): Promise<StatisticsReport> {
  return apiFetch<StatisticsReportResponse>(
    `${resolveAdminUrl(env.adminReportPath)}${buildQueryString({
      from: filters?.from ?? undefined,
      to: filters?.to ?? undefined
    })}`,
    undefined,
    Boolean(env.adminBaseUrl)
  ).then(adaptStatisticsReport);
}

export function getStatisticEvents(query: StatisticsEventsQuery = {}): Promise<PageDto<StatisticEvent>> {
  return apiFetch<PageDtoResponse<StatisticEventResponse>>(
    `${resolveAdminUrl(env.adminEventsPath)}${buildQueryString({
      from: query.from ?? undefined,
      to: query.to ?? undefined,
      eventType: query.eventType ?? undefined,
      service: query.service ?? undefined,
      actorSub: query.actorSub ?? undefined,
      actorUsername: query.actorUsername ?? undefined,
      entityType: query.entityType ?? undefined,
      entityId: query.entityId ?? undefined,
      query: query.query ?? undefined,
      page: query.page ?? undefined,
      size: query.pageSize ?? undefined
    })}`,
    undefined,
    Boolean(env.adminBaseUrl)
  ).then((page) =>
    adaptPageDto({
      ...page,
      items: page.items.map(adaptStatisticEvent)
    })
  );
}
