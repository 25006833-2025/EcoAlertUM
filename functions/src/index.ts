import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions";

import { askAssistant } from "./assistant/askAssistant";
import { onReportCreated } from "./reports/onReportCreated";
import { ingestOfficialData } from "./ingest/ingestOfficialData";

initializeApp();

setGlobalOptions({ maxInstances: 10 });

export { askAssistant, onReportCreated, ingestOfficialData };