# Portal Ani — Production Readiness Handoff

**Purpose:** Onboard a fresh AI agent session with zero prior context. Turn this repo from fast “vibecode” iteration into a maintainable, tested, shippable Android app for Meta Portal.

**Repo:** `portalani` · Package `com.portal.portalani` · Kotlin + Jetpack Compose · **v0.11.1** at time of writing.

**Status (2026-06-19):** Phases 0–3 largely complete — audit doc, 107 JVM tests, CI on push/PR, R8 release builds, coordinators extracted (`SlideshowFeedLoader`, `CalendarCoordinator`, `AniListSessionHandler`), network retry on read-only API calls, emulator UI smoke tests in CI. Remaining work: further VM slimming, optional Detekt, tighten emulator job to block merges when stable.

**Owner:** The project owner (non-technical). Explain trade-offs in plain language. Get approval before large refactors or dependency additions.

---

## Copy-paste prompt for the next agent session

Paste everything inside the block below as your **first user message** (or save as a Cursor rule / project instruction).

```markdown
# Mission: Make Portal Ani production-ready (not vibecode)

You are the lead engineer for **portalani** — an AniList-powered anime screensaver for **Meta Portal** (landscape 1280×800, minSdk 28, targetSdk 29, DreamService screensaver). The app works on device today but was built iteratively with AI assistance. Your job is to **audit deeply**, **add real tests**, **remove AI slop**, and **establish release discipline** — without breaking Portal-specific behavior or rewriting working features for aesthetics.

## Non-negotiables

1. **Read before you edit.** Start with `README.md`, `docs/SETUP.md`, and this file (`docs/PRODUCTION-READINESS.md`). Map the architecture before proposing changes.
2. **Evidence over opinions.** Run `./gradlew test`, `./gradlew assembleDebug`, and cite file:line for every claim in your audit report.
3. **Test the logic that matters first.** Pure Kotlin in `data/` (filters, season encoding, calendar week math, JSON cache round-trips) gets JVM unit tests before UI refactors.
4. **No slop PRs.** Do not add generic `utils/`, `helpers/`, `Manager` classes, duplicate abstractions, or 200-line “cleanup” commits that change behavior without tests.
5. **Portal constraints stay sacred.** Landscape-only, DreamService screensaver registration, `WRITE_SECURE_SETTINGS` deploy flow, OAuth redirect `portalani://callback`, offline cache — do not regress these. Verify against `docs/SETUP.md`.
6. **Small, reviewable PRs.** Prefer a sequence: audit doc → unit tests → targeted refactors → CI → release hardening. One concern per PR where possible.
7. **Ask the user** before: new major dependencies, splitting into multi-module, changing OAuth/storage, or deleting features.

## Phase 0 — Inventory (do not skip)

Deliver a written **Architecture & Risk Report** (`docs/AUDIT-YYYY-MM-DD.md`) covering:

| Area | Files to read | Questions |
|------|---------------|-----------|
| UI shell | `PortalAniApp.kt` (~1.5k lines), `AnimeFrameSlide.kt`, `CalendarFrame.kt` | God composable? State hoisting? |
| State | `MainViewModel.kt` (~1.2k lines) | What belongs in VM vs repository? |
| Network | `AniListClient.kt`, `AniListAuth.kt`, `WeatherClient.kt` | Error handling, retries, parsing fragility |
| Persistence | `TokenStore.kt`, `AnimeSlideCache.kt`, `CalendarWeekCache.kt` | Migration strategy? Corrupt cache handling? |
| Domain | `LibraryFilters.kt` (~600 lines), `CalendarWeek.kt`, `Models.kt` | Testable pure functions? |
| Dialogs | `AnimeInteractionDialogs.kt` (~1k lines) | Duplication with theme? |
| Platform | `AnimeDreamService.kt`, `BootReceiver.kt`, `ScreensaverGuard.kt`, `MainActivity.kt` | Lifecycle edge cases |
| Build | `app/build.gradle.kts`, `scripts/deploy.sh` | Release signing, minify, CI gap |

