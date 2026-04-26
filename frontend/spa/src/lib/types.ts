export interface PageDto<T> {
  page: number;
  pageSize: number;
  totalElements: number;
  items: T[];
}

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
  [key: string]: unknown;
}

export interface StatisticEvent {
  [key: string]: unknown;
}

export interface ApiErrorPayload {
  message?: string;
  error?: string;
  [key: string]: unknown;
}
