# EcoAlert UM — System Architecture

Status: **draft v0.1**. Data shapes are defined in [data-models.md](data-models.md).

## 1. Components

| # | Component | Technology | Responsibility |
|---|---|---|---|
| A | **Mobile app** | Android, Kotlin + Jetpack Compose | UI for all 6 modules. Reads Firestore (with offline cache), submits reports, uploads photos, receives pushes. Holds **no secrets**. |
| B | **Auth** | Firebase Authentication (email/password + email verification) | Login for `@siswa.um.edu.my` only. Issues the ID token used by Firestore rules and backend functions. No Google or Microsoft sign-in in v1, because that may need UM approval we won't get in time. |
| C | **Database** | Cloud Firestore | Stores `hazardReadings`, `sourceStatus`, `forecasts`, `alerts`, `reports`, `users`, `quizResults`, `emergencyContacts`. |
| D | **File storage** | Cloud Storage for Firebase | Report photos at `reports/{reportId}/photo.jpg`. |
| E | **Backend** | Cloud Functions for Firebase (Node.js/TypeScript) | Four functions, listed below. The only component that talks to official APIs or the AI API. |
| F | **Scheduler** | Cloud Scheduler (via `onSchedule`) | Triggers ingestion every 5 minutes. Each source is fetched only when its own interval is due (see `ingestOfficialData`). |
| G | **Secrets** | Google Secret Manager (via `defineSecret`) | Holds the AI API key and any official-API tokens. |
| H | **Official data sources** | MET Malaysia via data.gov.my (weather warnings, 7-day forecast), DOE MyEQMS (Air Pollutant Index), JPS InfoBanjir (river levels, rainfall) | External. Read-only. Owned by the government. Endpoints and limits are verified in [data-sources.md](data-sources.md). |
| I | **AI API** | Cloud LLM API. **Provider is C's decision.** Fixed constraint: it is called only from Cloud Functions, and the key lives in Secret Manager | Explains hazards, answers questions, categorises reports. |
| J | **Push service** | Firebase Cloud Messaging (FCM) | Delivers alerts to devices. |

### Backend functions (inside E)

| Function | Trigger | What it does |
|---|---|---|
| `ingestOfficialData` | Every 5 min (F) | 1. Fetches each source that is **due** on its own schedule, in parallel, each with a timeout: MET warnings every 5 min, MET forecast every 6 h, InfoBanjir every 15 min (2 requests: water level + rainfall), DOE every 15 min (1 query narrowed to the nearest stations). That totals about 580 requests a day. 2. Normalises them to `HazardReading` with the shared `Level` scale. 3. Upserts `hazardReadings/{id}` **only if the value changed**. 4. Updates `sourceStatus/{source}`. 5. Runs the alert rules. 6. Creates or updates `alerts` (with dedup). 7. Sends FCM pushes for new or escalated alerts. |
| `expireAlerts` | Every 15 min | Sets `status = expired` on alerts past `expiresAt`. |
| `askAssistant` | HTTPS callable (requires auth) | Receives the user's question. Adds the current readings and active alerts as context. Calls the AI API with the key from G. Returns the answer. Rate-limited per UID. |
| `onReportCreated` | Firestore `onDocumentCreated(reports/{id})` | Sends the photo and description to the AI API. Writes `aiCategory` and `aiConfidence`. Can also flag spam or inappropriate photos for moderators. |

## 2. Diagram description (boxes and arrows)

Draw three zones. Arrow labels are in quotes.

**Zone 1: Student's phone**
- [Android App]

**Zone 2: Firebase / Google Cloud (our backend)**
- [Firebase Auth]
- [Cloud Firestore]
- [Cloud Storage]
- [Cloud Functions]: a group box containing `ingestOfficialData`, `expireAlerts`, `askAssistant`, `onReportCreated`
- [Cloud Scheduler]
- [Secret Manager]
- [FCM]

