import { describe, expect, it } from "vitest";
import { formatMoney, formatNumber, getRoleLabel, getServiceLabel, getStatisticEventTypeLabel, getStatisticMetadataDetails, titleCase } from "./format";

describe("format helpers", () => {
  it("formats money in ru-RU", () => {
    expect(formatMoney(1500)).toContain("₽");
  });

  it("formats numbers in ru-RU", () => {
    expect(formatNumber(12500)).toMatch(/12\D*500/);
  });

  it("translates known statuses", () => {
    expect(titleCase("bronze_status")).toBe("Бронзовый статус");
    expect(titleCase("PAID")).toBe("Оплачен");
  });

  it("translates roles, services and event types", () => {
    expect(getRoleLabel("ROLE_ADMIN")).toBe("Администратор");
    expect(getServiceLabel("tickets")).toBe("Билеты");
    expect(getStatisticEventTypeLabel("TICKET_PURCHASED")).toBe("Билет куплен");
  });

  it("builds Russian metadata details", () => {
    expect(
      getStatisticMetadataDetails({
        privilegeStatus: "GOLD",
        paidFromBalance: true,
        price: 1500
      })
    ).toEqual([
      { key: "privilegeStatus", label: "Статус привилегий", value: "Золото" },
      { key: "paidFromBalance", label: "Списано с баланса", value: "Да" },
      { key: "price", label: "Цена", value: formatNumber(1500) }
    ]);
  });
});
