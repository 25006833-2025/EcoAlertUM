# Mock data

Fake data for building UI and logic before the backend exists. Field definitions are in [docs/data-models.md](../docs/data-models.md).

All files describe **one consistent scenario**: Thursday 23 Oct 2026, around 16:00, an afternoon thunderstorm over UM.

- MET has a thunderstorms warning for KL. The UM rain gauge shows heavy rain (38.5 mm/h, level `alert`). Sg. Klang at Bukit Putra is at `alert` and rising, Sg. Klang at Jalan Ang Seng is `normal`, and air quality is `normal`.
- `forecasts.json` holds MET's 7-day forecast for `Tn079` Kuala Lumpur.
- Two alerts are `active` and one older AQI alert is `expired`.
- There are reports in every status: `verified`, `unverified` (×2), `rejected`, `resolved`. One has no photo and no AI category yet.
- Users: three students (one with notifications off and no faculty) and one team member with the `moderator` role. Everyone uses an `@siswa.um.edu.my` email.
- Quiz results include a pre/post pair (`uid_aisyah_01`, 4 → 9) so you can test the learning-gain screen.

Cross-references are consistent: `reporterUid`, `verifiedBy` and `uid` point at `users.json`, and `readingIds` point at `hazardReadings.json`.

## What is real and what is invented

"Real" means copied from a real response in [docs/samples/](../docs/samples/), captured on 23 Oct 2026. Never treat anything in the "Invented" column as real data.

| File | Real (from official sources) | Invented (scenario values) |
|---|---|---|
| `hazardReadings.json` | Station IDs (`0232231RF`, `0232121WL`, `0232131WL`, `CA19B`), station names, river `thresholds`. Coordinates come from the data.gov.my station list (**to confirm**) | Every `value` and `previousValue` (38.5 mm/h, 25.12 m, 21.40 m, API 64) and every timestamp. The KL thunderstorms warning follows MET's real format, but the warning itself is invented. On 23 Oct, real readings were different, and real warnings did not include KL |
| `forecasts.json` | MET's 23 Oct forecast for `Tn079`: dates, Malay phrases, temperatures | `en` translations (written by us, **to confirm** by a BM speaker) and `fetchedAt` |
| `alerts.json` | Station names and thresholds quoted in messages | All alerts, their messages, IDs and timestamps |
| `reports.json` | — | Everything |
| `users.json` | — | Everything (fictional people) |
| `quizResults.json` | — | Everything |
| `emergencyContacts.json` | National numbers 999, 112, 994, 991 | UM numbers (`+6030000000x` placeholders, to be replaced by F) |

**Still to confirm:**
- Station coordinates (hand-check against the official station positions)
- Forecast translations (the Malay→English phrase table in data-models.md)
- The rainfall "danger" rule (proposed ≥ 60 mm over 3 h, see data-models.md)

## Notes

- Each file is a JSON array. `id` is the Firestore document ID. It is not stored as a field.
- Timestamps are ISO 8601 strings here. In Firestore they are `Timestamp`. `location` is `{lat, lng}` here and a `GeoPoint` in Firestore.
- Emergency numbers 999 / 112 / 994 / 991 are the real Malaysian national numbers. The **UM numbers are placeholders** (`+6030000000x`). F will replace them with real numbers from UM's official website and fill in `sourceUrl` and `sourceCheckedAt`.
- `photoPath` points at Storage paths that do not exist. Use a placeholder image in the UI for now.
