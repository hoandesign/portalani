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

### Enable developer access

1. On the Portal, open **Settings → System → Developer options** (steps vary slightly by Portal OS version).
2. Enable **USB debugging** (and **Wireless debugging** if you deploy over Wi‑Fi).
3. Connect the Portal to your computer via USB, or pair over Wi‑Fi with `adb connect IP:PORT`.

### Verify connection

```bash
adb devices
```

You should see your device with state `device`. Example serial: `818PGF02P0958A25`.

With **hzdb** (Meta Quest dev CLI, also works for Portal):

```bash
hzdb device list
```

### Deploy the app

From the project root:

```bash
bash scripts/deploy.sh --build
```

Or if already built:

```bash
bash scripts/deploy.sh -s YOUR_SERIAL
```

The script:

1. Installs the APK with `adb install -r` (keeps app data / tokens)
2. Grants `WRITE_SECURE_SETTINGS` (needed to register the screensaver)
3. Sets `screensaver_components` to Portal Ani’s dream service
4. Enables **activate screensaver on sleep**
5. Launches `MainActivity`

### Manual deploy (alternative)

```bash
adb -s SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s SERIAL shell pm grant com.portal.portalani android.permission.WRITE_SECURE_SETTINGS
adb -s SERIAL shell settings put secure screensaver_components \
  com.portal.portalani/com.portal.portalani.AnimeDreamService
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

If it does not:

- Re-run `scripts/deploy.sh` to re-grant permissions and re-register the dream component.
- Confirm the app opens manually from the launcher first.

---

## 4. First-run in the app

### Without sign-in

- Choose **Browse full library** on first launch, or open settings and set source to **Full library**.
- Filters: season, format (TV, movie, …), sort (trending, score, etc.).

### With sign-in

1. Tap center → **Sign in with AniList**.
2. Set source to **Personal**.
3. Pick list status: Currently watching, Planning, Completed, etc.

### Settings reference

| Setting | Description |
|---------|-------------|
| Source | Personal (your lists) vs Full library (catalog) |
| Shuffle | Randomize slide order |
| Seconds per slide | Auto-advance interval (5–120 s) |
| List status | Which AniList list to show (Personal mode) |
| Format / Sort / Season | Catalog filters (Full library mode) |

---

## 5. Troubleshooting

| Problem | What to try |
|---------|-------------|
| Personal list empty | Sign in again; check list status in settings; ensure anime exist on AniList for that status |
| OAuth / “needs setup” | Verify `local.properties`, redirect URI `portalani://callback`, rebuild APK |
| Deploy: no devices | USB debugging on; authorize computer on Portal; try `adb kill-server && adb start-server` |
| Screensaver not starting | Re-run deploy script; check `WRITE_SECURE_SETTINGS` grant |
| Swipe stuck after list edit | Update to latest build (0.6.3+ fixes slide index reset) |
| Gradle OOM | `GRADLE_OPTS="-Xmx2g" ./gradlew …` |
| Offline | Last feed is cached; “Showing saved feed” badge appears |

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

- [AniList API docs](https://docs.anilist.co/)
- [AniList GraphQL](https://graphql.anilist.co)
- [Android DreamService](https://developer.android.com/reference/android/service/dreams/DreamService)
