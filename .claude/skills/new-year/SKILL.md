---
name: new-year
description: Generate and ship the Telugu calendar data for a new year (for example 2028): run the panchangam generator in Docker, verify the festival dates against published sources, rebuild the app and release it. Use when the owner says the calendar needs next year's data, or the app shows "No data for this month".
---

# Add a year to the Telugu Calendar

Read `CLAUDE.md` and `DATA_FORMAT.md` first. Everything runs in Docker. Ask the owner before committing or pushing.

1. **Generate**
   ```bash
   export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"
   cd D:/Telugu-Calendar/tools/panchangam
   docker compose run --rm generator python generate.py <year>
   ```
   This writes the twelve month files `app/src/main/assets/calendar/<year>-01.json` ... `<year>-12.json`.
2. **Government holidays**: the generator only knows fixed national days for a year whose Telangana / Andhra Pradesh holiday lists are not published yet. When the lists come out, add them in the generator's holidays file for that year (see `tools/panchangam/README.md`), regenerate, and mark each entry `h:1` (both states) or `h:2` (optional / one state).
3. **Verify**: search for the published Telugu panchangam / festival dates for the new year (Ugadi, Sankranti, Vinayaka Chavithi, Dasara, Deepavali, Maha Shivaratri at least). Add them to `tools/panchangam/expected-<year>.json` with a `source` for each, then `docker compose run --rm generator python verify.py`. Fix the rule or add a documented override for any mismatch; never loosen the check.
4. **Check in the app**: `cd D:/Telugu-Calendar && docker compose run --rm android ./gradlew testDebugUnitTest assembleDebug`. The bundled-data test checks that every year has its twelve month files and a full year of consecutive days.
5. **Release** with the `release-build` skill (minor update; "What's new": "Calendar for <year>").
