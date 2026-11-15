import { GeoPoint, Timestamp } from "firebase-admin/firestore";

import {
  HttpGet,
  Level,
  Parsed,
  SourceRun,
  UM_LOCATION,
  cellNumber,
  isSentinel,
  isStale,
  parseMytDateTime,
  readingId,
  writeReadings,
} from "./readings";
import { IngestStore, StoredDoc } from "./store";

/**
 * JPS Public InfoBanjir: river water levels and rainfall for WP Kuala Lumpur. There is no API;
 * these are the HTML fragments the public pages load (docs/data-sources.md section 2).
 */

const BASE = "https://publicinfobanjir.water.gov.my";
export const WATER_LEVEL_URL = `${BASE}/index.php/aras-air/data-paras-air/aras-air-data/?state=WLH&district=Kuala%20Lumpur&station=ALL`;
export const RAINFALL_URL = `${BASE}/wp-content/themes/shapely/agency/searchresultrainfall.php?state=WLH&district=Kuala%20Lumpur&station=ALL&loginStatus=0&language=1`;

/**
 * InfoBanjir updates every 15 min, but the page lags: at 21:05 the 23 Oct sample still showed
 * 20:30 for some stations (35 min old). 45 min (3x) avoids rejecting current readings just before
 * each update, and matches the app's outdated limit for InfoBanjir.
 */
export const INFOBANJIR_INTERVAL_MS = 15 * 60 * 1000;
export const INFOBANJIR_MAX_AGE_MS = 45 * 60 * 1000;

/** Hand-kept station list: the tables have no coordinates (positions from data.gov.my, to confirm). */
export interface StationConfig {
  id: string;
  location: GeoPoint;
  distanceKm: number;
}

export const RIVER_STATIONS: StationConfig[] = [
  { id: "0232121WL", location: new GeoPoint(3.113333, 101.67586), distanceKm: 3.2 }, // Sg. Klang di Bukit Putra (F2)
  { id: "0232131WL", location: new GeoPoint(3.121864, 101.678657), distanceKm: 3.2 }, // Sg. Klang di Jalan Ang Seng (F2)
];

export const RAIN_GAUGE: StationConfig = { id: "0232231RF", location: UM_LOCATION, distanceKm: 0 }; // Universiti Malaya (F2)

/**
 * Gauges known to report 0 when broken. data-sources.md found Jalan Ang Seng (F2) at 0 for 7 days
 * while its neighbours recorded rain. Other gauges may have a genuinely dry week, so the rule is not general.
 */
export const KNOWN_FAULTY_GAUGES: ReadonlySet<string> = new Set(["0232131RF"]);

// --- Parsing ---------------------------------------------------------------------------------

export interface FloodThresholds {
  alert: number;
  warning: number;
  danger: number;
}

export interface WaterLevelRow {
  stationId: string;
  stationName: string;
  lastUpdated: string;
  waterLevel: number | null;
  thresholds: { normal: number | null; alert: number | null; warning: number | null; danger: number | null };
}

export interface RainfallRow {
  stationId: string;
  stationName: string;
  lastUpdated: string;
  /** Previous 6 daily totals, rainfall since midnight, then the last hour (header order). */
  daily: Array<number | null>;
  sinceMidnight: number | null;
  lastHour: number | null;
}

const WATER_LEVEL_LABELS = ["Station ID", "Station Name", "Last Update", "wl", "Normal", "Alert", "Warning", "Danger"];
const RAINFALL_CELLS = 13;

function stripTags(html: string): string {
  return html.replace(/<[^>]*>/g, "").replace(/&nbsp;/g, " ").replace(/\s+/g, " ").trim();
}

/** Rows start at the "No" cell; the HTML is missing opening `<tr>` tags, so rows can't be found by `<tr>`. */
function rowChunks(html: string): string[] {
  return html.split("<td data-th='No'>").slice(1).map((chunk) => `<td data-th='No'>${chunk}`);
}

/**
 * Water-level table. Every cell carries a `data-th` label, so cells are read by label rather than
 * position. Throws when the layout changes, so the source is marked down instead of publishing bad data.
 */
export function parseWaterLevelTable(html: string): WaterLevelRow[] {
  const chunks = rowChunks(html);
  if (chunks.length === 0) throw new Error("InfoBanjir water level: no rows found (page layout changed?)");
  return chunks.map((chunk) => {
    const cells = new Map<string, string>();
    for (const m of chunk.matchAll(/<td data-th='([^']*)'>([\s\S]*?)<\/td>/g)) {
      if (!cells.has(m[1])) cells.set(m[1], stripTags(m[2]));
    }
    const missing = WATER_LEVEL_LABELS.filter((label) => !cells.has(label));
    if (missing.length > 0) throw new Error(`InfoBanjir water level: row without ${missing.join(", ")} (page layout changed?)`);
    return {
      stationId: cells.get("Station ID")!,
      stationName: cells.get("Station Name")!,
      lastUpdated: cells.get("Last Update")!,
      waterLevel: cellNumber(cells.get("wl")),
      thresholds: {
        normal: cellNumber(cells.get("Normal")),
        alert: cellNumber(cells.get("Alert")),
        warning: cellNumber(cells.get("Warning")),
        danger: cellNumber(cells.get("Danger")),
      },
    };
  });
}

