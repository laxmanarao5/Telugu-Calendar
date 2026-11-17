# Agent instructions (Codex, Antigravity, Cursor, Copilot, Gemini CLI, Claude Code, ...)

The full project notes are in **[CLAUDE.md](CLAUDE.md)**. Read it completely before changing anything. Despite the file name it applies to every AI agent and every developer.

## Rules that matter most
* Use **Docker for every build tool** (nothing is installed on the machine): `docker compose run --rm android ./gradlew ...`; the data generator runs from `tools/panchangam/`.
* **Ask the owner before anything outward-facing**: commit, push, release, GitHub changes.
* The app only reads `app/src/main/assets/calendar/<year>-<MM>.json` (one file per month; the app reads the current month first); the format is [DATA_FORMAT.md](DATA_FORMAT.md). Change generator, doc and app together.
* Festival dates are verified against published Telugu panchangams (`tools/panchangam/verify.py`); do not loosen the check to make it pass.
* Never commit `secrets.properties` or `keystore/`.

## Task playbooks
`.claude/skills/<name>/SKILL.md` are plain Markdown steps any agent can follow: `release-build` (bump, build, publish an APK) and `new-year` (generate and ship the next year's data).
