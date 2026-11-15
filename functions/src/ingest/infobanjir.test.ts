import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  RAIN_GAUGE,
  RIVER_STATIONS,
  RainfallRow,
  WaterLevelRow,
  floodLevel,
  parseRainfallTable,
  parseWaterLevelTable,
  rainfallLevel,
  rainfallReading,
  waterLevelReading,
} from "./infobanjir";
import { sampleText } from "./testing/samples";

/** The samples were captured around 21:05 MYT; most stations showed 20:45. */
const NOW = new Date("2026-10-23T20:55:00+08:00");
const [BUKIT_PUTRA, ANG_SENG] = RIVER_STATIONS;

const waterRows = () => parseWaterLevelTable(sampleText("infobanjir-water-level-kl.html"));
const rainRows = () => parseRainfallTable(sampleText("infobanjir-rainfall-kl.html"));
const row = <T extends { stationId: string }>(rows: T[], id: string) => rows.find((r) => r.stationId === id)!;

describe("InfoBanjir water level", () => {
  it("parses all 32 Kuala Lumpur stations from the real page", () => {
    assert.equal(waterRows().length, 32);
  });

  it("matches the thresholds in data-sources.md for both Sg. Klang stations", () => {
    const rows = waterRows();
    assert.deepEqual(row(rows, "0232121WL").thresholds, { normal: 18.0, alert: 24.7, warning: 26.2, danger: 26.7 });
    assert.deepEqual(row(rows, "0232131WL").thresholds, { normal: row(rows, "0232131WL").thresholds.normal, alert: 23.5, warning: 24.5, danger: 25.6 });
  });

  it("turns Sg. Klang di Bukit Putra into a normal flood_level reading", () => {
    const parsed = waterLevelReading(row(waterRows(), "0232121WL"), BUKIT_PUTRA, NOW);
    assert.ok(parsed.ok);
    const { id, data } = parsed.reading;

    assert.equal(id, "infobanjir_0232121WL_flood_level");
    assert.equal(data.stationName, "Sg. Klang di Bukit Putra (F2)");
    assert.equal(data.value, 18.96);
    assert.equal(data.unit, "m");
    assert.equal(data.level, "normal");
    assert.deepEqual(data.thresholds, { alert: 24.7, warning: 26.2, danger: 26.7 });
    assert.equal(data.stationDistanceKm, 3.2);
    assert.equal(data.summary.en, "Water level 18.96 m — Normal (Advisory at 24.70 m)");
    assert.equal(data.summary.ms, "Paras air 18.96 m — Normal (Waspada pada 24.70 m)");
    assert.equal(data.observedAt.toDate().toISOString(), "2026-10-23T12:45:00.000Z");
  });

  it("reads Jalan Ang Seng too", () => {
    const parsed = waterLevelReading(row(waterRows(), "0232131WL"), ANG_SENG, NOW);
    assert.ok(parsed.ok);
    assert.equal(parsed.reading.data.value, 19.69);
    assert.equal(parsed.reading.data.level, "normal");
  });

  it("maps JPS thresholds to levels at the exact boundaries", () => {
    const t = { alert: 24.7, warning: 26.2, danger: 26.7 };
    assert.equal(floodLevel(24.69, t), "normal");
    assert.equal(floodLevel(24.7, t), "alert");
    assert.equal(floodLevel(26.2, t), "warning");
    assert.equal(floodLevel(26.7, t), "danger");
  });

  it("names the next threshold in the summary", () => {
    const base = row(waterRows(), "0232121WL");
    const at = (value: number) => {
      const parsed = waterLevelReading({ ...base, waterLevel: value }, BUKIT_PUTRA, NOW);
      assert.ok(parsed.ok);
      return parsed.reading.data.summary.en;
    };
    assert.equal(at(25.12), "Water level 25.12 m — Advisory (Warning at 26.20 m)");
    assert.equal(at(26.5), "Water level 26.50 m — Warning (Danger at 26.70 m)");
    assert.equal(at(27), "Water level 27.00 m — Danger");
  });

  it("rejects -9999, missing, zero and stale readings", () => {
    const base: WaterLevelRow = row(waterRows(), "0232121WL");
    for (const waterLevel of [-9999, null, 0]) {
      assert.equal(waterLevelReading({ ...base, waterLevel }, BUKIT_PUTRA, NOW).ok, false, `value ${waterLevel}`);
    }
    // A 20:45 reading is still current at 21:30 (45 min) but stale at 21:31.
    assert.equal(waterLevelReading(base, BUKIT_PUTRA, new Date("2026-10-23T21:30:00+08:00")).ok, true);
    const late = waterLevelReading(base, BUKIT_PUTRA, new Date("2026-10-23T21:31:00+08:00"));
    assert.deepEqual(late, { ok: false, reason: "stale (23/10/2026 20:45)" });
    const badThresholds = waterLevelReading({ ...base, thresholds: { ...base.thresholds, warning: 20 } }, BUKIT_PUTRA, NOW);
    assert.equal(badThresholds.ok, false);
  });

  it("throws when the page layout changes", () => {
    assert.throws(() => parseWaterLevelTable("<table></table>"), /no rows/);
    assert.throws(() => parseWaterLevelTable("<td data-th='No'>1</td><td data-th='Station ID'>X</td>"), /layout changed/);
  });
});

