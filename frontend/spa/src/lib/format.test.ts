import { describe, expect, it } from "vitest";
import { formatMoney, titleCase } from "./format";

describe("format helpers", () => {
  it("formats money with digits", () => {
    expect(formatMoney(1500)).toContain("1");
  });

  it("converts enum strings to title case", () => {
    expect(titleCase("bronze_status")).toBe("Bronze Status");
  });
});
