import { Timestamp } from "firebase-admin/firestore";

import { DOE_INTERVAL_MS, runDoe } from "./doe";
import { INFOBANJIR_INTERVAL_MS, runInfobanjir } from "./infobanjir";
import { MET_INTERVAL_MS, runMet } from "./met";
import { HttpGet, SourceId, SourceRun, httpGet, recordStatus } from "./readings";
import { IngestStore, SOURCE_STATUS } from "./store";

/** One official source: how often it is fetched and how to run it. */
export interface Adapter {
  source: SourceId;
  intervalMs: number;
  /** May return extra fields (e.g. MET's forecast result); they show up in the run summary. */
  run(store: IngestStore, now: Date, get: HttpGet): Promise<SourceRun>;
}

/** Per-source schedule (architecture.md decision 2): MET every 5 min, InfoBanjir and DOE every 15 min. */
export const ADAPTERS: Adapter[] = [
  { source: "met", intervalMs: MET_INTERVAL_MS, run: runMet },
  { source: "infobanjir", intervalMs: INFOBANJIR_INTERVAL_MS, run: runInfobanjir },
  { source: "doe", intervalMs: DOE_INTERVAL_MS, run: runDoe },
];

/** The scheduler fires every 5 min, a little after the mark; this keeps a 15-min source on 15 min, not 20. */
const DUE_SLACK_MS = 60_000;

export type SourceSummary = SourceRun | { skipped: "not due" };

export interface IngestOptions {
  /** Run every source now, ignoring the schedule (local runs). */
  force?: boolean;
  adapters?: Adapter[];
}

/**
 * One ingestion run. Sources run in parallel and in isolation: a failing source only marks
 * itself as down (`sourceStatus/{source}.ok = false`) and keeps its last readings
 * (architecture.md, Flow 2). The others are not affected.
 */
export async function runIngestion(
  store: IngestStore,
  now: Date = new Date(),
  get: HttpGet = httpGet,
  options: IngestOptions = {},
): Promise<Record<string, SourceSummary>> {
  const adapters = options.adapters ?? ADAPTERS;
  const results = await Promise.all(adapters.map((adapter) => runOne(adapter, store, now, get, options.force ?? false)));
  return Object.fromEntries(results);
}

async function runOne(adapter: Adapter, store: IngestStore, now: Date, get: HttpGet, force: boolean): Promise<[string, SourceSummary]> {
  try {
    if (!force && !(await isDue(store, adapter, now))) return [adapter.source, { skipped: "not due" }];
    const run = await adapter.run(store, now, get);
    await recordStatus(store, adapter.source, now, run);
    return [adapter.source, run];
  } catch (e) {
    const error = e instanceof Error ? e.message : String(e);
    console.error(`ingest ${adapter.source} failed`, e);
    const run: SourceRun = { ok: false, error, stationsMissing: [], written: 0, deleted: 0, unchanged: 0 };
    try {
      await recordStatus(store, adapter.source, now, run);
    } catch (statusError) {
      console.error(`ingest ${adapter.source}: could not record status`, statusError);
    }
    return [adapter.source, run];
  }
}

/** Due when the last attempt (success or failure) is at least one interval ago, or there is none. */
async function isDue(store: IngestStore, adapter: Adapter, now: Date): Promise<boolean> {
  const lastAttemptAt = (await store.getDoc(SOURCE_STATUS, adapter.source))?.lastAttemptAt;
  if (!(lastAttemptAt instanceof Timestamp)) return true;
  return now.getTime() - lastAttemptAt.toMillis() >= adapter.intervalMs - DUE_SLACK_MS;
}
