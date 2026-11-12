import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { Timestamp } from "firebase-admin/firestore";

import { runIngestion } from "./ingest";
import { writeReadings } from "./readings";
import { MemoryStore } from "./testing/memoryStore";
import { sampleGet, sampleText } from "./testing/samples";

const NOW = new Date("2026-10-23T20:55:00+08:00");
const minutes = (n: number) => new Date(NOW.getTime() + n * 60_000);
const failing = () => {
  throw new Error("HTTP 503 from publicinfobanjir.water.gov.my");
};

describe("runIngestion", () => {
  it("runs every source from the saved samples", async () => {
    const store = new MemoryStore();
    const summary = await runIngestion(store, NOW, sampleGet(), { force: true });

    assert.equal((summary.met as { ok: boolean }).ok, true);
    assert.equal((summary.infobanjir as { ok: boolean }).ok, true);
    assert.equal((summary.doe as { ok: boolean }).ok, true);
    assert.ok(store.reading("doe_CA19B_air_quality"));
    assert.ok(store.reading("infobanjir_0232121WL_flood_level"));
    assert.ok(store.reading("infobanjir_0232131WL_flood_level"));
    assert.ok(store.reading("infobanjir_0232231RF_rainfall"));
    assert.equal(store.status("infobanjir")?.ok, true);
    assert.deepEqual(store.status("infobanjir")?.stationsMissing, []);
    assert.equal((store.status("infobanjir")?.lastSuccessAt as Timestamp).toMillis(), NOW.getTime());
  });

  it("keeps the other sources running when InfoBanjir fails", async () => {
    const store = new MemoryStore();
    await runIngestion(store, NOW, sampleGet(), { force: true });

    const later = minutes(15);
    const summary = await runIngestion(
      store,
      later,
      sampleGet({ "aras-air-data": failing, "searchresultrainfall": failing }),
      { force: true },
    );

    assert.equal((summary.met as { ok: boolean }).ok, true);
    assert.equal((summary.doe as { ok: boolean }).ok, true);
    const status = store.status("infobanjir")!;
    assert.equal(status.ok, false);
    assert.match(status.error, /HTTP 503/);
    assert.equal((status.lastAttemptAt as Timestamp).toMillis(), later.getTime());
    // lastSuccessAt and the last good readings stay, so the app shows them as saved data.
    assert.equal((status.lastSuccessAt as Timestamp).toMillis(), NOW.getTime());
    assert.equal(store.reading("infobanjir_0232121WL_flood_level")?.value, 18.96);
    assert.equal(store.status("met")?.ok, true);
  });

  it("records a source as down even when its adapter throws outright", async () => {
    const store = new MemoryStore();
    const summary = await runIngestion(store, NOW, sampleGet({ "/weather/warning/": () => "not json" }), { force: true });

    assert.equal((summary.met as { ok: boolean }).ok, false);
    assert.equal(store.status("met")?.ok, false);
    assert.equal((summary.infobanjir as { ok: boolean }).ok, true);
  });

  it("marks InfoBanjir not ok and lists stale stations, keeping their last good docs", async () => {
    const store = new MemoryStore();
    await runIngestion(store, NOW, sampleGet(), { force: true });

    // 21:31: the 20:30 and 20:45 readings are older than 45 min.
    await runIngestion(store, new Date("2026-10-23T21:31:00+08:00"), sampleGet(), { force: true });

    const status = store.status("infobanjir")!;
    assert.equal(status.ok, false);
    assert.deepEqual([...status.stationsMissing].sort(), ["0232121WL", "0232131WL", "0232231RF"]);
    assert.equal((status.lastSuccessAt as Timestamp).toMillis(), NOW.getTime());
    assert.equal(store.reading("infobanjir_0232131WL_flood_level")?.value, 19.69);
  });

  it("fetches InfoBanjir every 15 minutes and MET every 5", async () => {
    const store = new MemoryStore();
    await runIngestion(store, NOW, sampleGet());

    const at5 = await runIngestion(store, minutes(5), sampleGet());
    assert.deepEqual(at5.infobanjir, { skipped: "not due" });
    assert.equal((at5.met as { ok: boolean }).ok, true);

    // The scheduler fires a little early or late; 14:30 later still counts as due.
    const at15 = await runIngestion(store, minutes(14.5), sampleGet());
    assert.notDeepEqual(at15.infobanjir, { skipped: "not due" });
  });
});

describe("writeReadings", () => {
  const reading = (value: number, observedAt: Date) => ({
    id: "infobanjir_0232121WL_flood_level",
    data: {
      type: "flood_level",
      source: "infobanjir",
      value,
      level: "normal",
      previousValue: null,
      observedAt: Timestamp.fromDate(observedAt),
      fetchedAt: Timestamp.fromDate(observedAt),
    },
  });
  const scope = { source: "infobanjir", type: "flood_level" } as const;

  it("skips unchanged readings even when the observation time moves on", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(18.96, NOW)], false);
    const result = await writeReadings(store, scope, [reading(18.96, minutes(15))], false);

    assert.deepEqual(result, { written: 0, deleted: 0, unchanged: 1 });
    assert.equal(store.commits, 1);
  });

  it("stores the previous value when the reading changes", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(18.96, NOW)], false);
    await writeReadings(store, scope, [reading(19.4, minutes(15))], false);

    assert.equal(store.reading("infobanjir_0232121WL_flood_level")?.previousValue, 18.96);
    assert.equal(store.reading("infobanjir_0232121WL_flood_level")?.value, 19.4);
  });

  it("deletes docs missing from the current list only when asked", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(18.96, NOW)], false);

    assert.equal((await writeReadings(store, scope, [], false)).deleted, 0);
    assert.equal((await writeReadings(store, scope, [], true)).deleted, 1);
  });

  it("still parses the real MET warning sample through the shared writer", async () => {
    const store = new MemoryStore();
    const summary = await runIngestion(store, NOW, sampleGet(), { force: true });
    assert.deepEqual(JSON.parse(sampleText("met-datagovmy-weather-warning.json")).length, 5);
    assert.equal((summary.met as { written: number }).written, 0);
    assert.ok(store.docs.get("forecasts/Tn079"));
  });
});
