import { Firestore, Timestamp } from "firebase-admin/firestore";

import { READINGS, SOURCE_STATUS } from "../ingest/store";
import { Reading, evaluateRules } from "./rules";
import { SyncResult, firestoreAlertStore, syncAlerts } from "./syncAlerts";

const MINUTE_MS = 60 * 1000;

/**
 * How old a reading may be before the rules ignore it: three missed fetches, the same limits
 * as the app's FreshnessPolicy (MET every 5 min, InfoBanjir and DOE every 15 min).
 */
export const MAX_AGE_MS: Record<string, number> = {
  met: 15 * MINUTE_MS,
  infobanjir: 45 * MINUTE_MS,
  doe: 45 * MINUTE_MS,
};

/** A saved reading plus the two fields needed to judge how current it is. */
export interface SavedReading extends Reading {
  source: string;
  fetchedAt: Date | null;
}

/**
 * Keeps only readings the rules should use:
 * - drops MET warnings that have already ended (validTo in the past);
 * - drops readings older than their source's limit, so a source that goes down cannot keep an
 *   old alert alive. "Last updated" is the later of the reading's fetchedAt and its source's
 *   lastSuccessAt, because ingestion does not rewrite a reading whose value is unchanged
 *   (same as Freshness.lastUpdatedOf in the app).
 */
export function usableReadings(
  readings: SavedReading[],
  lastSuccessAt: Map<string, Date>,
  now: Date,
): Reading[] {
  return readings.filter((r) => {
    if (r.validTo && r.validTo.toDate().getTime() <= now.getTime()) return false;

    const maxAge = MAX_AGE_MS[r.source];
    if (maxAge === undefined) return false;

    const times = [r.fetchedAt, lastSuccessAt.get(r.source)]
      .filter((d): d is Date => d instanceof Date)
      .map((d) => d.getTime());
    if (times.length === 0) return false;

    return now.getTime() - Math.max(...times) <= maxAge;
  });
}

/** A Firestore Timestamp field as a Date, or null when missing. */
function toDate(value: unknown): Date | null {
  return value instanceof Timestamp ? value.toDate() : null;
}

/**
 * Runs after each ingestion (architecture.md steps 5-6): reads hazardReadings and sourceStatus,
 * keeps current readings only, applies the alert rules and creates or updates alerts.
 */
export async function runAlertRules(db: Firestore, now: Date): Promise<SyncResult[]> {
  const [readingSnap, statusSnap] = await Promise.all([
    db.collection(READINGS).get(),
    db.collection(SOURCE_STATUS).get(),
  ]);

  const lastSuccessAt = new Map<string, Date>();
  for (const doc of statusSnap.docs) {
    const at = toDate(doc.get("lastSuccessAt"));
    if (at) lastSuccessAt.set(doc.id, at);
  }

  const saved: SavedReading[] = readingSnap.docs.map((doc) => ({
    id: doc.id,
    type: doc.get("type"),
    level: doc.get("level"),
    summary: doc.get("summary") ?? { en: "", ms: "" },
    validTo: doc.get("validTo") ?? null,
    source: doc.get("source"),
    fetchedAt: toDate(doc.get("fetchedAt")),
  }));

  const candidates = evaluateRules(usableReadings(saved, lastSuccessAt, now), now);
  return syncAlerts(firestoreAlertStore(db), candidates, now);
}