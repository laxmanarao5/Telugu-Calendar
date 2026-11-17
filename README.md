# తెలుగు క్యాలెండర్ · Telugu Calendar

A free Android app: the Telugu panchangam calendar for **2026** (2027 is generated the same way and is added by dropping in one data file), with

* **Wall calendar view**: a traditional paper-calendar page. The Telugu year, season, ayana and lunar months in the header, the tithi under every date, festivals in each box, red Sundays and holidays, and the month's festival list below. Swipe for the next month.
* **Modern view**: a clean month grid with dots on special days, and the full panchangam of the selected day under it (tithi, nakshatra, yoga, karana with end times, sunrise, sunset, Rahu Kalam, Yamagandam, Gulika, Abhijit).
* **Festivals (పండుగలు)**: every festival, holiday, vratam (Ekadashi, Sankashti, Pradosham, Amavasya, Pournami ...) and special day of the year, with filters. Opens at the next upcoming one.
* Telugu and English (switch in the top bar), light and dark themes.
* **Settings (More tab)**: language, theme, and **"Opens on"**: which view the app starts with (Wall calendar by default; Modern or Festivals if you prefer).
* A **Google AdMob banner** at the bottom (test ads until you put in your own ids, see below).
* An **in-app update checker**: on launch it reads `releases/version.json` from this GitHub repo and offers the new APK.

## Download
**[Download the latest APK](https://github.com/laxmanarao5/Telugu-Calendar/raw/main/releases/telugu-calendar.apk)** (version 1.0.0, about 24 MB). On your phone, open the file and allow "Install unknown apps" for your browser when asked. The app then updates itself from this repo.

Everything is worked out ahead of time and bundled in the app as JSON, so it runs fully offline. Nothing to pay for, no server.

## Build and run (everything in Docker, nothing installed on the machine)
```bash
docker compose build                                                  # once: JDK 21 + Android SDK + Gradle
docker compose run --rm android ./gradlew testDebugUnitTest assembleDebug
```
The APK is `app/build/outputs/apk/debug/app-debug.apk`. On Windows with Docker Desktop, first run `export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"` in Git Bash.

## Calendar data (2026, 2027, ...)
`app/src/main/assets/calendar/<year>.json` is produced by `tools/panchangam/` (Python + Swiss Ephemeris, Lahiri ayanamsa, Hyderabad sunrise). The format is in [DATA_FORMAT.md](DATA_FORMAT.md).
The app loads every year file it finds and opens on the current month, so **adding a year is: generate its file, rebuild, release**:
```bash
cd tools/panchangam
docker compose run --rm generator python generate.py 2028
docker compose run --rm generator python verify.py        # checks the generated festivals against published dates
```
See [tools/panchangam/README.md](tools/panchangam/README.md) for how festival dates are decided and verified.

## Ads (Google AdMob)
Without any setup the app shows Google's public **test** banner. To use your own: copy `secrets.properties.sample` to `secrets.properties` and fill in
`ADMOB_APP_ID` (looks like `ca-app-pub-1234567890123456~1234567890`) and `ADMOB_BANNER_UNIT_ID` (`ca-app-pub-1234567890123456/1234567890`), then rebuild.
`secrets.properties` is git-ignored. Never tap your own live ads while testing (use the test ids).

## Releases and the update checker
Phones compare their `versionCode` with `releases/version.json` on `main` of this repo (public repo, no sign-in needed).
```bash
docker compose run --rm android sh make-keystore.sh                       # once: the signing key (keystore/ is git-ignored: back it up!)
docker compose run --rm android sh bump-version.sh minor                  # or major / patch
docker compose run --rm android ./gradlew testDebugUnitTest publishApk    # writes releases/telugu-calendar.apk + version.json
```
Commit `version.properties` and `releases/`, push, and installed apps offer the update. Android only installs an update signed with the **same key**, so never lose `keystore/`.
The repo name used by the updater is `Updater.REPO` in `app/src/main/java/com/telugucalendar/app/data/update/Updater.kt`.

## Repo layout
| Path | What |
|---|---|
| `app/` | The Android app (Kotlin, Jetpack Compose, Material 3) |
| `app/src/main/assets/calendar/` | Generated year data |
| `tools/panchangam/` | The data generator and its verification |
| `DATA_FORMAT.md` | Contract between generator and app |
| `releases/` | Published APK and `version.json` for the updater |
| `CLAUDE.md`, `AGENTS.md` | Notes for AI agents and new developers |

## A note on accuracy
Tithi, nakshatra, yoga and karana are computed astronomically for **Hyderabad sunrise**; in other towns the times differ by a few minutes. Festival dates follow the usual Telugu panchangam rules and were checked against published 2026 dates, but for important ceremonies please also ask your priest. Marriage / muhurtham dates are not included.