/**
 * Rainfall table. Only "No" and "Station ID" are labelled, so the other cells are read by position
 * (order confirmed from the header's colspan/rowspan). A different cell count means the layout changed.
 */
export function parseRainfallTable(html: string): RainfallRow[] {
  const chunks = rowChunks(html);
  if (chunks.length === 0) throw new Error("InfoBanjir rainfall: no rows found (page layout changed?)");
  return chunks.map((chunk) => {
    const cells = [...chunk.matchAll(/<td[^>]*>([\s\S]*?)<\/td>/g)].map((m) => stripTags(m[1]));
    if (cells.length !== RAINFALL_CELLS) {
      throw new Error(`InfoBanjir rainfall: expected ${RAINFALL_CELLS} cells per row, got ${cells.length} (page layout changed?)`);
    }
    return {
      stationId: cells[1],
      stationName: cells[2],
      lastUpdated: cells[4],
      daily: cells.slice(5, 11).map(cellNumber),
      sinceMidnight: cellNumber(cells[11]),
      lastHour: cellNumber(cells[12]),
    };
  });
}

// --- Levels and readings ---------------------------------------------------------------------

/** JPS thresholds per station: below Waspada normal, then alert (Advisory), warning, danger. */
export function floodLevel(value: number, t: FloodThresholds): Level {
  if (value >= t.danger) return "danger";
  if (value >= t.warning) return "warning";
  if (value >= t.alert) return "alert";
  return "normal";
}

/**
 * JPS 1-hour bands (docs/data-models.md): 0-30 mm normal (light/moderate), over 30 alert (heavy),
 * over 60 warning (very heavy).
 * TODO: "danger" is not set from 1-hour rain alone. Proposed rule: >= 60 mm over the last 3 h
 * (JPS flash-flood guidance), still to confirm and needs stored history.
 */
export function rainfallLevel(mmLastHour: number): Level {
  if (mmLastHour > 60) return "warning";
  if (mmLastHour > 30) return "alert";
  return "normal";
}

const FLOOD_WORDS: Record<Level, LocalizedPair> = {
  normal: { en: "Normal", ms: "Normal" },
  alert: { en: "Advisory", ms: "Waspada" },
  warning: { en: "Warning", ms: "Amaran" },
  danger: { en: "Danger", ms: "Bahaya" },
};

interface LocalizedPair {
  en: string;
  ms: string;
}

function floodSummary(value: number, level: Level, t: FloodThresholds): LocalizedPair {
  const metres = value.toFixed(2);
  const next: { level: Level; at: number } | null =
    level === "normal" ? { level: "alert", at: t.alert }
      : level === "alert" ? { level: "warning", at: t.warning }
        : level === "warning" ? { level: "danger", at: t.danger }
          : null;
  const word = FLOOD_WORDS[level];
  const suffix = next
    ? { en: ` (${FLOOD_WORDS[next.level].en} at ${next.at.toFixed(2)} m)`, ms: ` (${FLOOD_WORDS[next.level].ms} pada ${next.at.toFixed(2)} m)` }
    : { en: "", ms: "" };
  return {
    en: `Water level ${metres} m — ${word.en}${suffix.en}`,
    ms: `Paras air ${metres} m — ${word.ms}${suffix.ms}`,
  };
}

/** JPS intensity names for the last hour (BM wording to confirm). */
function rainfallSummary(mm: number): LocalizedPair {
  const amount = mm.toFixed(1);
  const [en, ms] =
    mm === 0 ? ["No rain", "Tiada hujan"]
      : mm <= 10 ? ["Light rain", "Hujan renyai"]
        : mm <= 30 ? ["Moderate rain", "Hujan sederhana"]
          : mm <= 60 ? ["Heavy rain", "Hujan lebat"]
            : ["Very heavy rain", "Hujan sangat lebat"];
  return { en: `${en}: ${amount} mm in the last hour`, ms: `${ms}: ${amount} mm dalam sejam lalu` };
}