describe("InfoBanjir rainfall", () => {
  it("parses all 56 rain gauges from the real page", () => {
    assert.equal(rainRows().length, 56);
  });

  it("reads the UM gauge columns in header order", () => {
    const um = row(rainRows(), "0232231RF");
    assert.equal(um.stationName, "Universiti Malaya (F2)");
    assert.deepEqual(um.daily, [39.0, 1.5, 1.0, 12.5, 1.0, 0.5]);
    assert.equal(um.sinceMidnight, 13.0);
    assert.equal(um.lastHour, 0.0);
  });

  it("turns the UM gauge into a rainfall reading", () => {
    const parsed = rainfallReading(row(rainRows(), "0232231RF"), RAIN_GAUGE, NOW);
    assert.ok(parsed.ok);
    const { id, data } = parsed.reading;

    assert.equal(id, "infobanjir_0232231RF_rainfall");
    assert.equal(data.value, 0);
    assert.equal(data.unit, "mm/h");
    assert.equal(data.level, "normal");
    assert.equal(data.stationDistanceKm, 0);
    assert.equal(data.summary.en, "No rain: 0.0 mm in the last hour");
    assert.equal(data.observedAt.toDate().toISOString(), "2026-10-23T12:45:00.000Z");
  });

  it("uses the JPS bands, with no danger level yet", () => {
    assert.equal(rainfallLevel(30), "normal");
    assert.equal(rainfallLevel(30.5), "alert");
    assert.equal(rainfallLevel(60), "alert");
    assert.equal(rainfallLevel(60.5), "warning");
    assert.equal(rainfallLevel(200), "warning");
  });

  it("accepts a dry week at the UM gauge (7 zero days)", () => {
    const dryWeek: RainfallRow = { ...row(rainRows(), "0232231RF"), daily: [0, 0, 0, 0, 0, 0], sinceMidnight: 0, lastHour: 0 };
    const parsed = rainfallReading(dryWeek, RAIN_GAUGE, NOW);
    assert.ok(parsed.ok);
    assert.equal(parsed.reading.data.value, 0);
    assert.equal(parsed.reading.data.level, "normal");
  });

  it("rejects 7 zero days on the known faulty Jalan Ang Seng gauge (real sample)", () => {
    const angSeng: RainfallRow = row(rainRows(), "0232131RF");
    assert.deepEqual(angSeng.daily, [0, 0, 0, 0, 0, 0]);
    const config = { ...RAIN_GAUGE, id: "0232131RF" };
    assert.deepEqual(rainfallReading(angSeng, config, NOW), { ok: false, reason: "all zero for 7 days on a known faulty gauge" });
  });

  it("rejects -9999 and stale readings", () => {
    const um = row(rainRows(), "0232231RF");
    assert.equal(rainfallReading({ ...um, lastHour: -9999 }, RAIN_GAUGE, NOW).ok, false);
    assert.equal(rainfallReading(um, RAIN_GAUGE, new Date("2026-10-23T21:30:00+08:00")).ok, true);
    assert.equal(rainfallReading(um, RAIN_GAUGE, new Date("2026-10-23T21:31:00+08:00")).ok, false);
  });

  it("throws when the column count changes", () => {
    const html = "<td data-th='No'>1</td><td data-th='Station ID'>0232231RF</td><td>UM</td>";
    assert.throws(() => parseRainfallTable(html), /expected 13 cells/);
  });
});
