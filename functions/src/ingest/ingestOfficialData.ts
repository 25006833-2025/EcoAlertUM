import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onSchedule } from "firebase-functions/v2/scheduler";

import { runIngestion } from "./ingest";
import { firestoreStore } from "./store";

/**
 * Wakes every 5 minutes and fetches each source that is due (architecture.md, function E):
 * MET every run, InfoBanjir and DOE every 15 minutes.
 * Needs the Blaze plan: scheduled functions and outbound HTTP do not run on Spark.
 */
export const ingestOfficialData = onSchedule(
  {
    schedule: "every 5 minutes",
    timeZone: "Asia/Kuala_Lumpur",
    timeoutSeconds: 120,
    retryCount: 0, // the next run is 5 minutes away anyway
  },
  async () => {
    const summary = await runIngestion(firestoreStore(getFirestore()));
    logger.info("ingestOfficialData", summary);
  },
);
