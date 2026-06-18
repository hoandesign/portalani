# Portal Ani — Release builds

How to produce a minified release APK for sideloading on Meta Portal. This app targets **API 29** intentionally (Portal device); it is not published on Google Play.

---

## Version

Single source of truth: [`app/build.gradle.kts`](../app/build.gradle.kts) (`versionName`, `versionCode`).

After bumping the version, tag the repo:

```bash
git tag -a v0.10.0 -m "v0.10.0 — tests, CI, MainViewModel tests, release minify"
git push origin v0.10.0
```

Use the same tag name as `versionName` (with a `v` prefix).

---

## Prerequisites

Same as [SETUP.md](SETUP.md):

- JDK 17+
- Android SDK (API 36 build-tools)
- `local.properties` with `sdk.dir` and AniList OAuth credentials

---

## Build release APK

```bash
cd ~/Documents/Github/portalani
GRADLE_OPTS="-Xmx4g" ./gradlew test assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

R8 minify and resource shrinking are **enabled** for release. Run the full test suite before shipping.

---

## Signing (optional but recommended)

Without a release keystore in `local.properties`, Gradle signs release builds with the **debug key** so sideload installs work. For your own production key, add to `local.properties` (never commit):

```properties
RELEASE_STORE_FILE=/path/to/portalani-release.keystore
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=portalani
RELEASE_KEY_PASSWORD=your_key_password
```

Create a keystore once:

```bash
keytool -genkey -v \
  -keystore ~/portalani-release.keystore \
  -alias portalani \
  -keyalg RSA -keysize 2048 -validity 10000
```

Then rebuild `assembleRelease`. Gradle picks up the signing config automatically.

**Do not commit** keystores or passwords. Back up the keystore somewhere safe — you need the same key to upgrade installs without uninstalling.

---

## Deploy to Portal

USB deploy (minified release build):

```bash
bash scripts/deploy.sh --release
```

Or manually:

```bash
GRADLE_OPTS="-Xmx4g" ./gradlew assembleRelease
bash scripts/deploy.sh --apk app/build/outputs/apk/release/app-release.apk
```

Build debug for day-to-day dev:

```bash
bash scripts/deploy.sh --build
```

After switching between debug and release signatures, `deploy.sh` may uninstall once — sign in to AniList again.

---

## CI

GitHub Actions runs `test`, `assembleDebug`, and `assembleRelease` on every push/PR. Release builds use the debug signing key when no `RELEASE_STORE_FILE` is set, so R8 minify is validated on a signed artifact.

---

## Release checklist

1. `./gradlew test` — all JVM tests green
2. `./gradlew assembleRelease` — R8 succeeds
3. Bump `versionCode` / `versionName` in `app/build.gradle.kts`
4. Deploy to Portal: screensaver on sleep, OAuth, calendar week swipe, filter modals
5. Tag `vX.Y.Z` and push tag
