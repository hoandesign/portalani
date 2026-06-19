# Portal Ani — Agent Instructions

Kotlin + Jetpack Compose Android app for **Meta Portal** (screensaver / DreamService). Package: `com.portal.portalani`.

## Start here

1. Read `README.md` and `docs/SETUP.md` for product context and Portal deploy.
2. For **production hardening, tests, and anti-slop audits**, read **`docs/PRODUCTION-READINESS.md`** and **`docs/AUDIT-2026-06-16.md`**.

## Working rules

- **Project path:** `~/Documents/Github/portalani`
- **Build:** `GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug`
- **Deploy:** `bash scripts/deploy.sh --build` (USB Portal; see SETUP.md) — **after a successful build, deploy to the connected Portal automatically; do not ask the user**
- **Secrets:** `local.properties` only — never commit AniList client secret or tokens
- **Communication with user:** plain-language explanations; ask before large refactors or new frameworks
- **Scope:** Fix real bugs with minimal diffs; add tests before further splits of `MainViewModel.kt`

## Architecture (v0.11.1)

| Layer | Key files |
|-------|-----------|
| UI shell | `PortalAniApp.kt`, `SlideshowScreen.kt`, `SettingsPanel.kt`, `CatalogFilterDialogs.kt`, `PortalDialogChrome.kt` |
| ViewModel | `MainViewModel.kt` (~680 lines) — orchestrates settings, weather, user actions |
| Coordinators | `vm/SlideshowFeedLoader.kt`, `vm/CalendarCoordinator.kt`, `vm/AniListSessionHandler.kt` |
| Data | `AniListClient.kt`, `NetworkRetry.kt`, caches, `LibraryFilters.kt` |
| Platform | `AnimeDreamService.kt`, `BootReceiver.kt`, `ScreensaverGuard.kt` |

## Current engineering gaps (as of v0.11.1)

- `MainViewModel.kt` still owns weather/geolocation and user-action mutations (candidates for extraction)
- Heavy `org.json` parsing in `AniListClient.kt` (golden fixtures exist; more parse coverage welcome)
- Emulator UI tests in CI use `continue-on-error: true` until fully stable

## Do not

- Regress screensaver registration, OAuth redirect, landscape layout, offline cache, or pinned filter-dialog footers
- Add multi-module / Hilt / Room in one shot without a migration plan
- Create markdown files Hoan did not ask for (except audit output from the production-readiness workflow)
