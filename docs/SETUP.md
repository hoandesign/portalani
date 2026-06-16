# Portal Ani — Setup Guide

Step-by-step setup for developers: AniList OAuth, building the app, and installing on a Meta Portal.

---

## 1. AniList developer setup

Portal Ani uses AniList OAuth so you can sign in and manage your lists from the Portal.

### Create an OAuth app

1. Sign in at [anilist.co](https://anilist.co).
2. Open **[Settings → Developer](https://anilist.co/settings/developer)**.
3. Click **Create New Client** (or edit an existing one).
4. Fill in:
   - **App name:** e.g. `Portal Ani`
   - **Redirect URL:** must be **exactly**
     ```
     portalani://callback
     ```
     No `https://`, no trailing slash.
5. Save and note your **Client ID** and **Client Secret**.

### Add credentials to the project

1. Copy the example file:
   ```bash
   cp local.properties.example local.properties
   ```
2. Edit `local.properties`:
   ```properties
   sdk.dir=/Users/YOU/Library/Android/sdk
   ANILIST_CLIENT_ID=your_client_id
   ANILIST_CLIENT_SECRET=your_client_secret
   ```
3. **Never commit `local.properties`** — it is in `.gitignore`.

Gradle injects these into `BuildConfig` at compile time. Rebuild after any change.

### Sign-in flow on Portal

1. Open Portal Ani → tap center → **Sign in with AniList**.
2. A WebView opens the AniList authorize page.
3. Approve access; you are redirected to `portalani://callback`.
4. The app exchanges the code for an access token and stores it locally.

If sign-in fails, check that the redirect URI in AniList matches exactly and that you rebuilt after setting `local.properties`.

---

## 2. Developer machine setup

### Prerequisites

| Tool | Notes |
|------|--------|
| JDK 11+ | Android Gradle Plugin requirement |
| Android SDK | API 28+ (Portal), compile SDK 36 |
| `adb` | Android platform-tools, or Meta **hzdb** CLI |

Typical macOS SDK path: `~/Library/Android/sdk`

### Clone and build

```bash
cd ~/Documents/Github/portalani   # or your clone path

cp local.properties.example local.properties
# edit local.properties (SDK path + AniList credentials)

GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

> **Tip:** If Gradle runs out of memory, always set `GRADLE_OPTS="-Xmx2g"`.

### Optional: launcher icon

```bash
bash scripts/generate-icon.sh
```

Requires Google Chrome (headless screenshot of `scripts/icon-generator.html`).

---

## 3. Meta Portal setup

Official reference: [Meta — Set up your device (Portal)](https://developers.meta.com/horizon/documentation/android-apps/portal-setup/)

### Supported devices

| Device | minSdkVersion | Connection |
|--------|---------------|------------|
| Portal (1st and 2nd gen) | 28 / 29 | USB-C (back of device) |
| Portal Mini | 29 | USB-C |
| Portal+ (1st and 2nd gen) | 28 / 29 | USB-C |
| Portal Go | 29 | USB-C (under rubber cover) |
| Portal TV | 29 | USB-C |

Portal Ani targets **minSdk 28** (Portal / Portal+ 1st gen) and **targetSdk 29**.

### Enable ADB on the Portal

Meta Portal uses **Settings → Debug**, not the generic Android “Developer options” menu.

1. On the Portal, open **Settings → Debug**.
2. Tap **ADB Enabled**. Enter your PIN if prompted.
3. Connect the Portal to your computer with a **USB-C** cable (port on the back of the device).
4. On the **first** connection, tap **Allow** on the Portal to trust your computer.

**Portal Go:** The USB-C port is under a rubber cover. Remove it with a flathead screwdriver or rigid flat edge before connecting.

### Install `adb` on your computer

Download [Android platform-tools](https://developer.android.com/tools/releases/platform-tools), extract the folder, and add it to your `PATH` (or run `adb` from inside that folder).

### Verify connection

```bash
adb devices
```

You should see your device with state `device`. Example serial: `818PGF02P0958A25`.

If the list is empty:

- Confirm **ADB Enabled** is on in **Settings → Debug**
- Replug the USB-C cable
- Tap **Allow** on the Portal if the trust prompt appears
- Try `adb kill-server && adb start-server`

Optional — Meta VR CLI ([AI Tooling](https://developers.meta.com/horizon/documentation/android-apps/portal-ai-tooling)):

```bash
metavr device list
```

This project’s deploy script also works with **hzdb** (`hzdb device list`) if you already use the Meta Quest dev CLI.

### Deploy the app

From the project root:

```bash
bash scripts/deploy.sh --build
```

Or if already built (rebuilds automatically when `app/src` or `app/build.gradle.kts` changed):

```bash
bash scripts/deploy.sh -s YOUR_SERIAL
```

The script:

1. Installs the APK with `adb install -r` (keeps app data / tokens)
2. Grants `WRITE_SECURE_SETTINGS` (needed to register the screensaver)
3. Sets `screensaver_components` to Portal Ani’s dream service
4. Enables the screensaver (`screensaver_enabled=1`) and **activate on sleep**
5. Launches `MainActivity`

### Manual deploy (alternative)

```bash
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL shell pm grant com.portal.portalani android.permission.WRITE_SECURE_SETTINGS
adb -s SERIAL shell settings put secure screensaver_components \
  com.portal.portalani/com.portal.portalani.AnimeDreamService
adb -s SERIAL shell settings put secure screensaver_enabled 1
adb -s SERIAL shell settings put secure screensaver_activate_on_sleep 1
adb -s SERIAL shell am start -n com.portal.portalani/.MainActivity
```

### Updating without losing sign-in

- **Do:** `adb install -r` or `scripts/deploy.sh` (replace install)
- **Do not:** `adb uninstall com.portal.portalani` unless you want to wipe OAuth tokens and settings

If you see a signature mismatch after switching debug/release keystores, the deploy script may uninstall once — sign in again afterward.

### Screensaver on Portal

After a successful deploy:

1. Put the Portal to **sleep** (or wait for idle timeout).
2. Portal Ani should start as the dream/screensaver.

Verify registration:

```bash
adb shell settings get secure screensaver_components
adb shell settings get secure screensaver_enabled
```

Expected:

- `screensaver_components=com.portal.portalani/com.portal.portalani.AnimeDreamService`
- `screensaver_enabled=1`

If it does not:

- Re-run `scripts/deploy.sh` to re-grant permissions and re-register the dream component.
- Open Portal Ani once from the launcher (the app re-asserts the screensaver on resume).
- **Another screensaver app installed?** Portal only runs one dream at a time. If you also have [portal-gphotos](https://github.com/ram-nat/portal-gphotos) or similar, whichever app last re-registered wins. Open Portal Ani after the other app, or redeploy Portal Ani, to switch back.
- Portal’s launcher resets the screensaver on boot; Portal Ani re-applies it via boot receiver + a 5-minute background guard (portal-gphotos uses 15 minutes).
- **Quiet hours** (Settings → Power → Off when sleeping): during the sleep window the screensaver stays off by design.
- **portal-gphotos still installed?** Uninstall it, or open Portal Ani after using Photos — both apps fight over `screensaver_components` every few minutes.

---

## 4. First-run in the app

### Without sign-in

- Choose **Browse full library** on first launch, or open settings and set source to **Full library**.
- Filters: season, format (TV, movie, …), sort (trending, score, etc.).

### With sign-in

1. Tap center → **Sign in with AniList**.
2. Set source to **Personal**.
3. Pick one or more lists: Currently watching, Planning, Completed, etc.

### Settings reference

**Slideshow:** frame mode, seconds per slide, shuffle.

**What to show:** source (Personal vs Full library), then list status or catalog filters depending on source.

**Clock & weather:** show clock, show weather, temperature unit (°C / °F), and location. Weather requires the clock to be on. Turning weather on without a saved location opens the location dialog.

| Setting | Description |
|---------|-------------|
| Source | Personal (your lists) vs Full library (catalog) — under **What to show** |
| Frame mode | **Poster only** (centered poster + optional clock) or **Informative** (poster + synopsis side by side) |
| Shuffle | Randomize slide order |
| Seconds per slide | Auto-advance interval (5–120 s) |
| Lists | Which AniList lists to show (Personal mode); select multiple |
| Format / Sort / Season | Catalog filters (Full library mode) |
| Show clock | Time and date overlay in poster mode (bottom-left) |
| Show weather | Current temp and icon beside the date; uses [Open-Meteo](https://open-meteo.com/), no API key |
| Temperature | Celsius or Fahrenheit |
| Location | City search or **Use my location**; stored on device for refresh |
| Power | Always on (default), sleep after idle, or off during quiet hours (default 10 PM–7 AM; 30 min grace if opened during quiet hours) |

### Clock & weather setup

1. Open **Settings** (long-press center).
2. Under **Clock & weather**, turn on **Show clock**.
3. Optionally turn on **Show weather** — if you have no location yet, the location dialog opens.
4. Pick **Use my location** (Portal will ask for location permission once) or search for a city and choose a match.
5. Set **Temperature** to °C or °F if needed.

The clock stays fixed while you swipe slides. It hides automatically when you flip a poster open for details, then fades back when you collapse.

Weather needs network access (`INTERNET`). Location permission is optional and only requested when you tap **Use my location**.

### Slide info panel

When signed in and an anime is on your list, a colored status badge appears above the title (e.g. play icon for **Currently watching**). The row below the title shows community score plus format, year, episodes, studio, and airing status. Genre tags appear under the synopsis.

---

## 5. Troubleshooting

| Problem | What to try |
|---------|-------------|
| Personal list empty | Sign in again; check list status in settings; ensure anime exist on AniList for that status |
| OAuth / “needs setup” | Verify `local.properties`, redirect URI `portalani://callback`, rebuild APK |
| Deploy: no devices | **Settings → Debug → ADB Enabled**; USB-C connected; tap **Allow** on Portal; try `adb kill-server && adb start-server` |
| Screensaver not starting | Re-run deploy script; check `WRITE_SECURE_SETTINGS` grant |
| Swipe stuck after list edit | Update to latest build (0.6.3+ fixes slide index reset) |
| Gradle OOM | `GRADLE_OPTS="-Xmx2g" ./gradlew …` |
| Offline | Last feed is cached; “Showing saved feed” badge appears |
| Weather not showing | Enable clock + weather in settings; set a location; check network on Portal |
| Location denied | Use city search instead of **Use my location**, or grant permission in Portal system settings |

---

## 6. Environment variables (optional)

| Variable | Purpose |
|----------|---------|
| `ADB` | Path to `adb` binary |
| `ANDROID_HOME` | Used to find platform-tools |
| `SERIAL` | Default device serial for `deploy.sh` |
| `GRADLE_OPTS` | JVM heap for Gradle builds |

---

## Related links

- [Meta Portal setup (official)](https://developers.meta.com/horizon/documentation/android-apps/portal-setup/)
- [AniList API docs](https://docs.anilist.co/)
- [AniList GraphQL](https://graphql.anilist.co)
- [Android DreamService](https://developer.android.com/reference/android/service/dreams/DreamService)
