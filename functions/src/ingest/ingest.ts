import { DocumentData, Firestore, Timestamp } from "firebase-admin/firestore";

import {
  FORECAST_LOCATION_ID,
  MET_BASE_URL,
  MetForecastDay,
  MetWarning,
  WarningReading,
  forecastToDoc,
  warningsToReadings,
} from "./met";

/** MET forecast changes daily; 6 h keeps us far below MET's 4 requests per minute (architecture.md). */
export const FORECAST_INTERVAL_MS = 6 * 60 * 60 * 1000;
const FETCH_TIMEOUT_MS = 10_000;

export type FetchJson = (url: string) => Promise<unknown>;

export interface IngestSummary {
  met: { ok: boolean; written: number; deleted: number; unchanged: number; forecast: "updated" | "skipped" | "failed"; error?: string };
}

/** GET with a timeout. Throws on HTTP errors so the caller can record the source as down. */
export const fetchJson: FetchJson = async (url) => {
  const response = await fetch(url, {
    headers: { Accept: "application/json", "User-Agent": "EcoAlertUM-ingest" },
    signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status} from ${url}`);
  return response.json();
};

/**
 * One ingestion run. Each source is isolated: a failure only marks that source as down and
 * leaves its last readings in place (architecture.md, Flow 2). Only MET is implemented so far;
 * InfoBanjir and DOE adapters plug in here the same way.
 */
export async function runIngestion(db: Firestore, now: Date = new Date(), get: FetchJson = fetchJson): Promise<IngestSummary> {
  return { met: await ingestMet(db, now, get) };
}

async function ingestMet(db: Firestore, now: Date, get: FetchJson): Promise<IngestSummary["met"]> {
  const statusRef = db.collection("sourceStatus").doc("met");
  try {
    const warnings = await get(`${MET_BASE_URL}/warning/`);
    if (!Array.isArray(warnings)) throw new Error("MET warnings: expected a JSON array");
    const result = await syncWarnings(db, warningsToReadings(warnings as MetWarning[], now));
    const forecast = await maybeUpdateForecast(db, now, get);

    await statusRef.set({
      ok: true,
      lastSuccessAt: Timestamp.fromDate(now),
      lastAttemptAt: Timestamp.fromDate(now),
      error: null,
      stationsMissing: [],
    });
    return { ok: true, ...result, forecast };
  } catch (e) {
    const error = e instanceof Error ? e.message : String(e);
    // merge keeps lastSuccessAt, so the app can show "saved <time>".
    await statusRef.set({ ok: false, lastAttemptAt: Timestamp.fromDate(now), error }, { merge: true });
    return { ok: false, written: 0, deleted: 0, unchanged: 0, forecast: "skipped", error };
  }
}

/**
 * Makes `hazardReadings` match the current MET warnings for UM: writes new or changed ones,
 * leaves unchanged ones alone (fewer writes and listener updates), and deletes warnings MET
 * no longer publishes.
 */
export async function syncWarnings(
  db: Firestore,
  readings: WarningReading[],
): Promise<{ written: number; deleted: number; unchanged: number }> {
  const collection = db.collection("hazardReadings");
  const existing = await collection.where("source", "==", "met").where("type", "==", "weather_warning").get();
  const existingById = new Map(existing.docs.map((doc) => [doc.id, doc.data()]));
  const currentIds = new Set(readings.map((r) => r.id));

  const batch = db.batch();
  let written = 0;
  let unchanged = 0;
  for (const reading of readings) {
    const old = existingById.get(reading.id);
    if (old && sameWarning(old, reading)) {
      unchanged++;
    } else {
      batch.set(collection.doc(reading.id), reading.data);
      written++;
    }
  }
  let deleted = 0;
  for (const id of existingById.keys()) {
    if (!currentIds.has(id)) {
      batch.delete(collection.doc(id));
      deleted++;
    }
  }
  if (written + deleted > 0) await batch.commit();
  return { written, deleted, unchanged };
}

/** Same content apart from fetchedAt. */
function sameWarning(old: DocumentData, reading: WarningReading): boolean {
  const d = reading.data;
  return old.level === d.level
    && old.stationName === d.stationName
    && old.summary?.en === d.summary.en
    && old.summary?.ms === d.summary.ms
    && sameTime(old.validFrom, d.validFrom)
    && sameTime(old.validTo, d.validTo);
}

function sameTime(a: unknown, b: Timestamp): boolean {
  return a instanceof Timestamp && a.isEqual(b);
}

async function maybeUpdateForecast(db: Firestore, now: Date, get: FetchJson): Promise<"updated" | "skipped" | "failed"> {
  const ref = db.collection("forecasts").doc(FORECAST_LOCATION_ID);
  const saved = await ref.get();
  const fetchedAt = saved.get("fetchedAt");
  if (fetchedAt instanceof Timestamp && now.getTime() - fetchedAt.toMillis() < FORECAST_INTERVAL_MS) {
    return "skipped";
  }
  try {
    const url = `${MET_BASE_URL}/forecast/?contains=${FORECAST_LOCATION_ID}@location__location_id`;
    const days = await get(url);
    if (!Array.isArray(days)) throw new Error("MET forecast: expected a JSON array");
    const doc = forecastToDoc(days as MetForecastDay[], FORECAST_LOCATION_ID, now);
    if (!doc) throw new Error(`MET forecast: no days for ${FORECAST_LOCATION_ID}`);
    if (doc.unknownPhrases.length > 0) console.warn("MET forecast: unknown phrases", doc.unknownPhrases);
    await ref.set(doc.data);
    return "updated";
  } catch (e) {
    // The forecast is informational only; a failure must not mark warnings as down.
    console.error("MET forecast failed", e);
    return "failed";
  }
}
