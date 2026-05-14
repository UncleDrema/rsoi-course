export interface PageDto<T> {
  page: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
  items: T[];
}

export type ApiScalar = string | number | boolean | null;

export interface Flight {
  flightNumber: string;
  fromAirport: string;
  toAirport: string;
  date: string;
  price: number;
}

export interface Airport {
  id: number;
  name: string;
  city: string;
  country: string;
}

export interface CreateAirportInput {
  name: string;
  city: string;
  country: string;
}

export interface CreateFlightInput {
  flightNumber: string;
  datetime: string;
  fromAirportId: number;
  toAirportId: number;
  price: number;
}

export interface PrivilegeShortInfo {
  balance: number | null;
  status: string | null;
}

export interface PrivilegeHistoryItem {
  date: string;
  ticketUid: string;
  balanceDiff: number;
  operationType: string;
}

export interface PrivilegeInfo {
  balance: number;
  status: string;
  history: PrivilegeHistoryItem[];
}

export interface Ticket {
  ticketUid: string;
  flightNumber: string;
  fromAirport: string;
  toAirport: string;
  date: string;
  price: number;
  status: string;
}

export interface BoughtTicket extends Ticket {
  paidByMoney: number;
  paidByBonuses: number;
  privilege: PrivilegeShortInfo | null;
}

export interface UserInfo {
  tickets: Ticket[];
  privilege: PrivilegeShortInfo | null;
}

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  name: string;
  roles: string[];
}

export interface AdminCreateUserInput {
  username: string;
  email: string;
  password: string;
  name: string;
}

export interface StatisticsReport {
  from: string | null;
  to: string | null;
  totalEvents: number;
  ticketsPurchased: number;
  ticketsCanceled: number;
  flightsCreated: number;
  airportsCreated: number;
  privilegeDeposited: number;
  privilegeWithdrawn: number;
  privilegeCompensated: number;
  countsByEventType: Record<string, number>;
  countsByService: Record<string, number>;
  recentEvents: StatisticEvent[];
}

export type StatisticMetadataRecord = Record<string, ApiScalar>;

export type StatisticServiceName = "flights" | "tickets" | "privileges" | "statistics" | "gateway";

export type KnownStatisticEventType =
  | "AIRPORT_CREATED"
  | "FLIGHTS_VIEWED"
  | "FLIGHT_CREATED"
  | "FLIGHT_VIEWED"
  | "PRIVILEGE_COMPENSATED"
  | "PRIVILEGE_DEPOSITED"
  | "PRIVILEGE_WITHDRAWN"
  | "TICKET_CANCELED"
  | "TICKET_CREATED"
  | "TICKET_PURCHASED";

export type StatisticEventType = KnownStatisticEventType | string;

export type FlightsViewedMetadata = StatisticMetadataRecord & {
  page: number;
  size: number;
  totalElements: number;
};

export type FlightViewedMetadata = StatisticMetadataRecord & {
  flightNumber: string;
};

export type FlightCreatedMetadata = StatisticMetadataRecord & {
  flightNumber: string;
  fromAirportId: number;
  toAirportId: number;
  price: number;
};

export type AirportCreatedMetadata = StatisticMetadataRecord & {
  name: string;
  city: string;
  country: string;
};

export type TicketPurchasedMetadata = StatisticMetadataRecord & {
  flightNumber: string;
  price: number;
  paidFromBalance: boolean;
  paidByMoney: number;
  paidByBonuses: number;
  privilegeBalance: number;
  privilegeStatus: string;
};

export type TicketCanceledMetadata = StatisticMetadataRecord & {
  flightNumber: string;
  price: number;
  status: string;
  privilegeBalance: number;
  privilegeStatus: string;
};

export type PrivilegeEventMetadata = StatisticMetadataRecord & {
  ticketUid?: string;
  amount?: number;
  operationType?: string;
};

export interface StatisticEventBase<TEventType extends string, TMetadata extends StatisticMetadataRecord> {
  eventId: string;
  eventType: TEventType;
  service: string;
  actorSub: string | null;
  actorUsername: string | null;
  actorRoles: string[];
  entityType: string;
  entityId: string;
  metadata: TMetadata;
  occurredAt: string;
}

export type StatisticEvent =
  | StatisticEventBase<"AIRPORT_CREATED", AirportCreatedMetadata>
  | StatisticEventBase<"FLIGHT_CREATED", FlightCreatedMetadata>
  | StatisticEventBase<"FLIGHTS_VIEWED", FlightsViewedMetadata>
  | StatisticEventBase<"FLIGHT_VIEWED", FlightViewedMetadata>
  | StatisticEventBase<"PRIVILEGE_COMPENSATED", PrivilegeEventMetadata>
  | StatisticEventBase<"PRIVILEGE_DEPOSITED", PrivilegeEventMetadata>
  | StatisticEventBase<"PRIVILEGE_WITHDRAWN", PrivilegeEventMetadata>
  | StatisticEventBase<"TICKET_CANCELED", TicketCanceledMetadata>
  | StatisticEventBase<"TICKET_CREATED", StatisticMetadataRecord>
  | StatisticEventBase<"TICKET_PURCHASED", TicketPurchasedMetadata>
  | StatisticEventBase<string, StatisticMetadataRecord>;

export interface StatisticsFilters {
  from?: string | null;
  to?: string | null;
}

export interface StatisticsEventsQuery extends StatisticsFilters {
  page?: number;
  pageSize?: number;
  eventType?: StatisticEventType | null;
  service?: StatisticServiceName | string | null;
  actorSub?: string | null;
  actorUsername?: string | null;
  entityType?: string | null;
  entityId?: string | null;
  query?: string | null;
}

export interface ApiErrorPayload {
  message?: string;
  error?: string;
  details?: ApiScalar;
}
