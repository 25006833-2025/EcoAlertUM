import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, it } from "node:test";

import {
  MetForecastDay,
  MetWarning,
  forecastToDoc,
  parseMetTime,
  translatePhrase,
  warningLevel,
  warningStationId,
  warningToReading,
  warningsToReadings,
} from "./met";

/** Real responses captured on 23 Oct 2026 (docs/samples). Tests run from functions/lib/ingest. */
function sample<T>(name: string): T {
  return JSON.parse(readFileSync(join(__dirname, "..", "..", "..", "docs", "samples", name), "utf8")) as T;
}

const NOW = new Date("2026-10-23T16:05:00+08:00");

/** The mock scenario's warning, in MET's real format. */
const klThunderstorm: MetWarning = {
  warning_issue: { issued: "2026-10-23T15:30:00", title_en: "Thunderstorms Warning", title_bm: "Amaran Ribut Petir" },
  valid_from: "2026-10-23T15:30:00",
  valid_to: "2026-10-23T19:00:00",
  heading_en: "THUNDERSTORMS WARNING",
  text_en: "Thunderstorms, heavy rain and strong winds are expected over W.P. Kuala Lumpur until 7:00 PM.",
  text_bm: "Ribut petir, hujan lebat dan angin kencang dijangka di W.P. Kuala Lumpur sehingga 7:00 petang.",
};

describe("MET warnings", () => {
  it("ignores every real warning from 23 Oct (marine, no advisory, thunderstorms outside KL)", () => {
    const warnings = sample<MetWarning[]>("met-datagovmy-weather-warning.json");
    assert.equal(warnings.length, 5);
    assert.deepEqual(warningsToReadings(warnings, NOW), []);
  });

  it("turns a KL thunderstorm warning into an Advisory reading", () => {
    const reading = warningToReading(klThunderstorm, NOW)!;

    assert.equal(reading.id, "met_20261023T1530_thunderstorms-warning_weather_warning");
    assert.equal(reading.data.stationId, "20261023T1530_thunderstorms-warning");
    assert.equal(reading.data.level, "alert");
    assert.equal(reading.data.summary.en, "Thunderstorms Warning for Kuala Lumpur until 19:00");
    assert.equal(reading.data.summary.ms, "Amaran Ribut Petir bagi Kuala Lumpur sehingga 19:00");
    assert.equal(reading.data.validTo.toDate().toISOString(), "2026-10-23T11:00:00.000Z");
    assert.equal(reading.data.location.latitude, 3.127447);
  });

  it("matches Petaling too, because UM sits on the KL / Petaling border", () => {
    const petaling = { ...klThunderstorm, text_en: "Heavy rain over Selangor (Petaling, Klang).", text_bm: "" };
    assert.equal(warningToReading(petaling, NOW)!.data.summary.en, "Thunderstorms Warning for Petaling until 19:00");
  });

  it("escalates only on MET's explicit level wording", () => {
    const severe = { ...klThunderstorm, heading_en: "CONTINUOUS RAIN WARNING (SEVERE LEVEL)" };
    const danger = { ...klThunderstorm, heading_en: "CONTINUOUS RAIN WARNING (DANGER LEVEL)" };
    assert.equal(warningLevel(severe), "warning");
    assert.equal(warningLevel(danger), "danger");
  });

  it("skips marine, empty-validity and non-UM warnings", () => {
    const marine = { ...klThunderstorm, warning_issue: { ...klThunderstorm.warning_issue, title_en: "Strong Winds and Rough Seas Warning" } };
    const empty = { ...klThunderstorm, valid_to: klThunderstorm.valid_from };
    const elsewhere = { ...klThunderstorm, text_en: "Thunderstorms over Perak and Kedah.", text_bm: "" };
    assert.equal(warningLevel(marine), null);
    assert.equal(warningLevel(empty), null);
    assert.equal(warningLevel(elsewhere), null);
  });

  it("shows the date when a warning ends on a later day", () => {
    const overnight = { ...klThunderstorm, valid_to: "2026-10-24T01:00:00" };
    assert.equal(warningToReading(overnight, NOW)!.data.summary.en, "Thunderstorms Warning for Kuala Lumpur until 24/10 01:00");
  });

  it("keeps one reading per warning ID", () => {
    assert.equal(warningsToReadings([klThunderstorm, klThunderstorm], NOW).length, 1);
  });

  it("reads MET times as Malaysia time", () => {
    assert.equal(parseMetTime("2026-10-23T20:30:00")!.toISOString(), "2026-10-23T12:30:00.000Z");
    assert.equal(parseMetTime(null), null);
    assert.equal(parseMetTime("not a date"), null);
    assert.equal(warningStationId("2026-10-23T15:30:00", "No Advisory!"), "20261023T1530_no-advisory");
  });
});

describe("MET forecast", () => {
  it("builds the Kuala Lumpur (Tn079) forecast from the real sample", () => {
    const doc = forecastToDoc(sample<MetForecastDay[]>("met-datagovmy-forecast-kuala-lumpur.json"), "Tn079", NOW)!;

    assert.equal(doc.id, "Tn079");
    assert.equal(doc.data.locationName, "Kuala Lumpur");
    assert.equal(doc.data.days.length, 7);
    const dates = doc.data.days.map((d) => d.date);
    assert.deepEqual(dates, [...dates].sort());
    assert.deepEqual(doc.unknownPhrases, []);
    for (const day of doc.data.days) {
      assert.notEqual(day.summary.en, day.summary.ms, `untranslated phrase on ${day.date}`);
      assert.equal(typeof day.maxTemp, "number");
    }
  });

  it("returns null when the location is missing", () => {
    assert.equal(forecastToDoc([], "Tn079", NOW), null);
  });

  it("keeps unknown phrases in Malay and reports them", () => {
    const unknown = new Set<string>();
    assert.deepEqual(translatePhrase("Hujan renyai", unknown), { en: "Hujan renyai", ms: "Hujan renyai" });
    assert.deepEqual([...unknown], ["Hujan renyai"]);
    assert.deepEqual(translatePhrase("Ribut petir"), { en: "Thunderstorms", ms: "Ribut petir" });
  });
});