**Zone 3: External services**
- [MET Malaysia]
- [DOE Air Quality]
- [JPS InfoBanjir]
- [AI API]

**Arrows**

1. Android App → Firebase Auth: "sign in (@siswa.um.edu.my email + password)"
2. Android App ⇄ Cloud Firestore: "read readings / alerts / contacts (live + offline cache); write reports, quiz results, profile"
3. Android App → Cloud Storage: "upload report photo"
4. Android App → `askAssistant`: "question + ID token"
5. Cloud Scheduler → `ingestOfficialData`: "every 5 min"
6. `ingestOfficialData` → MET Malaysia / DOE / InfoBanjir: "HTTPS fetch"
7. `ingestOfficialData` → Cloud Firestore: "upsert hazardReadings, sourceStatus, alerts"
8. `ingestOfficialData` → FCM: "send to topic"
9. FCM → Android App: "push alert"
10. Cloud Firestore → `onReportCreated`: "new report trigger"
11. `onReportCreated` → AI API: "categorise photo + text"
12. `askAssistant` → AI API: "prompt + live hazard context"
13. Secret Manager → `askAssistant`, `onReportCreated`: "AI API key"
14. `onReportCreated` → Cloud Firestore: "write aiCategory"

Visual tip: draw the **official sources and the AI API reachable only from Cloud Functions**. No arrow goes from the app straight to Zone 3. This one picture explains both the caching decision and the API-key decision.

## 3. Key flows

**Flow 1: Official data becomes a push alert**
1. Scheduler fires `ingestOfficialData`.
2. The function fetches InfoBanjir and finds Sg. Klang di Bukit Putra (F2), `0232121WL`, at 25.12 m. That is at or above the station's Waspada threshold of 24.70 m, so it maps to `level = alert` and upserts the reading.
3. Rule `flood_level_alert` matches. No active alert has the same `ruleId` and `area`, so the function creates one.
4. FCM sends to topics `alerts_en` and `alerts_ms`, each with text in its own language. The phone shows a notification.
5. The app's Firestore listener updates the dashboard and alert list.

**Flow 2: A source goes down**
1. The DOE fetch times out.
2. `sourceStatus/doe` is set to `{ ok: false, lastSuccessAt: <old time> }`. The old `hazardReadings` doc is **left in place**.
3. The app still shows the last AQI with "Updated 47 min ago · DOE unavailable".
4. Other sources are not affected, because each fetch is isolated.

**Flow 3: A student reports a hazard**
1. The app uploads the photo to Storage. Then it creates `reports/{id}` with `status = unverified`.
2. `onReportCreated` calls the AI API and fills in `aiCategory`.
3. The report shows on the map as "Unverified" (grey pin).
4. Other students tap "I see this too", which increments `confirmations`.
5. A moderator verifies the report. The pin turns coloured, and the reporter gets a direct push (via `fcmTokens`) plus a badge. For the demo, moderators are team members whose `role` was set to `moderator` in the Firebase console.

**Flow 4: A student asks the AI assistant**
1. The app calls `askAssistant` with the question and the user's language.
2. The function checks the rate limit, loads active alerts and the latest readings, and builds a prompt. The prompt includes a safety instruction: always defer to official warnings and 999 in emergencies.
3. The function calls the AI API and returns the answer. The key never leaves the server.

## 4. Security rules (summary)

| Collection | Read | Write |
|---|---|---|
| `hazardReadings`, `sourceStatus`, `alerts` | Signed-in, verified UM email | Backend only. The Admin SDK bypasses rules, so no client write rule is needed. |
| `emergencyContacts` | Signed-in | Admins only |
| `reports` | Signed-in | Create: `reporterUid == auth.uid` and `status == 'unverified'`. Update: moderators may change `status`. Any user may increment `confirmations`, and only by 1. |
| `users/{uid}` | Owner, plus moderators/admins | Owner, but **not** `role` or `badges` |
| `quizResults` | Owner, plus admins (for analysis) | Create by owner only. Never updated. |
| Storage `reports/**` | Signed-in | Signed-in. Images only, under 5 MB. |

