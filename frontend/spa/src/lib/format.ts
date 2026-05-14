import type { ApiScalar, StatisticEventType, StatisticMetadataRecord, StatisticServiceName } from "./types";

const RU_LOCALE = "ru-RU";

const STATUS_LABELS: Record<string, string> = {
  PAID: "Оплачен",
  CANCELED: "Отменен",
  BRONZE: "Бронза",
  SILVER: "Серебро",
  GOLD: "Золото",
  FILL_IN_BALANCE: "Пополнение баланса",
  DEBIT_THE_ACCOUNT: "Списание со счета",
  BRONZE_STATUS: "Бронзовый статус",
  SILVER_STATUS: "Серебряный статус",
  GOLD_STATUS: "Золотой статус"
};

const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: "Администратор",
  ROLE_USER: "Пользователь",
  ADMIN: "Администратор",
  USER: "Пользователь"
};

const SERVICE_LABELS: Record<string, string> = {
  flights: "Рейсы",
  tickets: "Билеты",
  privileges: "Привилегии",
  statistics: "Статистика",
  gateway: "Шлюз"
};

const EVENT_TYPE_LABELS: Record<string, string> = {
  AIRPORT_CREATED: "Создан аэропорт",
  FLIGHTS_VIEWED: "Просмотрен список рейсов",
  FLIGHT_CREATED: "Создан рейс",
  FLIGHT_VIEWED: "Просмотрен рейс",
  PRIVILEGE_COMPENSATED: "Компенсированы бонусы",
  PRIVILEGE_DEPOSITED: "Начислены бонусы",
  PRIVILEGE_WITHDRAWN: "Списаны бонусы",
  TICKET_CANCELED: "Билет отменен",
  TICKET_CREATED: "Создан билет",
  TICKET_PURCHASED: "Билет куплен"
};

const METADATA_LABELS: Record<string, string> = {
  amount: "Сумма",
  city: "Город",
  country: "Страна",
  flightNumber: "Номер рейса",
  fromAirportId: "ID аэропорта вылета",
  name: "Название",
  operationType: "Операция",
  page: "Страница",
  paidByBonuses: "Оплачено бонусами",
  paidByMoney: "Оплачено деньгами",
  paidFromBalance: "Списано с баланса",
  price: "Цена",
  privilegeBalance: "Баланс бонусов",
  privilegeStatus: "Статус привилегий",
  size: "Размер страницы",
  status: "Статус",
  ticketUid: "Билет",
  toAirportId: "ID аэропорта прилета",
  totalElements: "Всего записей"
};

export function formatDateTime(value: string): string {
  const normalized = value.includes("T") ? value : value.replace(" ", "T");
  const date = new Date(normalized);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(RU_LOCALE, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function formatMoney(value: number): string {
  return new Intl.NumberFormat(RU_LOCALE, {
    style: "currency",
    currency: "RUB",
    maximumFractionDigits: 0
  }).format(value);
}

export function formatNumber(value: number | null | undefined): string {
  if (value == null) {
    return "0";
  }
  return new Intl.NumberFormat(RU_LOCALE).format(value);
}

export function titleCase(value: string | null | undefined): string {
  if (!value) {
    return "Неизвестно";
  }

  const canonical = value.trim().toUpperCase().replace(/[\s-]+/g, "_");
  if (STATUS_LABELS[canonical]) {
    return STATUS_LABELS[canonical];
  }
  if (ROLE_LABELS[canonical]) {
    return ROLE_LABELS[canonical];
  }
  if (EVENT_TYPE_LABELS[canonical]) {
    return EVENT_TYPE_LABELS[canonical];
  }
  if (SERVICE_LABELS[value.trim().toLowerCase()]) {
    return SERVICE_LABELS[value.trim().toLowerCase()];
  }

  return value
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function getStatusLabel(value: string | null | undefined): string {
  return titleCase(value);
}

export function getRoleLabel(value: string | null | undefined): string {
  if (!value) {
    return "Неизвестная роль";
  }

  return ROLE_LABELS[value.trim().toUpperCase()] ?? titleCase(value);
}

export function getServiceLabel(value: StatisticServiceName | string | null | undefined): string {
  if (!value) {
    return "Неизвестный сервис";
  }

  return SERVICE_LABELS[value.trim().toLowerCase()] ?? titleCase(value);
}

export function getStatisticEventTypeLabel(value: StatisticEventType | string | null | undefined): string {
  if (!value) {
    return "Неизвестное событие";
  }

  return EVENT_TYPE_LABELS[value.trim().toUpperCase()] ?? titleCase(value);
}

export function getStatisticMetadataLabel(key: string): string {
  return METADATA_LABELS[key] ?? titleCase(key);
}

export function formatScalarValue(value: ApiScalar): string {
  if (typeof value === "number") {
    return formatNumber(value);
  }

  if (typeof value === "boolean") {
    return value ? "Да" : "Нет";
  }

  if (value == null) {
    return "Нет данных";
  }

  return value;
}

export function getStatisticMetadataDetails(metadata: StatisticMetadataRecord): Array<{ key: string; label: string; value: string }> {
  return Object.entries(metadata).map(([key, value]) => ({
    key,
    label: getStatisticMetadataLabel(key),
    value:
      key === "status" || key === "privilegeStatus" || key === "operationType"
        ? getStatusLabel(typeof value === "string" ? value : null)
        : formatScalarValue(value)
  }));
}
