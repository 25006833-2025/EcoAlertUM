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

  describe("fetchedAt across runs", () => {
    const BUKIT_PUTRA = "infobanjir_0232121WL_flood_level";
    const ANG_SENG = "infobanjir_0232131WL_flood_level";
    const ANG_SENG_VALUE = "\n        19.69</a>";
    const waterPage = (edit: (html: string) => string) => () => {
      const html = sampleText("infobanjir-water-level-kl.html");
      const edited = edit(html);
      assert.notEqual(edited, html, "the sample edit must change the page");
      return edited;
    };
    const fetchedAt = (store: MemoryStore, id: string) => (store.reading(id)!.fetchedAt as Timestamp).toMillis();

    const cases: Array<[string, (html: string) => string]> = [
      ["is rejected (-9999)", (html) => html.replace(ANG_SENG_VALUE, "\n        -9999</a>")],
      ["is missing from the page", (html) => html.replace(/<tr  class='item'><td data-th='No'>13<\/td>[\s\S]*?<\/tr>/, "")],
    ];
    for (const [name, edit] of cases) {
      it(`keeps the old fetchedAt when a station ${name}, and refreshes the healthy ones`, async () => {
        const store = new MemoryStore();
        await runIngestion(store, NOW, sampleGet(), { force: true });
        const before = { ...store.reading(ANG_SENG)! };

        const later = minutes(15);
        await runIngestion(store, later, sampleGet({ "aras-air-data": waterPage(edit) }), { force: true });

        assert.deepEqual(store.status("infobanjir")?.stationsMissing, ["0232131WL"]);
        assert.deepEqual(store.reading(ANG_SENG), before, "rejected or missing station is untouched");
        assert.equal(fetchedAt(store, ANG_SENG), NOW.getTime());
        assert.equal(fetchedAt(store, BUKIT_PUTRA), later.getTime(), "healthy unchanged station is confirmed");
        assert.equal(fetchedAt(store, "infobanjir_0232231RF_rainfall"), later.getTime());
      });
    }
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
  const ID = "infobanjir_0232121WL_flood_level";

  it("updates only fetchedAt on an unchanged reading", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(17.5, minutes(-15))], false);
    await writeReadings(store, scope, [reading(18.96, NOW)], false);
    const before = { ...store.reading(ID)! };

    // Same value, but the source's observation time and our fetch time move on.
    const result = await writeReadings(store, scope, [reading(18.96, minutes(15))], false);

    assert.deepEqual(result, { written: 0, deleted: 0, unchanged: 1 });
    const after = store.reading(ID)!;
    assert.ok((after.fetchedAt as Timestamp).isEqual(Timestamp.fromDate(minutes(15))));
    const withoutFetchedAt = (d: Record<string, unknown>) => Object.fromEntries(Object.entries(d).filter(([k]) => k !== "fetchedAt"));
    assert.deepEqual(withoutFetchedAt(after), withoutFetchedAt(before), "observedAt, previousValue and the rest are untouched");
  });

  it("writes a changed reading in full", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(18.96, NOW)], false);
    const result = await writeReadings(store, scope, [reading(19.4, minutes(15))], false);

    assert.deepEqual(result, { written: 1, deleted: 0, unchanged: 0 });
    const saved = store.reading(ID)!;
    assert.equal(saved.value, 19.4);
    assert.ok((saved.observedAt as Timestamp).isEqual(Timestamp.fromDate(minutes(15))));
    assert.ok((saved.fetchedAt as Timestamp).isEqual(Timestamp.fromDate(minutes(15))));
  });

  it("stores the previous value when the reading changes", async () => {
    const store = new MemoryStore();
    await writeReadings(store, scope, [reading(18.96, NOW)], false);
    await writeReadings(store, scope, [reading(19.4, minutes(15))], false);

    assert.equal(store.reading(ID)?.previousValue, 18.96);
    assert.equal(store.reading(ID)?.value, 19.4);
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
