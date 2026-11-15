# Cloud Functions

Backend for EcoAlert UM. See [docs/architecture.md](../docs/architecture.md) for how the functions fit together.

| Function | Trigger | Code |
|---|---|---|
| `ingestOfficialData` | Every 5 min | `src/ingest/` |
| `askAssistant` | HTTPS callable | `src/assistant/` |
| `onReportCreated` | New `reports/{id}` | `src/reports/` |

## Official data ingestion (`src/ingest/`)

`ingestOfficialData` wakes every 5 minutes and fetches each source that is due. It writes `hazardReadings` and `sourceStatus` (fields in [docs/data-models.md](../docs/data-models.md)).

| Source | File | Every | Writes |
|---|---|---|---|
| MET Malaysia (data.gov.my) | `met.ts` | 5 min (forecast at most every 6 h) | `weather_warning` readings for KL / Petaling, `forecasts/Tn079` |
| JPS Public InfoBanjir | `infobanjir.ts` | 15 min | `flood_level` for Sg. Klang di Bukit Putra and Jalan Ang Seng, `rainfall` for the UM gauge |
| DOE APIMS | `doe.ts` | 15 min | `air_quality` for CA19B Petaling Jaya, or CA15W Batu Muda as fallback |

Shared code lives in `readings.ts` (writer, `sourceStatus`, parsers) and `store.ts` (database access). `ingest.ts` runs the sources.

How it behaves:
- A reading is written in full only when it changes. `previousValue` keeps the last stored value.
- An unchanged reading gets a partial update of `fetchedAt` only, so `fetchedAt` means "last time we confirmed this value from the source". A station that is rejected or missing from the page is not touched, so its `fetchedAt` stays old and it goes stale on its own while the other stations stay current.
- Bad values are rejected: `-9999`, missing, impossible zeros, and stale readings. The last good doc stays, and the station is listed in `sourceStatus/{source}.stationsMissing`.
  - Stale means older than 45 min for InfoBanjir (the page lags behind its 15-min updates) or 2 h for DOE.
  - A rain gauge reporting 0 for all 7 days is rejected only if it is on the known-faulty list (`KNOWN_FAULTY_GAUGES` in `infobanjir.ts`, currently Jalan Ang Seng). A dry week elsewhere is accepted.
- Each source is isolated. If one fails, only its `sourceStatus` shows `ok: false`, and the others still run.
- Requests use a 10 s timeout and the User-Agent `EcoAlertUM-student-project`.

Station positions and distances are kept by hand in `infobanjir.ts` and `doe.ts`, because the source pages don't include them.

## Run it locally

Scheduled functions don't run by themselves in the emulator, so use the one-off runner. It runs **every source once**, ignoring the schedule, against the **Firestore emulator only**. It refuses to start without `FIRESTORE_EMULATOR_HOST`, so it can't write to the real project.

```powershell
npm --prefix functions install
firebase emulators:exec --only firestore --project demo-ecoalert "npm --prefix functions run ingest:emulator"
```

This calls the real MET, InfoBanjir and DOE sites (4 requests in total) and prints a summary per source. To look at the data afterwards, start the emulators with `firebase emulators:start`. Then run `npm --prefix functions run ingest:emulator` in a second terminal, with `FIRESTORE_EMULATOR_HOST=127.0.0.1:8080` set, and open the Emulator UI at http://127.0.0.1:4000/firestore.

## Tests

```powershell
npm --prefix functions test
```

The tests use the saved real responses in [docs/samples/](../docs/samples/) and an in-memory store. They never call the live sites.

## Deploy

Scheduled functions and outbound HTTP need the Blaze plan.

```powershell
firebase deploy --only functions
```
