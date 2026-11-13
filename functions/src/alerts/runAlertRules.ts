import { Firestore } from "firebase-admin/firestore";

import { READINGS } from "../ingest/store";
import { Reading, evaluateRules } from "./rules";
import { SyncResult, firestoreAlertStore, syncAlerts } from "./syncAlerts";

/**
 * Runs after each ingestion (architecture.md steps 5-6): reads the latest hazardReadings,
 * applies the alert rules and creates or updates alerts (one active alert per rule).
 */
export async function runAlertRules(db: Firestore, now: Date): Promise<SyncResult[]> {
  const snapshot = await db.collection(READINGS).get();

  const readings: Reading[] = snapshot.docs
    .map((doc) => ({
      id: doc.id,
      type: doc.get("type"),
      level: doc.get("level"),
      summary: doc.get("summary") ?? { en: "", ms: "" },
      validTo: doc.get("validTo") ?? null,
    }))
    // Skip MET warnings that have already ended
    .filter((r) => !(r.validTo && r.validTo.toDate().getTime() <= now.getTime()));

  const candidates = evaluateRules(readings, now);
  return syncAlerts(firestoreAlertStore(db), candidates, now);
}