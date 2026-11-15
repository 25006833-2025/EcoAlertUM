import { GeoPoint, Timestamp } from "firebase-admin/firestore";

import { HttpGet, Level, LocalizedText, Parsed, SourceRun, isSentinel, isStale, readingId, writeReadings } from "./readings";
import { IngestStore, StoredDoc } from "./store";

/**
 * DOE Air Pollutant Index (APIMS). No documented API; this is the ArcGIS query DOE's own public
 * map runs (docs/data-sources.md section 3), narrowed to the stations we use.
 */

/** Nearest stations to UM, in fallback order (distances are ours; DOE's own DISTANCE unit is unknown). */
export const DOE_STATIONS = [
  { id: "CA19B", distanceKm: 4.6 }, // Petaling Jaya
  { id: "CA15W", distanceKm: 10.1 }, // Batu Muda
] as const;

const QUERY_URL = "https://eqms.doe.gov.my/api3/publicmapproxy/PUBLIC_DISPLAY/CAQM_MCAQM_Current_Reading/MapServer/0/query";
const OUT_FIELDS = "STATION_ID,DATETIME,API,CLASS,PARAM_SYMBOL,LONGITUDE,LATITUDE,STATION_LOCATION,STATE_NAME";
const QUERY_PARAMS: Record<string, string> = {
  where: `STATION_ID IN (${DOE_STATIONS.map((s) => `'${s.id}'`).join(",")})`,
  outFields: OUT_FIELDS,
  returnGeometry: "false",
  f: "json",
};
export const DOE_URL = `${QUERY_URL}?${Object.entries(QUERY_PARAMS)
  .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
  .join("&")}`;

/** DOE updates hourly; polled every 15 min (architecture.md). Readings older than 2x the update interval are rejected. */
export const DOE_INTERVAL_MS = 15 * 60 * 1000;
export const DOE_MAX_AGE_MS = 2 * 60 * 60 * 1000;

const EIGHT_HOURS_MS = 8 * 60 * 60 * 1000;

export interface DoeAttributes {
  STATION_ID?: string;
  DATETIME?: number | null;
  API?: number | null;
  LATITUDE?: number | null;
  LONGITUDE?: number | null;
  STATION_LOCATION?: string | null;
  // CLASS and PARAM_SYMBOL are ignored: we compute the category ourselves, and PARAM_SYMBOL is undocumented.
}

/**
 * DATETIME is Malaysia wall-clock time stored as if it were UTC (DOE's own code reads it with getUTC*),
 * so the real instant is 8 hours earlier than the raw value.
 */
export function doeTime(raw: number): Date {
  return new Date(raw - EIGHT_HOURS_MS);
}

/** ArcGIS JSON -> station attributes. Throws when the shape changes. */
export function parseDoeResponse(json: string): DoeAttributes[] {
  const body = JSON.parse(json) as { features?: Array<{ attributes?: DoeAttributes }>; error?: { message?: string } };
  if (body.error) throw new Error(`DOE: ${body.error.message ?? "query error"}`);
  if (!Array.isArray(body.features)) throw new Error("DOE: response has no features (service changed?)");
  return body.features.map((f) => f.attributes ?? {});
}

/** DOE API bands (docs/data-models.md): 0-100 normal, 101-200 alert (Advisory), 201-300 warning, over 300 danger. */
export function airQualityLevel(api: number): Level {
  if (api > 300) return "danger";
  if (api > 200) return "warning";
  if (api > 100) return "alert";
  return "normal";
}

/** Category names from DOE's colour logic: 0-50 / 51-100 / 101-200 / 201-300 / over 300. */
function categoryOf(api: number): LocalizedText {
  if (api <= 50) return { en: "Good", ms: "Baik" };
  if (api <= 100) return { en: "Moderate", ms: "Sederhana" };
  if (api <= 200) return { en: "Unhealthy", ms: "Tidak Sihat" };
  if (api <= 300) return { en: "Very unhealthy", ms: "Sangat Tidak Sihat" };
  return { en: "Hazardous", ms: "Berbahaya" };
}

/** Rejects null, -9999, zero and stale readings (an API of exactly 0 does not happen in practice). */
export function airQualityReading(attrs: DoeAttributes, distanceKm: number, now: Date): Parsed<StoredDoc> {
  const api = attrs.API;
  if (api == null || isSentinel(api) || api <= 0) return { ok: false, reason: `no reading (API ${api})` };
  if (typeof attrs.DATETIME !== "number") return { ok: false, reason: "no DATETIME" };
  const observedAt = doeTime(attrs.DATETIME);
  if (isStale(observedAt, now, DOE_MAX_AGE_MS)) return { ok: false, reason: `stale (${observedAt.toISOString()})` };
  if (typeof attrs.LATITUDE !== "number" || typeof attrs.LONGITUDE !== "number") return { ok: false, reason: "no position" };

  const stationId = attrs.STATION_ID!;
  const value = Math.round(api);
  const category = categoryOf(value);
  return {
    ok: true,
    reading: {
      id: readingId("doe", stationId, "air_quality"),
      data: {
        type: "air_quality",
        source: "doe",
        stationId,
        stationName: attrs.STATION_LOCATION ?? stationId,
        location: new GeoPoint(attrs.LATITUDE, attrs.LONGITUDE),
        stationDistanceKm: distanceKm,
        value,
        unit: "API",
        previousValue: null,
        thresholds: null,
        level: airQualityLevel(value),
        summary: {
          en: `Air Pollutant Index ${value} — ${category.en}`,
          ms: `Indeks Pencemaran Udara ${value} — ${category.ms}`,
        },
        validFrom: null,
        validTo: null,
        observedAt: Timestamp.fromDate(observedAt),
        fetchedAt: Timestamp.fromDate(now),
      },
    },
  };
}

/** The first usable station in fallback order, plus the stations skipped on the way. */
export function chooseStation(rows: DoeAttributes[], now: Date): { reading: StoredDoc | null; missing: string[] } {
  const missing: string[] = [];
  for (const station of DOE_STATIONS) {
    const attrs = rows.find((r) => r.STATION_ID === station.id);
    const parsed = attrs ? airQualityReading(attrs, station.distanceKm, now) : { ok: false as const, reason: "not in response" };
    if (parsed.ok) return { reading: parsed.reading, missing };
    missing.push(station.id);
    console.warn(`DOE ${station.id} skipped: ${parsed.reason}`);
  }
  return { reading: null, missing };
}

/**
 * One DOE run: 1 request. Exactly one air_quality doc is kept (the station in use), so a fallback
 * replaces the other station's doc instead of leaving two for the app to choose from. When no
 * station is usable, nothing is written or deleted and the source is marked not ok.
 */
export async function runDoe(store: IngestStore, now: Date, get: HttpGet): Promise<SourceRun> {
  const { reading, missing } = chooseStation(parseDoeResponse(await get(DOE_URL)), now);
  if (!reading) {
    return { ok: false, error: "No usable DOE station", stationsMissing: missing, written: 0, deleted: 0, unchanged: 0 };
  }
  const result = await writeReadings(store, { source: "doe", type: "air_quality" }, [reading], true);
  return { ok: true, error: null, stationsMissing: missing, ...result };
}
