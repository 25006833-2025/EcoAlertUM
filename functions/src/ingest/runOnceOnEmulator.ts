import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

import { runIngestion } from "./ingest";
import { firestoreStore } from "./store";

/**
 * Runs one ingestion against the local Firestore emulator (the functions emulator does not
 * fire scheduled functions by itself). Refuses to run against a real project.
 *
 *   firebase emulators:exec --only firestore "npm --prefix functions run ingest:emulator"
 */
async function main(): Promise<void> {
  if (!process.env.FIRESTORE_EMULATOR_HOST) {
    throw new Error("FIRESTORE_EMULATOR_HOST is not set. This script only runs against the emulator.");
  }
  initializeApp({ projectId: process.env.GCLOUD_PROJECT ?? "demo-ecoalert" });
  const summary = await runIngestion(firestoreStore(getFirestore()), new Date(), undefined, { force: true });
  console.log(JSON.stringify(summary, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
