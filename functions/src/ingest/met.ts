import { GeoPoint, Timestamp } from "firebase-admin/firestore";

/**
 * MET Malaysia via data.gov.my: weather warnings and the 7-day forecast.
 * Field formats and quirks are documented in docs/data-sources.md section 1.
 * Everything here is pure (no network, no database) so it can be unit tested.
 */

export const MET_BASE_URL = "https://api.data.gov.my/weather";

/** JPS "Universiti Malaya" rain gauge position, used as the UM reference point. */
export const UM_LOCATION = new GeoPoint(3.127447, 101.650307);

/** MET forecast location nearest to UM (Kuala Lumpur town). */
export const FORECAST_LOCATION_ID = "Tn079";

/** Shared severity scale as stored in Firestore ("alert" is shown as Advisory in the app). */
export type Level = "normal" | "alert" | "warning" | "danger";

export interface MetWarning {
  warning_issue?: { issued?: string; title_en?: string; title_bm?: string };
  valid_from?: string | null;
  valid_to?: string | null;
  heading_en?: string;
  heading_bm?: string;
  text_en?: string;
  text_bm?: string;
}

export interface MetForecastDay {
  location?: { location_id?: string; location_name?: string };
  date?: string;
  morning_forecast?: string;
  afternoon_forecast?: string;
  night_forecast?: string;
  summary_forecast?: string;
  summary_when?: string;
  min_temp?: number;
  max_temp?: number;
}

export interface LocalizedText {
  en: string;
  ms: string;
}

/** One `hazardReadings` document (see docs/data-models.md). */
export interface WarningReading {
  id: string;
  data: {
    type: "weather_warning";
    source: "met";
    stationId: string;
    stationName: string;
    location: GeoPoint;
    stationDistanceKm: null;
    value: null;
    unit: null;
    previousValue: null;
    thresholds: null;
    level: Level;
    summary: LocalizedText;
    validFrom: Timestamp;
    validTo: Timestamp;
    observedAt: Timestamp;
    fetchedAt: Timestamp;
  };
}

/** MET times have no zone (e.g. "2026-10-23T20:30:00"); observed values are Malaysia time. */
export function parseMetTime(text: string | null | undefined): Date | null {
  if (!text) return null;
  const date = new Date(`${text}+08:00`);
  return Number.isNaN(date.getTime()) ? null : date;
}

const MARINE_OR_NONE = /strong winds and rough seas|angin kencang|laut bergelora|no advisory|tiada nasihat/i;
const LAND_HAZARD = /thunderstorm|heavy rain|continuous rain|ribut petir|hujan lebat|hujan berterusan/i;
const DANGER_LEVEL = /danger(ous)? level|tahap bahaya/i;
const SEVERE_LEVEL = /severe level|tahap buruk/i;

/** Which UM-area name the warning text mentions, if any. UM sits on the KL / Petaling border. */
export function umArea(warning: MetWarning): "Kuala Lumpur" | "Petaling" | null {
  const text = `${warning.text_en ?? ""} ${warning.text_bm ?? ""}`;
  if (/kuala lumpur/i.test(text)) return "Kuala Lumpur";
  if (/petaling/i.test(text)) return "Petaling";
  return null;
}

/**
 * Level for a warning that applies to UM, or null when it does not (marine, "No Advisory",
 * empty validity, no KL/Petaling mention). MET publishes no severity field, so land warnings
 * are "alert" unless the wording names a higher level (still unverified, see data-sources.md).
 */
export function warningLevel(warning: MetWarning): Level | null {
  const title = warning.warning_issue?.title_en ?? "";
  const words = `${title} ${warning.heading_en ?? ""} ${warning.text_en ?? ""}`;
  if (MARINE_OR_NONE.test(title)) return null;
  if (!warning.valid_from || !warning.valid_to || warning.valid_from === warning.valid_to) return null;
  if (!LAND_HAZARD.test(words) || umArea(warning) == null) return null;
  if (DANGER_LEVEL.test(words)) return "danger";
  if (SEVERE_LEVEL.test(words)) return "warning";
  return "alert";
}

/** "2026-10-23T15:30:00" + "Thunderstorms Warning" -> "20261023T1530_thunderstorms-warning". */
export function warningStationId(issued: string, titleEn: string): string {
  const time = issued.replace(/[-:]/g, "").slice(0, 13);
  const slug = titleEn.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
  return `${time}_${slug}`;
}

