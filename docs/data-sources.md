# EcoAlert UM — Official Data Source Verification

Status: **verified 23 Oct 2026, 20:55–21:15 MYT** by real requests, and spot-checked by hand against the official pages (see sections 2 and 3). Raw responses are in [samples/](samples/). This doc feeds `ingestOfficialData` in [architecture.md](architecture.md) and the `HazardReading` model in [data-models.md](data-models.md).

Legend: ✅ verified by a real request or by reading the official page · ⚠️ observed, but behaviour is undocumented · ❌ could not verify

UM reference point: **3.127447, 101.650307**. These are JPS's own coordinates for its "Universiti Malaya" rain gauge (station 26384). All distances below are straight-line from this point.

## Summary

| | MET Malaysia (forecast + warnings) | InfoBanjir / JPS (river levels + rainfall) | DOE (Air Pollutant Index) |
|---|---|---|---|
| **Use this endpoint** | `api.data.gov.my/weather/warning/` and `/weather/forecast/` (open government API) ✅ | `publicinfobanjir.water.gov.my` HTML fragments that the public page loads ⚠️ | `eqms.doe.gov.my/api3/publicmapproxy/.../CAQM_MCAQM_Current_Reading/MapServer/0/query` (ArcGIS) ⚠️ |
| **Official public API?** | **Yes**, documented ✅ | **No**. Only web pages ✅ | **No documented API**. The JSON service is used internally by DOE's map ✅ |
| **Registration / token** | None needed ✅ (MET's own `api.met.gov.my` needs a free token. We don't need it for v1) | None ✅ | None ✅ |
| **Format** | JSON ✅ | HTML table (malformed: missing `<tr>` tags) ✅ | ArcGIS JSON ✅ |
| **Update frequency** | Forecast: daily. Warnings: "when required" (official docs) ✅ | Every **15 min** (observed timestamps 20:30/20:45/21:00) ✅ | **Hourly** (observed) ✅ |
| **Rate limit** | **4 requests/min** (official docs) ✅ | None published ❌ | None published ❌ |
| **Terms** | Terms of Use link on the docs site is a placeholder (`#`) ❌ | Disclaimer only: no liability. No reuse terms ✅ | No terms found. Copyright line only ❌ |
| **Nearest to UM** | Forecast: `Tn079` Kuala Lumpur, `Tn076` Petaling Jaya. Warnings: free-text mention of "Kuala Lumpur" / "Petaling" ✅ | River: **Sg. Klang di Bukit Putra (F2)** 3.2 km, **Sg. Klang di Jalan Ang Seng (F2)** 3.2 km. Rain: **Universiti Malaya (F2)** on campus ✅ | **CA19B Petaling Jaya**, 4.6 km. **CA15W Batu Muda**, 10.1 km ✅ |
| **Biggest risk** | Warnings have **no structured area or severity**. They are free text | Page scraping can break when the site changes. No coordinates | Undocumented. **26 of 68 stations had no reading.** Timestamp quirk |

**Headline finding for the presentation:** our backend-cache design is the most practical option. Two of the three sources have no public API, the third allows only 4 requests per minute, and the "official" flood API on data.gov.my has been frozen since Feb 2024. A phone app calling these sources directly would break, get rate-limited, or show 2024 flood data as current.

---

## 1. MET Malaysia: forecast and warnings

### What we verified ✅
- **Endpoints** (no token, HTTP 200):
  - `GET https://api.data.gov.my/weather/warning/`
  - `GET https://api.data.gov.my/weather/forecast/`
  - Docs: <https://developer.data.gov.my/realtime-api/weather>
- **Trailing slash:** without it, the server returns `301` to the slashed URL. Use the slashed URL or follow redirects.
- **Filtering:** `?contains=<value>@<field>` (nested fields use `__`), `?limit=`, `?timestamp_start=`. For example, `?contains=Kuala Lumpur@location__location_name`.
- **Rate limit:** the official page <https://developer.data.gov.my/rate-limit> says, verbatim, "Weather — 4 requests per minute". Going over returns `429 Too Many Requests`. No rate-limit headers were returned. The docs mention no token.
- **Update frequency:** the docs say the forecast is "updated daily" and warnings are "updated when required". The sample forecast covers 23–29 Oct 2026, so it is current.
- **Forecast fields:** `location.location_id`, `location.location_name`, `date`, `morning_forecast`, `afternoon_forecast`, `night_forecast`, `summary_forecast`, `summary_when`, `min_temp`, `max_temp`.
  - The text is **Malay only**, e.g. "Ribut petir di beberapa tempat".
