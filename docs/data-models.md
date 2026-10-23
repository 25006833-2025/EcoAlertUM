# EcoAlert UM — Data Models

Status: **draft v0.1** (planning, no app code yet). Sample data for every model lives in [`/mock`](../mock).

Stack assumed: Android (Kotlin + Jetpack Compose) · Firebase Auth · Cloud Firestore · Cloud Storage · Cloud Functions · FCM.
See [architecture.md](architecture.md) for how the pieces fit together.

## Conventions

| Convention | Rule | Why |
|---|---|---|
| Field names | `camelCase` | Matches Kotlin data classes, so Firestore's `toObject()` maps fields directly. |
| Document ID | Stored in Firestore as the doc ID, shown as `id` in mock JSON | Mock files are flat arrays that are easy to load in tests. |
| Timestamps | Firestore `Timestamp`; ISO 8601 with `+08:00` in mock JSON | Malaysia time, readable in mocks, sortable in queries. |
| Enums | Lowercase `snake_case` strings | Readable in the console and safe to extend. Unknown values map to `other`/`normal` in the app. |
| Location | `GeoPoint` (`{ "lat": .., "lng": .. }` in mock) | Native Firestore type, maps straight onto Google Maps `LatLng`. |
| Translated text | `LocalizedText` = `{ "en": string, "ms": string }` | Used only for system-written text (alerts, contacts). User-written text is stored as typed. |
| Optional fields | Marked `?`, stored as `null` | Keeps the shape the same in every document. |

### Shared severity scale: `Level`

Every source maps its own scale onto one four-step scale, so the dashboard, alert rules and map colours use the same logic.

| `Level` | MET Malaysia warning | InfoBanjir water level (per-station thresholds) | InfoBanjir rainfall, last 1 h (JPS bands) | DOE Air Pollutant Index (API) | UI colour |
|---|---|---|---|---|---|
| `normal` | no active land warning mentioning Kuala Lumpur or Petaling | below Waspada | 0–30 mm (Light / Moderate) | 0–100 (Good / Moderate) | green |
| `alert` | thunderstorm or heavy-rain warning that mentions Kuala Lumpur or Petaling | ≥ Waspada (Alert) | 31–60 mm (Heavy) | 101–200 (Unhealthy) | yellow |
| `warning` | **to confirm**: MET's wording for higher warning levels has not been observed yet | ≥ Amaran (Warning) | > 60 mm (Very Heavy) | 201–300 (Very Unhealthy) | orange |
| `danger` | **to confirm** (same reason) | ≥ Bahaya (Danger) | **to confirm**: proposed ≥ 60 mm over the last 3 h, from JPS's flash-flood guidance and our own stored history | > 300 (Hazardous) | red |

The backend does the mapping when it ingests data. The app only ever reads `level`. Sources and evidence for every band are in [data-sources.md](data-sources.md).

---

## 1. HazardReading — collection `hazardReadings`