**Current gaps you must confirm:**
- **Zero test files** under `app/src/test` and `app/src/androidTest` (deps exist, unused)
- **No CI** (no `.github/workflows`)
- **Release build:** `isMinifyEnabled = false`
- **Manual JSON** parsing via `org.json` throughout data layer
- **Secrets:** `local.properties` → `BuildConfig` (document risk; do not commit secrets)

## Phase 1 — Kill AI slop (specific red flags for THIS repo)

Search the codebase and fix or ticket each hit. Do **not** blanket-reformat.

### Structural slop
- [ ] **God files:** `PortalAniApp.kt`, `MainViewModel.kt`, `AnimeInteractionDialogs.kt` — extract only when tests exist for extracted logic
- [ ] **VM does everything:** network, geolocation, weather, calendar cache, slideshow timer, OAuth side effects — draw a target layering diagram (UI → ViewModel → Repository → Client/Store)
- [ ] **Duplicate filter enums + parsing** in `LibraryFilters.kt` and `TokenStore.kt` (`runCatching { valueOf }` repeated) — consolidate deserialization once
- [ ] **Duplicate dialog chrome** — `PortalCenteredDialog`, `portalDialogSurface`, picker rows; ensure one source of truth

### Code-quality slop
- [ ] **`catch (e: Exception)`** in `MainViewModel.kt` — narrow exceptions; surface user-visible errors with context
- [ ] **Magic numbers** in UI (dp/sp thresholds) scattered vs `PortalAniTheme.kt` — centralize or document device-specific choices
- [ ] **Dead code / duplicate imports** (e.g. repeated `detectDragGestures` import history)
- [ ] **Comment noise** that restates the code — delete; keep only non-obvious business rules (AniList API quirks, Portal screensaver behavior)
- [ ] **Over-defensive `runCatching`** that silently swallows bugs — log or fail visibly in debug

### AI-tell patterns to reject in new code
- Unnecessary interfaces with one implementation
- `Repository` + `UseCase` layers for 3-call flows
- Generic `handleError()` / `safeApiCall()` wrappers used once
- README/architecture essays nobody asked for
- `@Suppress` without a one-line justification comment

## Phase 2 — Test strategy (pyramid)

### Tier 1 — JVM unit tests (highest ROI, do first)

Create `app/src/test/java/com/portal/portalani/` with **JUnit 4** (already on classpath). Add **MockK** or hand-rolled fakes only if needed.

**Must-have test classes (minimum):**

| Class | What to test |
|-------|----------------|
| `LibraryFiltersTest` | `matchesSlide`, `matchesContentFilters`, format/country/source/demographic sets, `hideHentai`, API value encoding |
| `SeasonSelectionTest` | `encode`/`decode`, picker state normalization, year column edge cases |
| `CalendarWeekTest` | Week boundaries Mon vs Sun, `parseAiringDate`, entry grouping |
| `isoCountryFlagEmoji` | Valid ISO codes, invalid input |
| `AnimeSlideCacheTest` | JSON round-trip for representative `AnimeSlide` (rankings, tags, list status) |
| `CalendarWeekCacheTest` | Cache serialize/deserialize |
| `AniListClientParseTest` | Extract `parseMedia` / `parseMediaPage` to testable package-visible functions; feed fixture JSON from `src/test/resources/anilist/` |

Use **golden JSON fixtures** copied from real AniList responses (trim PII). Never hit AniList API in unit tests.

### Tier 2 — ViewModel tests

Add `kotlinx-coroutines-test` and `androidx.arch.core:core-testing`. Test `MainViewModel` with fake `AniListClient` + fake stores:

- Settings change triggers correct reload path (personal vs library vs calendar)
- Filter change resets slide index / order token
- OAuth error → `NeedsSetup` state
- Calendar week navigation does not duplicate fetches

