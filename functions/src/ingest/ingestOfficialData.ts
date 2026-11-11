import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onSchedule } from "firebase-functions/v2/scheduler";

import { runIngestion } from "./ingest";

/**
 * Fetches official data into Firestore every 5 minutes (architecture.md, function E).
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
    const summary = await runIngestion(getFirestore());
    logger.info("ingestOfficialData", summary);
  },
);
