import { Timestamp } from "firebase-admin/firestore";

import { Level, LocalizedText, ReadingType } from "../ingest/readings";

/**
 * FR2.1 alert rules (Module 2, Aliyah). Looks at the latest hazard readings and decides which
 * alerts should be active. One rule per hazard type, so at most one active alert per type.
 * Pure logic: no database here, which makes it easy to test.
 */

/** Severity order, lowest first. "alert" is shown as Advisory in the app. */
const LEVEL_ORDER: Level[] = ["normal", "alert", "warning", "danger"];

export function levelRank(level: Level): number {
  return LEVEL_ORDER.indexOf(level);
}

/** The lowest level that raises an alert: Advisory ("alert"). */
export const MIN_LEVEL: Level = "alert";

/** Flood, rain and air alerts stay active this long after the last high reading. */
export const HOLD_MS = 3 * 60 * 60 * 1000; // 3 hours

/** The fields of a hazardReadings doc that the rules need. */
export interface Reading {
  id: string;
  type: ReadingType;
  level: Level;
  summary: LocalizedText;
  validTo?: Timestamp | null;
}

/** An alert the rules say should exist right now. syncAlerts.ts saves it. */
export interface AlertCandidate {
  ruleId: string;
  hazardType: ReadingType;
  level: Level;
  title: LocalizedText;
  message: LocalizedText;
  readingIds: string[];
  expiresAt: Date;
}

/** Title and safety advice for each rule. English only for v1 (team decision). */
const RULES: Record<ReadingType, { title: string; advice: string }> = {
  weather_warning: {
    title: "Weather warning for the UM area",
    advice: "Use covered walkways and avoid open areas until the warning ends.",
  },
  flood_level: {
    title: "River level high near UM",
    advice: "Avoid low-lying paths and underpasses. Don't walk or drive through moving water.",
  },
  rainfall: {
    title: "Heavy rain at UM",
    advice: "Avoid flood-prone walkways and use covered routes between buildings.",
  },
  air_quality: {
    title: "Unhealthy air quality",
    advice: "Limit time outdoors and avoid outdoor sports. Wear a mask if you need to go out.",
  },
};

/** The order rules are checked in. */
export const RULE_TYPES: ReadingType[] = ["weather_warning", "flood_level", "rainfall", "air_quality"];

/**
 * Applies every rule to [readings]. For each hazard type with a reading at Advisory or above,
 * returns one candidate using the most severe reading.
 */
export function evaluateRules(readings: Reading[], now: Date): AlertCandidate[] {
  const candidates: AlertCandidate[] = [];

  for (const type of RULE_TYPES) {
    const high = readings.filter((r) => r.type === type && levelRank(r.level) >= levelRank(MIN_LEVEL));
    if (high.length === 0) continue; // nothing serious for this hazard

    // The most severe reading decides the alert's level and text.
    const worst = high.reduce((a, b) => (levelRank(b.level) > levelRank(a.level) ? b : a));
    const rule = RULES[type];

    candidates.push({
      ruleId: type,
      hazardType: type,
      level: worst.level,
      title: { en: rule.title, ms: "" },
      message: { en: `${worst.summary.en} ${rule.advice}`.trim(), ms: "" },
      readingIds: high.map((r) => r.id).sort(),
      expiresAt: expiryFor(type, high, now),
    });
  }

  return candidates;
}

/** Weather alerts end when MET's warning ends; the others are held for HOLD_MS. */
function expiryFor(type: ReadingType, high: Reading[], now: Date): Date {
  const hold = new Date(now.getTime() + HOLD_MS);
  if (type !== "weather_warning") return hold;

  const ends = high
    .map((r) => r.validTo?.toDate())
    .filter((d): d is Date => d instanceof Date);
  return ends.length > 0 ? new Date(Math.max(...ends.map((d) => d.getTime()))) : hold;
}