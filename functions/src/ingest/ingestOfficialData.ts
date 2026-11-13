import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { onSchedule } from "firebase-functions/v2/scheduler";

import { runIngestion } from "./ingest";
import { firestoreStore } from "./store";
import { runAlertRules } from "../alerts/runAlertRules";

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
      const db = getFirestore();
      const summary = await runIngestion(firestoreStore(db));
      logger.info("ingestOfficialData", summary);

      // Then run the alert rules on the readings just saved (FR2.1)
      try {
        const alerts = await runAlertRules(db, new Date());
        logger.info("alertRules", { alerts });
      } catch (e) {
        logger.error("alertRules failed", e);
      }
    },
);
