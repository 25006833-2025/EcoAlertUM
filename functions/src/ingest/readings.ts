import { DocumentData, GeoPoint, Timestamp } from "firebase-admin/firestore";

import { IngestStore, SOURCE_STATUS, StoredDoc } from "./store";

/**
 * Shared by every source adapter: the reading shape, the writer, sourceStatus, and small parsers.
 * Field names and values follow docs/data-models.md and mock/hazardReadings.json.
 */

/** Shared severity scale as stored in Firestore ("alert" is shown as Advisory in the app). */
export type Level = "normal" | "alert" | "warning" | "danger";
export type SourceId = "met" | "infobanjir" | "doe";
export type ReadingType = "weather_warning" | "flood_level" | "rainfall" | "air_quality";

export interface LocalizedText {
  en: string;
  ms: string;
}

/** JPS "Universiti Malaya" rain gauge position, used as the UM reference point. */
export const UM_LOCATION = new GeoPoint(3.127447, 101.650307);

/** Deterministic document ID: each fetch overwrites the same doc. */
export function readingId(source: SourceId, stationId: string, type: ReadingType): string {
  return `${source}_${stationId}_${type}`;
}

/** Result of turning one source row into a reading: either a reading or why it was rejected. */
export type Parsed<T> = { ok: true; reading: T } | { ok: false; reason: string };

/** Malaysia local time without a zone, "dd/mm/yyyy HH:mm" or "dd/mm/yyyy HH:mm:ss". */
export function parseMytDateTime(text: string | undefined): Date | null {
  const m = /^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})(?::(\d{2}))?$/.exec((text ?? "").trim());
  if (!m) return null;
  const [, dd, mm, yyyy, hh, min, ss] = m;
  const date = new Date(`${yyyy}-${mm}-${dd}T${hh}:${min}:${ss ?? "00"}+08:00`);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Readings older than this are rejected (docs/data-sources.md: 2x the source's update interval). */
export function isStale(observedAt: Date, now: Date, maxAgeMs: number): boolean {
  return now.getTime() - observedAt.getTime() > maxAgeMs;
}

/** A number from a table cell, or null for empty / non-numeric text. */
export function cellNumber(text: string | undefined): number | null {
  const cleaned = (text ?? "").replace(/,/g, "").trim();
  if (cleaned === "") return null;
  const n = Number(cleaned);
  return Number.isFinite(n) ? n : null;
}

/** JPS and others use -9999 for "no data". */
export function isSentinel(value: number): boolean {
  return value <= -9999;
}

// --- Writer ----------------------------------------------------------------------------------

export interface WriteResult {
  written: number;
  deleted: number;
  unchanged: number;
}

/** These change on every fetch without the reading itself changing. */
const VOLATILE_FIELDS = new Set(["fetchedAt", "observedAt", "previousValue"]);

/**
 * Writes readings for one source and type: new or changed docs are written, unchanged ones are
 * left alone (fewer writes and listener updates, architecture.md decision 2). `previousValue`
 * comes from the stored doc. With [deleteMissing], docs of this source and type that are not in
 * [readings] are deleted (used when the source's current list is authoritative, e.g. MET warnings).
 */
export async function writeReadings(
  store: IngestStore,
  scope: { source: SourceId; type: ReadingType },
  readings: StoredDoc[],
  deleteMissing: boolean,
): Promise<WriteResult> {
  const existing = await store.findReadings(scope.source, scope.type);
  const ids = new Set(readings.map((r) => r.id));

  const sets: StoredDoc[] = [];
  let unchanged = 0;
  for (const reading of readings) {
    const old = existing.get(reading.id);
    if (old && sameContent(old, reading.data)) {
      unchanged++;
    } else {
      sets.push({ id: reading.id, data: { ...reading.data, previousValue: previousValueFor(old, reading.data) } });
    }
  }
  const deletes = deleteMissing ? [...existing.keys()].filter((id) => !ids.has(id)) : [];

  if (sets.length + deletes.length > 0) await store.commitReadings(sets, deletes);
  return { written: sets.length, deleted: deletes.length, unchanged };
}

/** Same reading apart from the fields that change on every fetch. */
export function sameContent(old: DocumentData, next: DocumentData): boolean {
  const keys = new Set([...Object.keys(old), ...Object.keys(next)]);
  for (const key of keys) {
    if (VOLATILE_FIELDS.has(key)) continue;
    if (!deepEqual(old[key], next[key])) return false;
  }
  return true;
}

function previousValueFor(old: DocumentData | undefined, next: DocumentData): number | null {
  if (!old) return next.previousValue ?? null;
  if (old.value !== next.value) return old.value ?? null;
  return old.previousValue ?? null;
}

function deepEqual(a: unknown, b: unknown): boolean {
  if (a instanceof Timestamp || b instanceof Timestamp) {
    return a instanceof Timestamp && b instanceof Timestamp && a.isEqual(b);
  }
  if (a instanceof GeoPoint || b instanceof GeoPoint) {
    return a instanceof GeoPoint && b instanceof GeoPoint && a.isEqual(b);
  }
  if (Array.isArray(a) || Array.isArray(b)) {
    return Array.isArray(a) && Array.isArray(b) && a.length === b.length && a.every((v, i) => deepEqual(v, b[i]));
  }
  if (a && b && typeof a === "object" && typeof b === "object") {
    const ra = a as Record<string, unknown>;
    const rb = b as Record<string, unknown>;
    const keys = new Set([...Object.keys(ra), ...Object.keys(rb)]);
    return [...keys].every((k) => deepEqual(ra[k], rb[k]));
  }
  return a === b;
}

// --- sourceStatus ----------------------------------------------------------------------------

/** What one adapter run reports. [ok] false keeps `lastSuccessAt`, so the app shows saved data as outdated. */
export interface SourceRun extends WriteResult {
  ok: boolean;
  error: string | null;
  stationsMissing: string[];
}

/** Updates `sourceStatus/{source}` on every run, success or failure (docs/data-models.md). */
export async function recordStatus(store: IngestStore, source: SourceId, now: Date, run: Pick<SourceRun, "ok" | "error" | "stationsMissing">): Promise<void> {
  const at = Timestamp.fromDate(now);
  if (run.ok) {
    await store.setDoc(SOURCE_STATUS, source, {
      ok: true,
      lastSuccessAt: at,
      lastAttemptAt: at,
      error: null,
      stationsMissing: run.stationsMissing,
    });
  } else {
    // merge keeps lastSuccessAt, so the app can show "saved <time>".
    await store.setDoc(SOURCE_STATUS, source, {
      ok: false,
      lastAttemptAt: at,
      error: run.error,
      stationsMissing: run.stationsMissing,
    }, true);
  }
}

// --- HTTP ------------------------------------------------------------------------------------

/** Returns the response body as text. Adapters parse JSON or HTML themselves. */
export type HttpGet = (url: string) => Promise<string>;

const FETCH_TIMEOUT_MS = 10_000;

/** GET with a timeout and a generic User-Agent. Throws on HTTP errors so the source is marked down. */
export const httpGet: HttpGet = async (url) => {
  const response = await fetch(url, {
    headers: { "User-Agent": "EcoAlertUM-student-project" },
    signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status} from ${new URL(url).host}`);
  return response.text();
};
