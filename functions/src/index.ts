import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions";

import { askAssistant } from "./assistant/askAssistant";
import { onReportCreated } from "./reports/onReportCreated";

initializeApp();

setGlobalOptions({ maxInstances: 10 });

export { askAssistant, onReportCreated };