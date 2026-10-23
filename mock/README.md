# Mock data

Fake data for building UI and logic before the backend exists. Field definitions are in [docs/data-models.md](../docs/data-models.md).

All files describe **one consistent scenario**: Thursday 23 Oct 2026, around 16:00, an afternoon thunderstorm over UM.

- There is heavy rain (level `warning`), Sg. Klang is at `alert`, and air quality is `normal`.
- Two alerts are `active` and one older AQI alert is `expired`.
- There are reports in every status: `verified`, `unverified` (×2), `rejected`, `resolved`. One has no photo and no AI category yet.
- Users: three students (one with notifications off and no faculty) and one team member with the `moderator` role. Everyone uses an `@siswa.um.edu.my` email.
- Quiz results include a pre/post pair (`uid_aisyah_01`, 4 → 9) so you can test the learning-gain screen.

Cross-references are consistent: `reporterUid`, `verifiedBy` and `uid` point at `users.json`, and `readingIds` point at `hazardReadings.json`.

## Notes

- Each file is a JSON array. `id` is the Firestore document ID. It is not stored as a field.
- Timestamps are ISO 8601 strings here. In Firestore they are `Timestamp`. `location` is `{lat, lng}` here and a `GeoPoint` in Firestore.
- Station IDs, coordinates and river thresholds are **plausible but invented**.
- Emergency numbers 999 / 112 / 994 / 991 are the real Malaysian national numbers. The **UM numbers are placeholders** (`+6030000000x`). F will replace them with real numbers from UM's official website and fill in `sourceUrl` and `sourceCheckedAt`.
- `photoPath` points at Storage paths that do not exist. Use a placeholder image in the UI for now.
