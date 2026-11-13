import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { DocumentData, Timestamp } from "firebase-admin/firestore";

import { expireOldAlerts } from "./expireAlerts";
import { HOLD_MS, Reading, evaluateRules } from "./rules";
import { AlertStore, syncAlerts } from "./syncAlerts";

const NOW = new Date("2026-11-13T15:00:00+08:00");

/** A short fake reading for tests. */
function reading(id: string, type: Reading["type"], level: Reading["level"], extra: Partial<Reading> = {}): Reading {
  return { id, type, level, summary: { en: `${id} summary.`, ms: "" }, ...extra };
}

/** A pretend alerts collection kept in memory, so the tests need no database. */
function memoryAlertStore(): AlertStore & { docs: Map<string, DocumentData> } {
  const docs = new Map<string, DocumentData>();
  let next = 1;
  return {
    docs,
    async findActive(ruleId, area) {
      for (const [id, d] of docs) {
        if (d.ruleId === ruleId && d.area === area && d.status === "active") return { id, level: d.level };
      }
      return undefined;
    },
    async create(data) {
      const id = `alert${next++}`;
      docs.set(id, { ...data });
      return id;
    },
    async update(id, data) {
      docs.set(id, { ...docs.get(id), ...data });
    },
    async listActive() {
      return [...docs]
        .filter(([, d]) => d.status === "active")
        .map(([id, d]) => ({ id, expiresAt: (d.expiresAt as Timestamp).toDate() }));
    },
    async markExpired(ids) {
      for (const id of ids) docs.set(id, { ...docs.get(id), status: "expired" });
    },
  };
}

describe("alert rules", () => {
  it("raises nothing when every reading is normal", () => {
    const readings = [reading("f1", "flood_level", "normal"), reading("q1", "air_quality", "normal")];
    assert.deepEqual(evaluateRules(readings, NOW), []);
  });

  it("raises one alert per hazard at Advisory or above", () => {
    const readings = [
      reading("f1", "flood_level", "alert"),
      reading("q1", "air_quality", "warning"),
      reading("r1", "rainfall", "normal"),
    ];
    const ruleIds = evaluateRules(readings, NOW).map((c) => c.ruleId);
    assert.deepEqual(ruleIds, ["flood_level", "air_quality"]);
  });

  it("uses the most severe reading when several stations are high", () => {
    const readings = [reading("f1", "flood_level", "alert"), reading("f2", "flood_level", "danger")];
    const [candidate] = evaluateRules(readings, NOW);
    assert.equal(candidate.level, "danger");
    assert.deepEqual(candidate.readingIds, ["f1", "f2"]);
    assert.match(candidate.message.en, /^f2 summary\./);
  });

  it("ends a weather alert when MET's warning ends", () => {
    const validTo = new Date("2026-11-13T19:00:00+08:00");
    const [candidate] = evaluateRules([reading("w1", "weather_warning", "alert", { validTo: Timestamp.fromDate(validTo) })], NOW);
    assert.equal(candidate.expiresAt.getTime(), validTo.getTime());
  });

  it("holds other alerts for 3 hours", () => {
    const [candidate] = evaluateRules([reading("q1", "air_quality", "alert")], NOW);
    assert.equal(candidate.expiresAt.getTime(), NOW.getTime() + HOLD_MS);
  });
});

describe("syncAlerts", () => {
  it("creates a new active alert for the whole campus", async () => {
    const store = memoryAlertStore();
    const results = await syncAlerts(store, evaluateRules([reading("f1", "flood_level", "alert")], NOW), NOW);
    assert.equal(results[0].change, "new");
    const saved = store.docs.get(results[0].alertId)!;
    assert.equal(saved.status, "active");
    assert.equal(saved.area, "whole_campus");
  });

  it("does not create a duplicate on the next run (no repeat push)", async () => {
    const store = memoryAlertStore();
    const candidates = evaluateRules([reading("f1", "flood_level", "alert")], NOW);
    await syncAlerts(store, candidates, NOW);
    const second = await syncAlerts(store, candidates, new Date(NOW.getTime() + 5 * 60_000));
    assert.equal(second[0].change, "updated");
    assert.equal(store.docs.size, 1);
  });

  it("marks a higher level as escalated, on the same alert", async () => {
    const store = memoryAlertStore();
    const first = await syncAlerts(store, evaluateRules([reading("f1", "flood_level", "alert")], NOW), NOW);
    const second = await syncAlerts(store, evaluateRules([reading("f1", "flood_level", "warning")], NOW), NOW);
    assert.equal(second[0].change, "escalated");
    assert.equal(second[0].alertId, first[0].alertId);
    assert.equal(store.docs.get(first[0].alertId)!.level, "warning");
  });
});

describe("expireOldAlerts", () => {
  it("expires only alerts past their end time", async () => {
    const store = memoryAlertStore();
    await syncAlerts(store, evaluateRules([reading("q1", "air_quality", "alert")], NOW), NOW);
    assert.deepEqual(await expireOldAlerts(store, NOW), []);

    const later = new Date(NOW.getTime() + HOLD_MS + 1);
    const expired = await expireOldAlerts(store, later);
    assert.equal(expired.length, 1);
    assert.equal(store.docs.get(expired[0])!.status, "expired");
  });

  it("lets a new alert start after the old one expired", async () => {
    const store = memoryAlertStore();
    const candidates = evaluateRules([reading("q1", "air_quality", "alert")], NOW);
    await syncAlerts(store, candidates, NOW);
    await expireOldAlerts(store, new Date(NOW.getTime() + HOLD_MS + 1));
    const again = await syncAlerts(store, candidates, new Date(NOW.getTime() + HOLD_MS + 2));
    assert.equal(again[0].change, "new");
  });
});