- **Forecast locations near UM:**
  - `St009` WP Kuala Lumpur (state)
  - `Ds058` Kuala Lumpur (district)
  - `Tn079` Kuala Lumpur (town)
  - `Ds057` Petaling (district)
  - `Tn076` Petaling Jaya (town)
- **Warning fields:** `warning_issue.{issued, title_en, title_bm}`, `valid_from`, `valid_to`, `heading_en/bm`, `text_en/bm`, `instruction_en/bm`.
  - Bilingual. This fits our `LocalizedText`.
- **Real warnings at 20:55 MYT:** 5 records.
  - 3 × "Strong Winds and Rough Seas" (marine)
  - 1 × "Thunderstorms Warning" for Perak, Kelantan, Terengganu, Pahang, Selangor (Kuala Selangor, Hulu Selangor, Sabak Bernam), Sarawak and Sabah. KL was not included.
  - 1 × "No Advisory" (tropical cyclone status)
- **MET's own API** `api.met.gov.my`:
  - It returns `401 {"detail":"Authentication credentials were not provided."}` without a token.
  - Its homepage says, verbatim: "You can make up to 1,000 requests a day with a burstable limit of 3 requests per minute".
  - Registration is name + email, with the token sent by email.

### Problems and things likely to break ⚠️
- **No structured area.** Affected areas only appear inside `text_en` as a list of states, each with districts in brackets. To decide whether a warning applies to UM, we must text-match "Kuala Lumpur", "W.P. Kuala Lumpur" or "Petaling" (UM sits on the KL/Petaling border).
- **No severity field.** None of today's 5 warnings had a level (Waspada/Buruk/Bahaya). Severity is only implied by the title and the "First/Second Category" wording in marine warnings.
- **Timestamps have no timezone** (`"2026-10-23T20:30:00"`). Observed values match MYT, so treat them as `+08:00`.
- **Odd records:** one marine warning had `valid_from == valid_to == 2026-10-23T00:00:00`. "No Advisory" records have `valid_from: null`. Treat both as non-hazards.
- **Forecast is Malay-only** and categorical (no rainfall amount). We need our own `ms → en` phrase table.
- **No live rainfall observations** in the open API. Our mock `met_48647_rainfall` reading does not exist in reality. Rainfall comes from InfoBanjir (§2).

### Could not verify ❌
- Wording of MET's **land** warnings with severity (e.g. continuous-rain levels). None were active, and the docs don't list categories. Our data-models.md table lists "Waspada / Buruk / Bahaya" for MET. **That is unverified.** Re-check when a KL rain warning is active.
- Whether the 4 requests/min limit is per IP or global, and whether a token raises it. The page doesn't say.
- data.gov.my terms of use: the footer link goes to `#`.
- What `api.met.gov.my` returns with a token. We didn't register.

## 2. InfoBanjir (JPS): river water levels and rainfall

### What we verified ✅
- **No public API.** The portal is a WordPress site. Each state page loads its data table via JavaScript from these fragments:
  - Water level: `GET /index.php/aras-air/data-paras-air/aras-air-data/?state=WLH&district=Kuala Lumpur&station=ALL`
  - Rainfall: `GET /wp-content/themes/shapely/agency/searchresultrainfall.php?state=WLH&district=Kuala Lumpur&station=ALL&loginStatus=0&language=1`
  - State codes come from the page: `WLH` = WP Kuala Lumpur, `SEL` = Selangor.