The latest official reading for one station and one hazard type. Only the backend writes these. The app only reads them.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Deterministic: `{source}_{stationId}_{type}`, e.g. `infobanjir_0232121WL_flood_level`. Each fetch **overwrites** the same doc, so the collection stays small and re-runs are idempotent. For warnings, `stationId` is `{issued}_{title-slug}` (see below). |
| `type` | enum | `rainfall` · `flood_level` · `air_quality` · `weather_warning` |
| `source` | enum | `met` · `doe` · `infobanjir`. Fixed by type: `weather_warning` → `met`, `flood_level` and `rainfall` → `infobanjir` (MET's open API has no rain observations), `air_quality` → `doe` |
| `stationId` | string | ID used by the source, e.g. `0232121WL` (InfoBanjir F2 ID), `CA19B` (DOE). For `weather_warning`: issue time + title slug, e.g. `20261023T1530_thunderstorms-warning` |
| `stationName` | string | As published by the source, e.g. "Sg. Klang di Bukit Putra (F2)". For warnings: MET's `title_en` |
| `location` | GeoPoint | Station position. For `weather_warning`: the UM reference point (3.127447, 101.650307), because MET publishes no area geometry |
| `stationDistanceKm` | number? | Straight-line distance from the UM reference point, e.g. `4.6` for DOE `CA19B`. The UI shows "Petaling Jaya station, 4.6 km away". `null` for warnings |
| `value` | number? | Numeric reading. `null` for text-only warnings |
| `unit` | string? | `mm/h`, `m`, `API` |
| `previousValue` | number? | The value from our last stored reading. The app derives rising/falling trend from it, because InfoBanjir's tables have no trend column. `null` for warnings and first readings |
| `thresholds` | object? | `flood_level` only: `{ alert: number, warning: number, danger: number }`, from InfoBanjir's Waspada / Amaran / Bahaya columns, in metres. The UI can show "5.7 m below Alert". `null` for other types |
| `level` | Level | Normalised severity (table above) |
| `summary` | LocalizedText | Short display text, e.g. "Water level 18.96 m — Normal (Alert at 24.70 m)" |
| `validFrom` | Timestamp? | `weather_warning` only: MET's `valid_from`. `null` for other types |
| `validTo` | Timestamp? | `weather_warning` only: MET's `valid_to`. Alerts raised from this warning expire at this time. `null` for other types |
| `observedAt` | Timestamp | When the source measured or issued it |
| `fetchedAt` | Timestamp | When our backend stored it. The app shows "updated X min ago" and a stale banner if this is too old |

> Supporting doc (not one of the six models): `sourceStatus/{source}` with `{ ok: bool, lastSuccessAt, lastAttemptAt, error?, stationsMissing: string[] }`. The app uses it to show a "source unavailable — showing cached data" banner. `stationsMissing` lists station IDs whose latest reading was rejected or `null` (e.g. DOE `CA16W` Cheras), so the app can explain why it shows a farther station.

> Supporting doc (not one of the six models): `forecasts/{locationId}`, MET's 7-day forecast for one location (e.g. `Tn079` Kuala Lumpur). It is informational only and never triggers alert rules. See [mock/forecasts.json](../mock/forecasts.json).
>
> | Field | Type | Notes |
> |---|---|---|
> | `id` | string | MET `location_id`, e.g. `Tn079` |
> | `locationName` | string | MET `location_name`, e.g. "Kuala Lumpur" |
> | `days` | array | One entry per date: `{ date: string (YYYY-MM-DD), morning: LocalizedText, afternoon: LocalizedText, night: LocalizedText, summary: LocalizedText, summaryWhen: LocalizedText, minTemp: int, maxTemp: int }` |
> | `fetchedAt` | Timestamp | |
>
> MET publishes forecast text in Malay only. `ms` holds MET's original phrase. `en` comes from our phrase table below (**translations to confirm** with a BM speaker). These are all the phrases seen in the 23 Oct samples:
>
> | MET phrase (`ms`) | Our `en` |
> |---|---|
> | Tiada Hujan | No rain |
> | Hujan | Rain |
> | Hujan di beberapa tempat | Rain in a few places |
> | Hujan di kebanyakan tempat | Rain in most places |
> | Ribut petir | Thunderstorms |
> | Ribut petir di beberapa tempat | Thunderstorms in a few places |
> | Ribut petir di kebanyakan tempat | Thunderstorms in most places |
> | Petang | Afternoon |
> | Petang dan Malam | Afternoon and night |
> | Sepanjang Hari | All day |
>
> An unknown phrase is stored with `en` = the Malay text, and logged so we can add it.

## 2. Alert — collection `alerts`

A notification the rules engine raised from one or more readings. The backend writes it. The app reads it and FCM pushes it.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Auto ID |
| `hazardType` | enum | Same values as `HazardReading.type` |
| `level` | Level | Never `normal` |
| `title` | LocalizedText | Push notification title |
| `message` | LocalizedText | Body text with the action to take, e.g. "Avoid Jalan Universiti underpass" |
| `area` | string | Always `whole_campus` in v1, because official data is per district or station, not per building. The field is kept so zone values (e.g. `engineering`) can be added later. |
| `ruleId` | string | Which rule fired, e.g. `flood_level_alert` |
| `readingIds` | string[] | Readings that triggered it (for traceability) |
| `status` | enum | `active` · `expired` · `cancelled` |
| `issuedAt` | Timestamp | |
| `expiresAt` | Timestamp | The backend sets `expired` after this time. This stops stale alerts staying on screen. |

Deduplication rule: the job does **not** create a new alert while an `active` alert with the same `ruleId` + `area` exists. It updates the existing one instead. Without this rule, users would be pushed again every 5 minutes.

## 3. Report — collection `reports`

A student's hazard report. Photo files go to Cloud Storage at `reports/{reportId}/photo.jpg`.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Auto ID |
| `reporterUid` | string | Must equal `request.auth.uid` (security rule) |
| `category` | enum | Picked by the student: `flood` · `fallen_tree` · `landslide` · `blocked_drain` · `other` |
| `aiCategory` | enum? | Filled by the backend AI after creation. Same enum. `null` until processed |
| `aiConfidence` | number? | 0–1 |
| `description` | string | Max 500 chars |
| `photoPath` | string? | Storage path, not a download URL (URLs embed tokens that can expire or leak) |
| `location` | GeoPoint | From device GPS, adjustable on the map |
| `locationLabel` | string | Human-readable, e.g. "Near KK8 bus stop" |
| `status` | enum | `unverified` · `verified` · `rejected` · `resolved` |
| `confirmations` | int | Other students tapping "I see this too". This gives moderators a signal |
| `verifiedBy` | string? | Moderator UID |
| `verifiedAt` | Timestamp? | |
| `createdAt` | Timestamp | |

Students can only create reports with `status = unverified`. Only `moderator`/`admin` users can change `status`. `confirmations` support a moderator's decision, but they never change `status` on their own. For the demo, team members are moderators.

## 4. User — collection `users`, doc ID = Firebase Auth UID

| Field | Type | Notes |
|---|---|---|
| `id` | string | Auth UID |
| `email` | string | Must end in `@siswa.um.edu.my` and be verified. Sign-in is email/password in v1 |
| `displayName` | string | |
| `faculty` | string? | Optional. Useful for zone-based alerts later |
| `role` | enum | `student` · `staff` · `moderator` · `admin`. Users can **not** change their own role. It is set in the Firebase console (demo: team members = `moderator`). `staff` is reserved for when `@um.edu.my` is allowed |
| `language` | enum | `en` · `ms` |
| `notificationsEnabled` | bool | |
| `fcmTokens` | string[] | For direct pushes (e.g. "your report was verified"). Broadcast alerts use FCM topics instead |
| `badges` | string[] | Badge IDs, e.g. `flood_ready`. Badge definitions are bundled in the app |
| `createdAt` | Timestamp | |

## 5. QuizResult — collection `quizResults`

One document per attempt. The collection is top-level (not under `users/`) so we can run queries across all students, e.g. average pre vs post score per quiz for the SDG 13.1 impact evaluation.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Auto ID |
| `uid` | string | |
| `quizId` | string | e.g. `flood_safety_01`. Quiz and lesson content is bundled in the app for v1 |
| `phase` | enum | `pre` · `post` · `practice`. `pre`/`post` measure learning gain |
| `score` | int | Correct answers |
| `maxScore` | int | |
| `language` | enum | Language the quiz was taken in |
| `durationSeconds` | int | |
| `badgeAwarded` | string? | Badge ID unlocked by this attempt, if any |
| `completedAt` | Timestamp | |

## 6. EmergencyContact — collection `emergencyContacts`

Official contact directory managed by admins. Everyone can read it. The app caches it, so it works offline. There are no personal or user-added contacts.

| Field | Type | Notes |
|---|---|---|
| `id` | string | Readable slug, e.g. `um_security` |
| `name` | LocalizedText | |
| `category` | enum | `campus_security` · `medical` · `fire_rescue` · `police` · `civil_defence` · `facilities` |
| `phone` | string | E.164 (`+603...`) or short code (`999`) |
| `altPhone` | string? | |
| `available24h` | bool | |
| `hours` | string? | When not 24h, e.g. "Mon–Fri 08:00–17:00" |
| `location` | string? | Physical location on campus |
| `sortOrder` | int | Display order (999 first) |
| `sourceUrl` | string? | Official page the number was taken from (citation). Required for UM numbers |
| `sourceCheckedAt` | Timestamp? | When someone last confirmed the number on that page |

---

## Not modelled yet (on purpose)

- **Quiz and lesson content.** Bundled as JSON in the app for v1. It can move to Firestore later.
- **Alert rules.** Hard-coded thresholds in the backend for v1 (see architecture.md). They can move to an `alertRules` collection later.
- **Reading history.** Only the latest values are stored. If we want trend charts, we can add `hazardReadings/{id}/history` with a 7-day TTL.
- **AI chat history.** Not stored. Each assistant session lives on the device only, which avoids privacy questions.