Keep VM tests **behavioral**, not implementation-locked to private methods.

### Tier 3 — Compose UI tests (selective)

Use existing `androidx.compose.ui:ui-test-junit4`. **Do not** screenshot-test the whole app.

Smoke tests only:
- Settings sheet opens (use test tags — add `Modifier.testTag` sparingly to key controls)
- Filter dialog shows Apply/Close
- `ListStatusDialog` scrolls (regression for v0.9.34 modal fix)

Run on emulator API 29 landscape or skip in CI if no emulator (document).

### Tier 4 — Manual / device

`bash scripts/deploy.sh --build` on Portal serial from `docs/SETUP.md`. Checklist: screensaver on sleep, OAuth, calendar week swipe, filter modals centered + scrollable.

## Phase 3 — CI & release hardening

1. **GitHub Actions** `.github/workflows/ci.yml`:
   - `GRADLE_OPTS="-Xmx2g" ./gradlew test assembleDebug` on push/PR
   - Cache Gradle
   - Fail on lint/detekt if added later
2. **Release build:**
   - Enable R8/Proguard with keep rules for Compose, OAuth, DreamService
   - Document signing in `docs/RELEASE.md` (keystore via env, not committed)
3. **Versioning:** single source in `app/build.gradle.kts`; tag `v0.10.0` when test + CI baseline lands
4. **Optional:** Detekt or ktlint — add only if auto-fixable; don't bikeshed style in a 10k-line first pass

## Phase 4 — Security & data

- [ ] Confirm `local.properties` and tokens never logged
- [ ] WebView OAuth (`AniListOAuthActivity`) — JS enabled; document threat model (AniList only)
- [ ] `allowBackup="true"` — assess if tokens should be excluded via backup rules
- [ ] Network: HTTPS only; certificate pinning only if the user requests (usually overkill here)

## Definition of done (production-ready v1)

You are **not done** until all are true:

- [ ] `docs/AUDIT-*.md` exists with prioritized backlog (P0/P1/P2)
- [ ] ≥ **40 meaningful unit tests** covering filters, season, calendar, cache JSON (count assertions, not `assertTrue(true)`)
- [ ] `./gradlew test` passes locally and in CI
- [ ] CI workflow green on `main`
- [ ] No new god-file growth (if you split a file, net LOC in largest file should shrink)
- [ ] `README.md` updated: version, testing section, CI badge
- [ ] `docs/SETUP.md` still accurate for Portal deploy
- [ ] The user can run one command to verify: `GRADLE_OPTS="-Xmx2g" ./gradlew test assembleDebug`

## Suggested PR sequence

1. `docs: architecture audit + test plan` (no behavior change)
2. `test: LibraryFilters + SeasonSelection + CalendarWeek`
3. `test: cache round-trips + AniList parse fixtures`
4. `refactor: extract AniList parsing for testability` (minimal)
5. `test: MainViewModel critical paths`
6. `ci: GitHub Actions`
7. `build: release minify + proguard rules`
8. `refactor: split PortalAniApp settings UI` (only if tests guard behavior)

## Commands

```bash
cd ~/Documents/Github/portalani
cp local.properties.example local.properties   # AniList credentials + sdk.dir

GRADLE_OPTS="-Xmx2g" ./gradlew test            # after you add tests
GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug
bash scripts/deploy.sh --build                 # Portal USB deploy

# Device (optional)
adb devices
metavr device list   # or hzdb device list
```

## What NOT to do

- Rewrite UI in React Native / Flutter / separate module graph “for cleanliness”
- Add Ktor/Retrofit/Room/Hilt in one mega-PR without incremental migration plan
- Delete calendar or filter features to shrink scope
- Commit `local.properties`, keystores, or AniList secrets
- “Fix” Portal centering/dialogs without Compose UI test or device screenshot evidence

## Success criteria for the user