UM-email check, used in every rule:
`request.auth.token.email_verified && request.auth.token.email.matches('.*@siswa\\.um\\.edu\\.my$')`

The app also checks the domain at sign-up, so users get a clear error message. That check is only for usability. The rule above is what actually enforces it. Staff (`@um.edu.my`) can be added later by widening the pattern.

---

## 5. Design decisions and how to defend them

**1. A backend job fetches official data. The app reads our stored copy.**
- *Resilience:* Government sites go down or slow down during floods, exactly when people need them. The last good reading stays in Firestore, and the app labels its age honestly.
- *Load and fair use:* About 580 requests a day across all sources, whatever the number of users, instead of several calls × every user × every app open. MET's open API allows only 4 requests per minute, and InfoBanjir and DOE have no public API at all. We avoid hitting rate limits or getting blocked.
- *Normalisation in one place:* Three sources with three formats and three severity scales become one `Level` scale. If a source changes its format, we fix one function, with no app update needed.
- *Enables alerts:* Rules need a server watching the data even when no one has the app open. The job that ingests the data is the natural place to evaluate the rules.

**2. Why a different interval per source?** Each source updates at its own verified rate. MET warnings are issued "when required", so we check every 5 minutes, using 1 of the 4 requests per minute allowed. The MET forecast updates daily, so every 6 hours is enough. InfoBanjir updates every 15 minutes, and DOE hourly, so we poll both every 15 minutes. Polling faster than a source updates adds load on government sites and gives us nothing new. The scheduler still wakes every 5 minutes (8,640 runs a month, inside the free tier) and fetches only the sources that are due. We write only when a value changes, which keeps Firestore writes and listener traffic low.

**3. The AI key stays on the backend.** Anything shipped in an APK can be extracted by decompiling it. A leaked key means someone else's usage on our bill. Keeping the key server-side also lets us add rate limiting per student, a fixed safety prompt, and live hazard context. If we switch AI provider later, only the backend changes.

**4. Firebase rather than a custom server.** The SDK is already in the project, and the team has no ops capacity. Firebase gives us auth, a realtime database, an offline cache, file storage, push and scheduled jobs from one console. For a semester project this means no servers to patch. The trade-offs: vendor lock-in, and Firestore's limited query power. Both are acceptable at this scale.

**5. Firestore offline cache is the second cache layer.** The Android SDK keeps the last-read documents on the device. With no signal (common in basements during a storm), the dashboard, alerts and emergency contacts still open. Two layers: the server cache covers the source being down, and the device cache covers the phone being offline.

**6. FCM topics for broadcasts, tokens for one-to-one pushes.** Campus-wide alerts go to topics (`alerts_en`, `alerts_ms`). That is one send per language and scales to any number of users. The app subscribes to the topic that matches the user's language setting. Direct tokens are only for personal messages such as "your report was verified".

**7. One shared `Level` scale.** MET, InfoBanjir and DOE each use different wording and thresholds. Mapping them all to `normal / alert / warning / danger` gives the UI one colour code and the rules engine one comparison. The mapping table also makes a good presentation slide.

**8. Reports start unverified. Moderators verify. AI only assists.** This prevents false reports, pranks and panic. The AI category is a *suggestion* that speeds up triage, never the final decision. That matters for accountability and is easy to defend ethically. Community `confirmations` are a supporting signal only. They never verify a report on their own. For the demo, team members act as moderators through the `role` flag. After the project, verification would move to a UM unit such as JPPHB (see section 6).

**9. Deterministic reading IDs and latest-only storage.** `{source}_{station}_{type}` means each run overwrites the same doc. Retries are idempotent, the collection never grows, and the app needs no queries, just a fixed list of docs to listen to. History can be added later if we want trend charts.