/** Turns one MET warning into a reading, or null when it does not apply to UM. */
export function warningToReading(warning: MetWarning, now: Date): WarningReading | null {
  const level = warningLevel(warning);
  const issued = warning.warning_issue?.issued;
  const issuedAt = parseMetTime(issued);
  const validFrom = parseMetTime(warning.valid_from);
  const validTo = parseMetTime(warning.valid_to);
  const area = umArea(warning);
  if (level == null || !issued || !issuedAt || !validFrom || !validTo || !area) return null;

  const titleEn = warning.warning_issue?.title_en?.trim() || "Weather warning";
  const titleMs = warning.warning_issue?.title_bm?.trim() || titleEn;
  const until = untilText(warning.valid_to!, issued);
  const stationId = warningStationId(issued, titleEn);

  return {
    id: `met_${stationId}_weather_warning`,
    data: {
      type: "weather_warning",
      source: "met",
      stationId,
      stationName: titleEn,
      location: UM_LOCATION,
      stationDistanceKm: null,
      value: null,
      unit: null,
      previousValue: null,
      thresholds: null,
      level,
      summary: {
        en: `${titleEn} for ${area} until ${until}`,
        ms: `${titleMs} bagi ${area} sehingga ${until}`,
      },
      validFrom: Timestamp.fromDate(validFrom),
      validTo: Timestamp.fromDate(validTo),
      observedAt: Timestamp.fromDate(issuedAt),
      fetchedAt: Timestamp.fromDate(now),
    },
  };
}

/** All warnings that apply to UM. Two records with the same ID keep the first. */
export function warningsToReadings(warnings: MetWarning[], now: Date): WarningReading[] {
  const byId = new Map<string, WarningReading>();
  for (const warning of warnings) {
    const reading = warningToReading(warning, now);
    if (reading && !byId.has(reading.id)) byId.set(reading.id, reading);
  }
  return [...byId.values()];
}

/** "19:00" on the issue day, otherwise "24/10 19:00". Text is MYT already. */
function untilText(validTo: string, issued: string): string {
  const time = validTo.slice(11, 16);
  if (validTo.slice(0, 10) === issued.slice(0, 10)) return time;
  return `${validTo.slice(8, 10)}/${validTo.slice(5, 7)} ${time}`;
}

// --- Forecast --------------------------------------------------------------------------------

/** MET forecast phrases (Malay only) with our English (docs/data-models.md, translations to confirm). */
const PHRASES: Record<string, string> = {
  "Tiada Hujan": "No rain",
  "Hujan": "Rain",
  "Hujan di beberapa tempat": "Rain in a few places",
  "Hujan di kebanyakan tempat": "Rain in most places",
  "Ribut petir": "Thunderstorms",
  "Ribut petir di beberapa tempat": "Thunderstorms in a few places",
  "Ribut petir di kebanyakan tempat": "Thunderstorms in most places",
  "Petang": "Afternoon",
  "Petang dan Malam": "Afternoon and night",
  "Sepanjang Hari": "All day",
  // Seen in live data on 12 Nov 2026, not yet in the docs table:
  "Pagi": "Morning",
  "Malam": "Night",
  "Pagi dan Petang": "Morning and afternoon",
  "Pagi dan Malam": "Morning and night",
};

/** Unknown phrases keep the Malay text in `en` so nothing is lost; [unknown] collects them for logging. */
export function translatePhrase(ms: string | undefined, unknown?: Set<string>): LocalizedText {
  const text = (ms ?? "").trim();
  const en = PHRASES[text];
  if (en === undefined && text !== "") unknown?.add(text);
  return { en: en ?? text, ms: text };
}

export interface ForecastDoc {
  id: string;
  data: {
    locationName: string;
    days: Array<{
      date: string;
      morning: LocalizedText;
      afternoon: LocalizedText;
      night: LocalizedText;
      summary: LocalizedText;
      summaryWhen: LocalizedText;
      minTemp: number | null;
      maxTemp: number | null;
    }>;
    fetchedAt: Timestamp;
  };
  unknownPhrases: string[];
}

/** The `forecasts/{locationId}` document, or null when MET returned no days for that location. */
export function forecastToDoc(days: MetForecastDay[], locationId: string, now: Date): ForecastDoc | null {
  const mine = days
    .filter((d) => d.location?.location_id === locationId && d.date)
    .sort((a, b) => a.date!.localeCompare(b.date!));
  if (mine.length === 0) return null;

  const unknown = new Set<string>();
  return {
    id: locationId,
    data: {
      locationName: mine[0].location?.location_name ?? locationId,
      days: mine.map((d) => ({
        date: d.date!,
        morning: translatePhrase(d.morning_forecast, unknown),
        afternoon: translatePhrase(d.afternoon_forecast, unknown),
        night: translatePhrase(d.night_forecast, unknown),
        summary: translatePhrase(d.summary_forecast, unknown),
        summaryWhen: translatePhrase(d.summary_when, unknown),
        minTemp: typeof d.min_temp === "number" ? d.min_temp : null,
        maxTemp: typeof d.max_temp === "number" ? d.max_temp : null,
      })),
      fetchedAt: Timestamp.fromDate(now),
    },
    unknownPhrases: [...unknown],
  };
}
