import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { DOE_URL, DoeAttributes, airQualityLevel, chooseStation, doeTime, parseDoeResponse, runDoe } from "./doe";
import { MemoryStore } from "./testing/memoryStore";
import { sampleText } from "./testing/samples";

/** The sample's readings are for 21:00 MYT; it was captured shortly after. */
const NOW = new Date("2026-10-23T21:10:00+08:00");

type Sample = { features: Array<{ attributes: DoeAttributes }> };

/** The real sample with some stations' readings changed. */
function sampleWith(changes: Record<string, Partial<DoeAttributes>>): string {
  const body = JSON.parse(sampleText("doe-apims-current-reading.json")) as Sample;
  for (const feature of body.features) {
    const change = changes[feature.attributes.STATION_ID ?? ""];
    if (change) Object.assign(feature.attributes, change);
  }
  return JSON.stringify(body);
}

const noReading = { API: null, DATETIME: null };

describe("DOE air quality", () => {
  it("corrects the 8-hour DATETIME quirk", () => {
    // Raw value decodes to 21:00 UTC, but the reading is for 21:00 MYT = 13:00 UTC.
    assert.equal(doeTime(1792789200000).toISOString(), "2026-10-23T13:00:00.000Z");
  });

  it("parses all 68 stations in the real sample", () => {
    assert.equal(parseDoeResponse(sampleText("doe-apims-current-reading.json")).length, 68);
  });

  it("uses CA19B Petaling Jaya when it has a reading", () => {
    const { reading, missing } = chooseStation(parseDoeResponse(sampleText("doe-apims-current-reading.json")), NOW);
    const data = reading!.data;

    assert.equal(reading!.id, "doe_CA19B_air_quality");
    assert.equal(data.stationName, "Petaling Jaya, SELANGOR");
    assert.equal(data.value, 143);
    assert.equal(data.unit, "API");
    assert.equal(data.level, "alert");
    assert.equal(data.stationDistanceKm, 4.6);
    assert.equal(data.location.latitude, 3.130875);
    assert.equal(data.summary.en, "Air Pollutant Index 143 — Unhealthy");
    assert.equal(data.summary.ms, "Indeks Pencemaran Udara 143 — Tidak Sihat");
    assert.equal(data.observedAt.toDate().toISOString(), "2026-10-23T13:00:00.000Z");
    assert.deepEqual(missing, []);
  });

  it("falls back to CA15W Batu Muda when CA19B is null, and records CA19B as missing", () => {
    const { reading, missing } = chooseStation(parseDoeResponse(sampleWith({ CA19B: noReading })), NOW);

    assert.equal(reading!.id, "doe_CA15W_air_quality");
    assert.equal(reading!.data.value, 158);
    assert.equal(reading!.data.stationDistanceKm, 10.1);
    assert.deepEqual(missing, ["CA19B"]);
  });

  it("rejects stale readings (older than 2 hours)", () => {
    const { reading, missing } = chooseStation(
      parseDoeResponse(sampleText("doe-apims-current-reading.json")),
      new Date("2026-10-23T23:30:00+08:00"),
    );
    assert.equal(reading, null);
    assert.deepEqual(missing, ["CA19B", "CA15W"]);
  });

  it("rejects -9999 and zero", () => {
    for (const API of [-9999, 0]) {
      const { reading } = chooseStation(parseDoeResponse(sampleWith({ CA19B: { API }, CA15W: noReading })), NOW);
      assert.equal(reading, null, `API ${API}`);
    }
  });

  it("maps API bands to levels", () => {
    assert.equal(airQualityLevel(100), "normal");
    assert.equal(airQualityLevel(101), "alert");
    assert.equal(airQualityLevel(200), "alert");
    assert.equal(airQualityLevel(201), "warning");
    assert.equal(airQualityLevel(300), "warning");
    assert.equal(airQualityLevel(301), "danger");
  });

  it("asks DOE for the two stations only", () => {
    assert.match(decodeURIComponent(DOE_URL), /where=STATION_ID IN \('CA19B','CA15W'\)/);
    assert.match(DOE_URL, /f=json/);
  });

  it("throws when the service shape changes", () => {
    assert.throws(() => parseDoeResponse(JSON.stringify({ error: { message: "Invalid query" } })), /Invalid query/);
    assert.throws(() => parseDoeResponse("{}"), /no features/);
  });
});

describe("runDoe", () => {
  it("keeps one air_quality doc: a fallback replaces the other station's doc", async () => {
    const store = new MemoryStore();
    await runDoe(store, NOW, async () => sampleText("doe-apims-current-reading.json"));
    assert.ok(store.reading("doe_CA19B_air_quality"));

    const run = await runDoe(store, NOW, async () => sampleWith({ CA19B: noReading }));

    assert.equal(run.ok, true);
    assert.deepEqual(run.stationsMissing, ["CA19B"]);
    assert.equal(store.reading("doe_CA19B_air_quality"), undefined);
    assert.equal(store.reading("doe_CA15W_air_quality")?.value, 158);
  });

  it("writes nothing and reports not ok when no station is usable", async () => {
    const store = new MemoryStore();
    await runDoe(store, NOW, async () => sampleText("doe-apims-current-reading.json"));

    const run = await runDoe(store, NOW, async () => sampleWith({ CA19B: noReading, CA15W: noReading }));

    assert.equal(run.ok, false);
    assert.deepEqual(run.stationsMissing, ["CA19B", "CA15W"]);
    assert.equal(store.reading("doe_CA19B_air_quality")?.value, 143, "last good reading is kept");
  });
});