**10. Top-level `quizResults` with a `phase` field.** Pre/post scores are our evidence for SDG 13.1 (awareness and capacity building). A top-level collection lets us compute average learning gain across all students with a single query.

**11. Login with UM student email and password.** Only verified `@siswa.um.edu.my` accounts can sign in. This keeps reports accountable and limits abuse. The check is enforced in **security rules**, not only in the app UI, because UI checks can be bypassed. We chose email/password with a verification email over Google or Microsoft sign-in. Sign-in through UM's identity provider would likely need approval from UM IT, which we can't get within the semester. Email/password needs nobody's permission and still proves the student owns the inbox.

**13. Alerts are campus-wide only in v1.** Official data is published per district or per station, not per campus building. Splitting it into campus zones would mean guessing, and a guessed "your zone is safe" is worse than no zone at all. The `area` field is always `whole_campus` for now. It stays in the model so zone alerts can be added later, if finer data becomes available (e.g. campus rain gauges or verified student reports).

**12. Bilingual by design.** System text (alerts, contacts, summaries) is stored as `{en, ms}`, so one alert serves both languages. User-written text is stored as typed. UI strings use Android `strings.xml` / `values-ms/strings.xml`.

## 6. Risks and open items

- **Firebase Blaze plan required.** Scheduled functions and outbound HTTP calls from Functions do not work on the free Spark plan. Blaze still includes the free quota. Set a budget alert of around RM 20 or less.
- **Source availability (verified 23 Oct 2026, see [data-sources.md](data-sources.md)).**
  - MET warnings and forecast are open APIs via data.gov.my (`api.data.gov.my/weather/...`).
  - data.gov.my's `/flood-warning` endpoint is **not live**: it has been frozen since Feb 2024. Use it only as a station/coordinate list.
  - River levels and rainfall come from InfoBanjir's public pages, which have no API.
  - Air quality comes from DOE MyEQMS's undocumented map service.
  - Both of those can break without notice, so build `ingestOfficialData` with one adapter per source so a fallback can be swapped in.
  - Reuse terms for InfoBanjir and DOE are **to confirm** with JPS and DOE.
- **Station choice.** Nearest stations to UM, from data-sources.md:
  - Rainfall: Universiti Malaya (F2) `0232231RF`
  - River level: Sg. Klang di Bukit Putra (F2) `0232121WL` and Sg. Klang di Jalan Ang Seng (F2) `0232131WL`
  - Air quality: DOE `CA19B` Petaling Jaya, with `CA15W` Batu Muda as fallback
  - Forecast: MET `Tn079` Kuala Lumpur

  Hand-check each station's coordinates before release (**to confirm**).
- **AI safety.** The assistant must never contradict an official warning. It should always point to 999/994 for emergencies. Answers should be labelled as AI-generated.

## 7. Decision log and action items

| Topic | Decision | Owner | Action |
|---|---|---|---|
| Login | Email/password + verification email, `@siswa.um.edu.my` only | — | Test whether a student account can sign in with Google or Microsoft. This is for information only. The v1 login method does not change. |
| Emergency contacts | Admin-managed directory only. No personal contacts | F | Replace the placeholder UM numbers with real numbers from UM's official website. Fill in `sourceUrl` and `sourceCheckedAt` for each contact. |
| Report verification | Team members are moderators for the demo (`role` flag). Community confirmations are a supporting signal only | — | Name JPPHB (or another suitable UM unit) as the future verifier in the adoption plan. |
| AI provider | C chooses | C | Choose a provider and model. Constraint: the key stays in a Cloud Function (Secret Manager). The app never calls the AI API directly. |
| Alert targeting | Campus-wide only in v1. `area = whole_campus` | — | None. The field stays for later. |

### Adoption plan (post-project)

- **Report verification** moves from team members to a UM unit such as **JPPHB**. Their staff would get the `moderator` role, and the login pattern would be widened to `@um.edu.my` for them.
- **Emergency contacts** would be maintained by the same unit or by UM Security, using the `admin` role.
