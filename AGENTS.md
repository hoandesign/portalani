# Portal Ani — Agent Instructions

Kotlin + Jetpack Compose Android app for **Meta Portal** (screensaver / DreamService). Package: `com.portal.portalani`.

## Start here

1. Read `README.md` and `docs/SETUP.md` for product context and Portal deploy.
2. For **production hardening, tests, and anti-slop audits**, read **`docs/PRODUCTION-READINESS.md`** — it contains the full session prompt to copy into a new chat.

## Working rules

- **Project path:** `~/Documents/Github/portalani`
- **Build:** `GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug`
- **Deploy:** `bash scripts/deploy.sh --build` (USB Portal; see SETUP.md)
- **Secrets:** `local.properties` only — never commit AniList client secret or tokens
- **Owner:** Hoan Do — plain-language explanations; ask before large refactors or new frameworks
- **Scope:** Fix real bugs with minimal diffs; add tests before splitting god files (`MainViewModel.kt`, `PortalAniApp.kt`, `AnimeInteractionDialogs.kt`)

## Current engineering gaps (as of v0.10.0)

- Compose UI smoke tests exist but are not run in CI (need emulator)
- Heavy `org.json` parsing in `AniListClient.kt`
- God files still large at UI layer (`AnimeInteractionDialogs.kt`; settings moved to `SettingsPanel.kt`)

## Do not

- Regress screensaver registration, OAuth redirect, landscape layout, or offline cache behavior
- Add multi-module / Hilt / Room in one shot without a migration plan
- Create markdown files Hoan did not ask for (except audit output from the production-readiness workflow)