When finished, the user should be able to:
1. Merge PRs that show green CI
2. Trust that filter/calendar bugs get caught by unit tests before deploy
3. Ship `app-release.apk` with minify enabled
4. Onboard a human dev using audit doc + SETUP without reading chat history

Start with **Phase 0 Inventory** — post the audit report and wait for the user's priority pick (tests-first vs CI-first vs refactor-first) before large edits.
```

---

## Reference: repository map (quick)

```
portalani/
├── app/src/main/java/com/portal/portalani/
│   ├── MainActivity.kt          # Entry, gestures, dream hooks
│   ├── MainViewModel.kt         # All app state (~1.2k lines) ⚠️
│   ├── AniListOAuthActivity.kt
│   ├── AnimeDreamService.kt     # Screensaver
│   ├── BootReceiver.kt
│   ├── ScreensaverGuard.kt
│   ├── data/
│   │   ├── AniListClient.kt     # GraphQL + org.json parsing
│   │   ├── LibraryFilters.kt    # Filters, season picker domain
│   │   ├── CalendarWeek.kt
│   │   ├── Models.kt
│   │   ├── TokenStore.kt        # SettingsStore + OAuth tokens
│   │   └── *Cache.kt
│   └── ui/
│       ├── PortalAniApp.kt      # Settings + shell (~1.5k lines) ⚠️
│       ├── AnimeInteractionDialogs.kt
│       ├── CalendarFrame.kt
│       └── PortalAniTheme.kt
├── docs/SETUP.md                # Portal deploy + AniList OAuth
├── scripts/deploy.sh
└── README.md                    # User-facing features
```

---

## Reference: known product behaviors (do not break)

| Feature | Critical behavior |
|---------|-------------------|
| Screensaver | `AnimeDreamService` registered via `scripts/deploy.sh` + `WRITE_SECURE_SETTINGS` |
| OAuth | Redirect `portalani://callback`; tokens in private prefs |
| Personal mode | List fetch → client-side format/country/source/demographic filter |
| Full library | AniList API pre-filters format/source; country/demographic on device |
| Calendar | Week grid, no season picker; airing schedule from AniList |
| Modals | Centered, scrollable lists, pinned Close/Apply (v0.9.34) |
| Offline | `AnimeSlideCache` / `CalendarWeekCache` show “saved feed” badge |
| Power | Quiet hours + idle sleep modes |

---

## Reference: dependency baseline

From `gradle/libs.versions.toml`: AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2026.02.01, OkHttp 4.12, Coil 2.7, WorkManager 2.9.1.

**Test deps already declared** in `app/build.gradle.kts` but unused: JUnit 4, Compose UI test, Espresso, AndroidX JUnit.

**Reasonable additions** (justify each): `kotlinx-coroutines-test`, MockK, Turbine (if Flows tested), Robolectric (only if Android context needed in JVM tests — prefer pure functions).

---

## Reference: slop vs legitimate complexity

| Legitimate (keep) | Slop (fix when tested) |
|-------------------|-------------------------|
| Large Compose trees for rich Portal UI | Same dialog layout copy-pasted 5 ways |
| `LibraryFilters` enums matching AniList API surface | Repeated `valueOf` parse blocks |
| Manual JSON (nocodegen) for small app | 500-line parse functions with no fixtures |
| ViewModel orchestrating slideshow + calendar | ViewModel owning HTTP + GPS + JSON parsing |
| Portal-specific dp constants | Magic numbers inline in random composables |

---

## Changelog for this handoff

| Date | Version | Notes |
|------|---------|-------|
| 2026-06-19 | 0.11.1 | Coordinators extracted; network retry; CI emulator UI tests; pinned filter dialog footers |
| 2026-06-16 | — | Initial production-readiness guide after v0.9.34 |

---

*Maintainer: update the copy-paste prompt when major architecture changes land (e.g. after repository extraction or CI goes green).*
