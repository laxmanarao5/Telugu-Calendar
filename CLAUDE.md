# Telugu Calendar: read this first

Any AI agent (or new developer) working in this repo should read this file before touching anything. This repo is meant to be **public**: no secrets, no real people's names or numbers.
`AGENTS.md` is the short entry point for other agent tools; keep the two in step.

## What this is
An Android app (Kotlin, Compose, Material 3) with the **Telugu panchangam calendar**: a wall-calendar view, a modern month view with the day's panchangam, and a festivals list. The owner asked for:
* 2026 now, and 2027 later: the app is **data driven**, one JSON file per year in `app/src/main/assets/calendar/`.
* A Google AdMob banner whose ids the owner configures later (blank = Google's test ids).
* An app-update checker that reads `releases/version.json` from the GitHub repo `laxmanarao5/Telugu-Calendar` (same design as the owner's Vinayaka Finance app).
It lives in its own repo (the owner's other projects are separate repos; this one was split out on purpose).

## How the owner wants us to work (decisions, not suggestions)
* **Docker for every build tool.** No JDK, Android SDK, Gradle or Python on the machine. `docker compose run --rm android ...` at the repo root; the data generator has its own compose file in `tools/panchangam/`.
  On this Windows host first: `export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"`.
* **Ask before anything outward-facing**: commit, push, publish a release, create or change GitHub repos. Commit and push only when the owner says so.
* **Panchangam is computed ahead of time, never on the phone.** The app only reads JSON (`DATA_FORMAT.md` is the contract). Change the format in the generator, the doc and the app together.
* **Telugu first**: Telugu is the default language, with an English switch. Every visible string lives in `domain/Strings.kt` (UI) or `domain/Names.kt` (tithi, nakshatra, months ...) as a Telugu/English pair.
* **Festival dates must match published Telugu panchangams.** `tools/panchangam/verify.py` checks the generated dates against `expected-2026.json`; never "fix" a mismatch by loosening the check.
* Marriage / muhurtham dates are deliberately not included (complex rules, easy to get wrong).

## Architecture (package `com.telugucalendar.app`, under `app/src/main/java/`)
| Area | Where | Notes |
|---|---|---|
| Data model | `data/Model.kt` | `YearData`, `DayData`, `Slot`, `Festival`, and `AppData` (all years indexed by date, pager months, festival filters) |
| Loading | `data/CalendarRepository.kt` | reads every `assets/calendar/*.json` |
| Settings | `data/Prefs.kt` | DataStore: language, theme, dismissed update |
| Updates | `data/update/Updater.kt` | version.json check, SHA-256 verified download, installer intent. `Updater.REPO` is the GitHub repo |
| Logic | `domain/CalendarLogic.kt`, `Names.kt`, `Strings.kt` | pure Kotlin, unit tested |
| UI | `ui/AppRoot.kt`, `ui/AppViewModel.kt`, `ui/screens/`, `ui/components/`, `ui/theme/` | tabs: Wall, Modern, Festivals, More; the two calendar tabs share the current month |
| Ads | `ui/components/BannerAd.kt` | adaptive AdMob banner; hides itself when no ad loads |

## Ads
`app/build.gradle.kts` reads `ADMOB_APP_ID` and `ADMOB_BANNER_UNIT_ID` from `secrets.properties` (git-ignored) or the environment; blank falls back to Google's public test ids. The app id goes into the manifest through a placeholder.
Do not put real ids in git history of a public repo unless the owner says so; they are not secret, but they identify the owner's AdMob account.

## Build and release (Docker, from the repo root)
```bash
docker compose build
docker compose run --rm android ./gradlew testDebugUnitTest assembleDebug
docker compose run --rm android sh make-keystore.sh              # once; keystore/ is git-ignored and must be backed up
docker compose run --rm android sh bump-version.sh minor         # or major / patch (edits version.properties)
docker compose run --rm android ./gradlew testDebugUnitTest publishApk   # releases/telugu-calendar.apk + version.json
```
Skills for these steps are in `.claude/skills/` (`release-build`, `new-year`). Android accepts an update only if it is signed with the same key, so **losing `keystore/` breaks in-place updates**.

## Adding a year
Run the generator for the new year, run `verify.py`, rebuild, bump the version, release. Details: `.claude/skills/new-year/SKILL.md`.

## State and open items
* See the README for what exists. **Not verified on a real phone**: layout on small screens, Telugu glyph rendering on every ROM, the AdMob banner with the owner's real ids, the in-app update install flow.
* Rahu / Yama / Gulika / Abhijit are the standard weekday-table values for Hyderabad sunrise to sunset.

## Conventions
* Kotlin, Compose, no XML layouts. Add or update a unit test with each behaviour change and run the tests in Docker.
* Commit messages end with the co-author trailer given by the session. Windows git warns about LF/CRLF; harmless.
