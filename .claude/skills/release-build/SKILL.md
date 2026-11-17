---
name: release-build
description: Build and publish a new version of the Telugu Calendar Android app (APK + version.json for the in-app updater). Asks whether this is a major, minor or patch update, bumps the version, and runs every step in Docker. Use when the user asks to release, publish, ship, build a new version, or bump the app version.
---

# Release a new version of Telugu Calendar

Read `CLAUDE.md` first if you have not. Everything runs in Docker from the repo root; install nothing on the machine.
Pushing is outward-facing: do the local steps freely, but ask before `git push`.

## 0. Setup (every session)
```bash
export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"   # Windows host: makes `docker` available
cd D:/Telugu-Calendar
docker image inspect telugu-calendar-build >/dev/null 2>&1 || docker compose build
```
Run builds with `run_in_background` and wait for the completion notification; do not poll.

## 1. Ask what kind of update this is
Use AskUserQuestion, header "Update type": **Minor (Recommended)** new features or data (e.g. 1.1.0 -> 1.2.0), **Major** big or breaking change, **Patch** bug fixes only.
Read `version.properties` first and show what each choice would produce. Also confirm 2 to 5 plain-language "What's new" lines (they go into the update dialog).

## 2. Check the tree
* `git status --short`: tell the user about uncommitted changes; they become part of this release.
* `keystore/keystore.properties` must exist. If it does not and this is the **first** release, run `docker compose run --rm android sh make-keystore.sh` and remind the owner to back up `keystore/`. If it is missing on a later release, **stop**: a new key breaks in-place updates. Ask the owner.
* The bundled data must verify: `cd tools/panchangam && docker compose run --rm generator python verify.py`.

## 3. Bump the version (in Docker)
```bash
docker compose run --rm android sh bump-version.sh <major|minor|patch>
```
Write the confirmed "What's new" lines into `releases/notes.txt` (replace the old text).

## 4. Test and build (in Docker)
```bash
docker compose run --rm android ./gradlew testDebugUnitTest publishApk --console=plain -q
```
Writes `releases/telugu-calendar.apk` and `releases/version.json`. If a test or the build fails, fix the cause (never skip tests); if you give up, restore `version.properties` and `releases/notes.txt` with git.

## 5. Verify the output
* `cat releases/version.json`: `versionCode` / `versionName` equal `version.properties`; `sha256` and `size` present; `sha256sum releases/telugu-calendar.apk` matches.
* Test results show 0 failures: `grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' app/build/test-results/testDebugUnitTest/*.xml`.

## 6. Commit
Stage only `version.properties`, `releases/` and source/doc changes that belong to the release. Message `Release <versionName>: <summary>` plus the session's co-author trailer. Never stage `secrets.properties`, `keystore/` or `build/`.

## 7. Push only with the owner's OK
Ask "Push release <versionName> to GitHub?". On yes `git push origin main`, wait ~10 seconds, then check what phones will see:
```bash
curl -sL https://raw.githubusercontent.com/laxmanarao5/Telugu-Calendar/main/releases/version.json
```
(GitHub caches raw files about 5 minutes.) Phones offer the update on next launch, or via More > Check for updates.

## 8. Report
Old -> new version, test count, APK size, whether it was pushed, and that nothing was run on a phone (unless it was).