/** Rejects -9999, missing values, impossible zeros, bad thresholds and stale readings (docs/data-sources.md). */
export function waterLevelReading(row: WaterLevelRow, station: StationConfig, now: Date): Parsed<StoredDoc> {
  const value = row.waterLevel;
  if (value == null || isSentinel(value) || value <= 0) return { ok: false, reason: `invalid water level ${row.waterLevel}` };
  const { alert, warning, danger } = row.thresholds;
  if (alert == null || warning == null || danger == null || !(alert < warning && warning < danger)) {
    return { ok: false, reason: "invalid thresholds" };
  }
  const observedAt = parseMytDateTime(row.lastUpdated);
  if (!observedAt) return { ok: false, reason: `unreadable time "${row.lastUpdated}"` };
  if (isStale(observedAt, now, INFOBANJIR_MAX_AGE_MS)) return { ok: false, reason: `stale (${row.lastUpdated})` };

  const thresholds = { alert, warning, danger };
  const level = floodLevel(value, thresholds);
  return {
    ok: true,
    reading: {
      id: readingId("infobanjir", station.id, "flood_level"),
      data: {
        type: "flood_level",
        source: "infobanjir",
        stationId: station.id,
        stationName: row.stationName,
        location: station.location,
        stationDistanceKm: station.distanceKm,
        value,
        unit: "m",
        previousValue: null,
        thresholds,
        level,
        summary: floodSummary(value, level, thresholds),
        validFrom: null,
        validTo: null,
        observedAt: Timestamp.fromDate(observedAt),
        fetchedAt: Timestamp.fromDate(now),
      },
    },
  };
}

/** A gauge on [KNOWN_FAULTY_GAUGES] reporting exactly 0 for every day and hour is treated as broken, not dry. */
export function rainfallReading(row: RainfallRow, station: StationConfig, now: Date): Parsed<StoredDoc> {
  const value = row.lastHour;
  if (value == null || isSentinel(value) || value < 0) return { ok: false, reason: `invalid rainfall ${row.lastHour}` };
  const all = [...row.daily, row.sinceMidnight, row.lastHour];
  if (KNOWN_FAULTY_GAUGES.has(row.stationId) && all.every((v) => v === 0)) {
    return { ok: false, reason: "all zero for 7 days on a known faulty gauge" };
  }
  const observedAt = parseMytDateTime(row.lastUpdated);
  if (!observedAt) return { ok: false, reason: `unreadable time "${row.lastUpdated}"` };
  if (isStale(observedAt, now, INFOBANJIR_MAX_AGE_MS)) return { ok: false, reason: `stale (${row.lastUpdated})` };

  return {
    ok: true,
    reading: {
      id: readingId("infobanjir", station.id, "rainfall"),
      data: {
        type: "rainfall",
        source: "infobanjir",
        stationId: station.id,
        stationName: row.stationName,
        location: station.location,
        stationDistanceKm: station.distanceKm,
        value,
        unit: "mm/h",
        previousValue: null,
        thresholds: null,
        level: rainfallLevel(value),
        summary: rainfallSummary(value),
        validFrom: null,
        validTo: null,
        observedAt: Timestamp.fromDate(observedAt),
        fetchedAt: Timestamp.fromDate(now),
      },
    },
  };
}

// --- Adapter ---------------------------------------------------------------------------------

/**
 * One InfoBanjir run: 2 requests. Good readings are written; rejected stations keep their last
 * good doc and are listed in stationsMissing. The run only counts as ok when every configured
 * station was read, because the app treats `lastSuccessAt` as "all readings of this source are current".
 */
export async function runInfobanjir(store: IngestStore, now: Date, get: HttpGet): Promise<SourceRun> {
  const [water, rain] = await Promise.allSettled([get(WATER_LEVEL_URL), get(RAINFALL_URL)]);
  const errors: string[] = [];
  const missing: string[] = [];
  const flood: StoredDoc[] = [];
  const rainfall: StoredDoc[] = [];

  const collect = <Row extends { stationId: string }>(
    page: PromiseSettledResult<string>,
    parse: (html: string) => Row[],
    stations: StationConfig[],
    toReading: (row: Row, station: StationConfig, now: Date) => Parsed<StoredDoc>,
    out: StoredDoc[],
  ) => {
    try {
      if (page.status === "rejected") throw page.reason;
      const rows = parse(page.value);
      for (const station of stations) {
        const row = rows.find((r) => r.stationId === station.id);
        const parsed = row ? toReading(row, station, now) : { ok: false as const, reason: "not in table" };
        if (parsed.ok) out.push(parsed.reading);
        else {
          missing.push(station.id);
          console.warn(`InfoBanjir ${station.id} rejected: ${parsed.reason}`);
        }
      }
    } catch (e) {
      errors.push(e instanceof Error ? e.message : String(e));
      missing.push(...stations.map((s) => s.id));
    }
  };
  collect(water, parseWaterLevelTable, RIVER_STATIONS, waterLevelReading, flood);
  collect(rain, parseRainfallTable, [RAIN_GAUGE], rainfallReading, rainfall);

  const a = await writeReadings(store, { source: "infobanjir", type: "flood_level" }, flood, false);
  const b = await writeReadings(store, { source: "infobanjir", type: "rainfall" }, rainfall, false);

  const ok = errors.length === 0 && missing.length === 0;
  const error = errors.length > 0 ? errors.join("; ") : missing.length > 0 ? `Stations missing: ${missing.join(", ")}` : null;
  return {
    ok,
    error,
    stationsMissing: missing,
    written: a.written + b.written,
    deleted: 0,
    unchanged: a.unchanged + b.unchanged,
  };
}