- **robots.txt** only disallows `/wp-admin/`. The data pages are not disallowed.
- **Disclaimer** (<https://publicinfobanjir.water.gov.my/penafian/>): the government and JPS accept no liability for losses from using the information. There are no reuse or automated-access terms.
- **No registration or token needed.** Responses carry `Cache-Control: no-cache, no-store`.
- **Update frequency: 15 min.** At 21:05 MYT most stations showed `23/10/2026 20:45`.
- **Water-level columns:** Station ID, Station name, District, Basin, Sub-basin, Last updated (`dd/mm/yyyy HH:mm`), Water level (m), then the four thresholds **Normal · Waspada (Alert) · Amaran (Warning) · Bahaya (Danger)**.
  - Each station has its own thresholds, so severity can be computed exactly.
- **Official meaning of the levels**, from the page legend:
  - Warning: "River level increasing to near flooding level & prepare for any evacuation action"
  - Danger: "River level caused considerable flooding, evacuation to be initiated"
- **Rainfall columns:** 6 daily totals (previous 6 days), Rainfall from midnight, **Total 1 Hour (Now)**. The order was confirmed from the `colspan`/`rowspan` header markup.
- **Rainfall intensity bands** from the JPS page legend, per hour:
  - Light 1–10 mm
  - Moderate 11–30 mm
  - Heavy 31–60 mm
  - Very Heavy >60 mm
  - Plus: "Convective rain more than 60 mm in 2 to 4 hours duration (typical) may cause flash floods".
- **Nearest stations to UM.** Coordinates come from the data.gov.my station list (§2a), joined through the legacy ID in each row's graph link.

| Station | ID | Distance | 20:45 reading | Alert / Warning / Danger |
|---|---|---|---|---|
| Rain gauge **Universiti Malaya (F2)** | `0232231RF` (legacy 26384) | on campus | 0.0 mm last hour, 13.0 mm since midnight | n/a |
| Sg. Klang di Bukit Putra (F2) | `0232121WL` (legacy 26413) | 3.2 km | 18.96 m | 24.70 / 26.20 / 26.70 |
| Sg. Klang di Jalan Ang Seng (F2) | `0232131WL` (legacy 26350) | 3.2 km | 19.69 m | 23.50 / 24.50 / 25.60 |
| Sg. Klang di Jambatan Petaling | `3016401` and `0231641WL` (F2) | ≈ 5.4 km* | 12.95 m | 16.81 / 17.81 / 18.81 |

\* Coordinates are taken from a data.gov.my record with the same name but a different ID (`3016002_`). Confirm the station's position before relying on this.

**Spot-checked by hand, 23 Oct 2026, 21:18 MYT ✅:** site Sg. Klang di Bukit Putra (F2), legacy page ID 26413. This one site has two gauges:
- Rain gauge `0232121RF`: the saved sample's 5.0 mm since midnight matches the live rainfall graph.
- Water-level gauge `0232121WL`: 18.96 m (Normal 18.00, Alert 24.70) matches the saved sample.

### Problems and things likely to break ⚠️
- **Scraping HTML is fragile.** The table HTML is malformed (rows lack opening `<tr>`), and any site redesign breaks the parser. Parse by `<td>` count, and alert ourselves when the column count changes.
- **No coordinates in the tables.** Keep a hand-checked station list (ID → lat/lng) in the backend config.
- **Two IDs per site.** Old IDs (`3116435`) and new "F2" IDs (`0232121WL`) appear side by side, often as duplicate rows for the same place. Pick one per site.
- **Bad values appear as real numbers:**
  - `-9999.0` (Kolam Takungan Batu)
  - `0.00` m (Sg. Bunus di Jalan Tun Razak (F2))
  - all-zero rain for 7 days at Jalan Ang Seng (F2) while neighbours recorded rain, so likely a faulty gauge
  - stations whose last update was 12 h old (two at 09:00)

  Reject these values. Never turn them into "normal".
- **Local time only** (`dd/mm/yyyy HH:mm`, no timezone). Treat as `+08:00`.
- **Terms are silent on automated access.** Poll gently: 2 requests per 15 min, with a descriptive User-Agent. Email JPS to ask for permission or an API (see "Registration and permissions").

### 2a. data.gov.my `/flood-warning`: do not use for live data
- `GET https://api.data.gov.my/flood-warning/` returns HTTP 200 with 1,276 stations as clean JSON with coordinates. ✅
- **Every timestamp is on or before `2024-02-23 14:45`**, and 725 records have none. The data is **frozen**. ✅
- It is listed in the changelog (v1.0.0, 2023-09-13), but it **no longer appears in the docs navigation**. ✅
- **Data quality problems:**
  - 458 stations have `warning_level < alert_level` (inconsistent thresholds)
  - one station has latitude `31.78`
  - sentinel values of `-9999`
- **Still useful as a static station/coordinate list** to join with InfoBanjir, after hand-checking the stations we use.

### Could not verify ❌
- Rate limits or blocking behaviour. None are published, and we deliberately did not test them.
- Selangor-side stations (e.g. Sg. Klang di Sri Manja, Petaling district). We did not fetch the `SEL` page live.
- Whether JPS offers an official data feed on request.

## 3. DOE: Air Pollutant Index (APIMS)

### What we verified ✅
- **No documented public API.** The MyEQMS portal (`eqms.doe.gov.my`) is a JavaScript single-page app. From its own bundle, the map queries:
  - `GET https://eqms.doe.gov.my/api3/publicmapproxy/PUBLIC_DISPLAY/CAQM_MCAQM_Current_Reading/MapServer/0/query?where=1=1&outFields=STATION_ID,DATETIME,API,CLASS,PARAM_SYMBOL,LONGITUDE,LATITUDE,STATION_LOCATION,STATE_NAME&returnGeometry=false&f=json`
  - This is a standard ArcGIS query. You can narrow it, e.g. `where=STATION_ID IN ('CA19B','CA15W','CA16W')`.
- **No token needed.** HTTP 200 JSON, 68 stations nationwide.
- **Fields:**
  - `STATION_ID`, `STATION_LOCATION`, `STATE_NAME`, `LATITUDE`, `LONGITUDE` (strings and doubles)
  - `API` (double)
  - `CLASS` (`Good` / `Moderate` / `Unhealthy` / …)
  - `PARAM_SYMBOL`
  - `DATETIME` (epoch ms)
- **Update frequency: hourly.** All 42 reporting stations shared one `DATETIME`.
- **API bands:** the DOE bundle's colour logic uses 0–50 / 51–100 / 101–200 / 201–300 / >300, matching the `CLASS` values we observed.
- **Nearest to UM** (our distance calculation; DOE's own `getstationidbylonglat` also returned `CA19B`):

| Station | ID | Distance | 21:00 reading |
|---|---|---|---|
| Petaling Jaya, Selangor | `CA19B` | 4.6 km | **API 143, Unhealthy** |
| Cheras, WP KL | `CA16W` | 7.9 km | **no reading** (`null`) |
| Batu Muda, WP KL | `CA15W` | 10.1 km | API 158, Unhealthy |
| Shah Alam, Selangor | `CA20B` | 10.8 km | **no reading** |

**Spot-checked by hand, 23 Oct 2026, ~21:40 MYT ✅:** DOE's public map showed API 143 for Petaling Jaya, matching `CA19B` at 21:00 MYT in the saved sample.

### Problems and things likely to break ⚠️
- **Undocumented internal service.** DOE can change the path (`api3`), the layer name, or block outside access at any time.
- **Timestamp quirk.** `DATETIME = 1792789200000` decodes to 21:00 **UTC**, but the reading is for 21:00 **MYT**. The value is local time stored as if it were UTC. DOE's own code formats it with `getUTC*()` methods.
  - We must do the same, or every reading will be 8 hours in the future.
- **Missing stations.** 26 of 68 stations returned `null` for every value, including Cheras (KL). Fall back to the next nearest station, and show which station is used.
- **`PARAM_SYMBOL` is `"**"`** for every reporting station. Its meaning is undocumented, so don't display it.
- **The nearest-station endpoint returns `DISTANCE: 2.88`** against our 4.6 km. The unit is unknown (it may be miles). Don't use it.

### Could not verify ❌
- Terms of use or reuse permission. Only a copyright line and a privacy-policy link were found.
- Rate limits. None are published.
- Whether data.gov.my or DOE offers an official air-quality API. None appears in the data.gov.my docs navigation.

---

## Mapping real responses to `HazardReading` and `Level`

| Source → our `type` | `id` | `value` / `unit` | `level` rule (proposed) | `observedAt` |
|---|---|---|---|---|
| InfoBanjir water level → `flood_level` | `infobanjir_0232121WL_flood_level` | water level / `m` | `< Waspada` → **normal** · `≥ Waspada` → **alert** · `≥ Amaran` → **warning** · `≥ Bahaya` → **danger** (JPS's own thresholds per station) | "Last updated" + `+08:00` |
| InfoBanjir rainfall → `rainfall` | `infobanjir_0232231RF_rainfall` | Total 1 Hour (Now) / `mm/h` | JPS bands: 0–30 mm (Light/Moderate) → **normal** · 31–60 mm (Heavy) → **alert** · > 60 mm (Very Heavy) → **warning** · **danger**: not set from 1-hour rain alone (proposal: ≥ 60 mm over the last 3 h, using JPS's flash-flood guidance and our own stored history) | "Last updated" + `+08:00` |
| MET warning → `weather_warning` | `met_<issued>_<title-slug>_weather_warning` | `null` | Only if `text_en` mentions Kuala Lumpur or Petaling and `now` is between `valid_from` and `valid_to`: thunderstorm or heavy-rain warning → **alert**. Escalate to **warning/danger** only when the title states a higher level (wording ❌ unverified). Marine and "No Advisory" → ignore | `warning_issue.issued` + `+08:00` |
| DOE API → `air_quality` | `doe_CA19B_air_quality` | `API` / `API` | 0–100 (Good, Moderate) → **normal** · 101–200 (Unhealthy) → **alert** · 201–300 (Very Unhealthy) → **warning** · > 300 (Hazardous) → **danger** | `DATETIME` read as MYT wall-clock |
| MET forecast | not a `HazardReading` | — | informational only | `date` |

**Ingestion rules that apply to every source:**
- Reject `-9999`, `null`, impossible zeros, and readings older than 2× the source's update interval. Record those stations in `sourceStatus`.
- Keep the last good reading rather than overwriting it with bad data.
- If a source's HTML or JSON shape changes, set `sourceStatus.ok = false`. Don't publish partial data.

**Real example** of the 20:45 reading at Sg. Klang di Bukit Putra:
`{ id: "infobanjir_0232121WL_flood_level", type: "flood_level", source: "infobanjir", stationId: "0232121WL", stationName: "Sg. Klang di Bukit Putra (F2)", location: {lat: 3.113333, lng: 101.67586}, value: 18.96, unit: "m", level: "normal", observedAt: "2026-10-23T20:45:00+08:00" }`
That is 5.74 m below the Alert level.

## Recommended changes (applied)

All 10 changes were applied to [data-models.md](data-models.md), [architecture.md](architecture.md) and [mock/](../mock) on 23 Oct 2026.

**HazardReading model**
1. **Add `validFrom` / `validTo` (Timestamp?)** for `weather_warning`. MET provides them, and alerts should expire exactly when the warning does.
2. **Add `thresholds: { alert, warning, danger }?`** (number) for `flood_level`. InfoBanjir gives exact values per station. The UI can then show "5.7 m below Alert", which is more useful than a coloured label.
3. **Add `previousValue: number?`** so the backend can show rising or falling trend. InfoBanjir's HTML has no trend column, so we compute it from our last stored value.
4. **`rainfall` readings come from `source: "infobanjir"`, not `met`.** MET's open API has no rain observations. Update the docs and mocks to match.
5. **Optional `stationDistanceKm: number`** so the UI can say "Petaling Jaya station, 4.6 km away". This matters because the AQI station is off-campus and sometimes falls back to one 10 km away.

**Other models and docs**
6. **Forecast needs its own small doc** (e.g. `forecasts/kuala_lumpur`: 7 days, with a Malay→English phrase mapping). It is not a hazard and shouldn't trigger the alert rules.
7. **`sourceStatus` should add `stationsMissing: string[]`.** This explains, for example, "Cheras AQI station offline".
8. **Fix data-models.md's `Level` table.** Remove the unverified MET "Waspada/Buruk/Bahaya" column, or mark it "to confirm". Add the JPS rainfall bands.
9. **Fix architecture.md:**
   - §6 says flood data "appears to be available via data.gov.my". It isn't live.
   - The ingestion job should **poll each source on its own schedule** instead of "every 5 min":
     - MET warnings: every 5 min (1 of 4 allowed requests per minute)
     - MET forecast: every 6 h
     - InfoBanjir: every 15 min (2 requests)
     - DOE: every 15 min (1 narrowed query)

     That totals about 600 requests a day across all sources.
10. **Update mock data with real IDs and thresholds.**
    - Today's mocks use invented ones. `infobanjir_3116430` at 31.2 m doesn't exist. The real Jambatan Sulaiman (F2) is `0232921WL` with thresholds 23.00 / 26.00 / 27.25 / 28.50 m.
    - `doe_CA0016` should be `CA19B`.
    - `met_48647_rainfall` should be `infobanjir_0232231RF_rainfall`.

## Registration and permissions (what you need to do)

| Item | Needed for v1? | Action |
|---|---|---|
| data.gov.my token | **No.** No token is documented, and 4 requests/min is enough | None |
| MET `api.met.gov.my` token | **No.** Only needed if data.gov.my stops working | Register at <https://api.met.gov.my/> (name + email). Store the token in Secret Manager, never in files |
| JPS (InfoBanjir) | No registration exists | **Recommended:** email JPS to say we use public data at 2 requests per 15 min, and ask whether an official feed exists. This shows good faith in the presentation |
| DOE (APIMS) | No registration exists | **Recommended:** same email to DOE, especially because the endpoint is undocumented |
