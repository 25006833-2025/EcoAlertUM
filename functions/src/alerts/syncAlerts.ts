import { DocumentData, Firestore, Timestamp } from "firebase-admin/firestore";

import { Level } from "../ingest/readings";
import { AlertCandidate, levelRank } from "./rules";

/**
 * Saves the rules' candidates to the `alerts` collection (docs/data-models.md, section 2).
 * Deduplication: never two active alerts with the same ruleId + area. An existing one is
 * updated instead, so students are not pushed again every 5 minutes.
 */

export const ALERTS = "alerts";

/** Always the whole campus in v1 (architecture.md decision 13). */
export const AREA = "whole_campus";

export interface ActiveAlert {
  id: string;
  level: Level;
}

/** The few database operations alerts need: Firestore in production, in memory in tests. */
export interface AlertStore {
  findActive(ruleId: string, area: string): Promise<ActiveAlert | undefined>;
  create(data: DocumentData): Promise<string>;
  update(id: string, data: DocumentData): Promise<void>;
}

export function firestoreAlertStore(db: Firestore): AlertStore {
  return {
    async findActive(ruleId, area) {
      const snapshot = await db
        .collection(ALERTS)
        .where("ruleId", "==", ruleId)
        .where("area", "==", area)
        .where("status", "==", "active")
        .limit(1)
        .get();
      const doc = snapshot.docs[0];
      return doc ? { id: doc.id, level: doc.get("level") as Level } : undefined;
    },
    async create(data) {
      return (await db.collection(ALERTS).add(data)).id;
    },
    async update(id, data) {
      await db.collection(ALERTS).doc(id).update(data);
    },
  };
}

/** What happened to one candidate. "new" and "escalated" alerts will be pushed (step 6). */
export interface SyncResult {
  ruleId: string;
  alertId: string;
  change: "new" | "escalated" | "updated";
}

export async function syncAlerts(store: AlertStore, candidates: AlertCandidate[], now: Date): Promise<SyncResult[]> {
  const results: SyncResult[] = [];

  for (const candidate of candidates) {
    const existing = await store.findActive(candidate.ruleId, AREA);

    // Fields that can change while an alert is active.
    const fields = {
      level: candidate.level,
      title: candidate.title,
      message: candidate.message,
      readingIds: candidate.readingIds,
      expiresAt: Timestamp.fromDate(candidate.expiresAt),
    };

    if (!existing) {
      const id = await store.create({
        ...fields,
        hazardType: candidate.hazardType,
        area: AREA,
        ruleId: candidate.ruleId,
        status: "active",
        issuedAt: Timestamp.fromDate(now),
      });
      results.push({ ruleId: candidate.ruleId, alertId: id, change: "new" });
    } else {
      await store.update(existing.id, fields);
      const escalated = levelRank(candidate.level) > levelRank(existing.level);
      results.push({ ruleId: candidate.ruleId, alertId: existing.id, change: escalated ? "escalated" : "updated" });
    }
  }

  return results;
}