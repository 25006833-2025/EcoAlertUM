import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onSchedule } from "firebase-functions/v2/scheduler";

import { AlertStore, firestoreAlertStore } from "./syncAlerts";

/** Marks active alerts past their end time as expired. Returns the IDs it expired. */
export async function expireOldAlerts(store: AlertStore, now: Date): Promise<string[]> {
  const active = await store.listActive();
  const ids = active.filter((a) => a.expiresAt.getTime() <= now.getTime()).map((a) => a.id);
  await store.markExpired(ids);
  return ids;
}

/** Runs every 15 minutes (architecture.md). Needs the Blaze plan, like ingestOfficialData. */
export const expireAlerts = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Asia/Kuala_Lumpur",
    timeoutSeconds: 60,
    retryCount: 0,
  },
  async () => {
    const expired = await expireOldAlerts(firestoreAlertStore(getFirestore()), new Date());
    logger.info("expireAlerts", { expired: expired.length });
  },
);