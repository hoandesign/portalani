# Portal Ani

AniList-powered anime screensaver for **Meta Portal**. Fullscreen landscape slideshow with cover art, rankings, trailers, optional clock and weather, and optional AniList sign-in.

Inspired by [portal-gphotos](https://github.com/ram-nat/portal-gphotos).

**Package:** `com.portal.portalani`  
**Version:** 0.9.0

## Features

- **Poster mode** (default) — large centered poster with Ken-Burns parallax; tap to **flip** and reveal full details; tap again to collapse
- **Informative mode** — poster plus synopsis, genres, and actions side by side
- **Calendar mode** — weekly airing grid (Mon–Sun or Sun–Sat): poster, title, and local air time per episode; swipe for other weeks; your list anime shown first
- **Clock & weather** (poster mode) — time and date fixed at the bottom-left; optional current conditions from [Open-Meteo](https://open-meteo.com/) beside the date (°C/°F, city search or device location)
- **Personal** — your AniList lists (watching, planning, completed, …); select multiple lists
- **Full library** — browse the catalog with season, format, and sort filters
- AniList OAuth (rate, favourite, add to list), YouTube trailers, offline cache
- Shuffle, slide interval, and power/sleep options
- Registers as Portal idle screensaver (`DreamService`)

## Quick start

1. Follow **[docs/SETUP.md](docs/SETUP.md)** — AniList OAuth app, build, and Portal deploy.
2. Build: `GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug`
3. Deploy: `bash scripts/deploy.sh --build`

## Gestures on Portal

| Action | How |
|--------|-----|
| **Next slide** | Swipe left or tap right edge |
| **Previous slide** | Swipe right or tap left edge |
| **Settings** | Long-press center |
| **More info** (poster mode) | Tap the poster — 3D flip to detail layout; tap again to collapse |
| **Other weeks** (calendar mode) | Swipe left/right or tap screen edges |

On first launch, subtle on-screen hints walk through swipe, hold-for-settings, and (in poster mode) tap-poster. They fade away after you try each gesture or after a short pause.

### Calendar mode

- Seven columns for the current week (starts **Monday** by default; switch to **Sunday** in Settings).
- Header shows month/year; each column shows day number and weekday.
- Up to ~3.5 episode rows visible; scroll down for more in that week.
- Episodes show cover, title, episode number, and **local** air time (e.g. 8:00 PM).
- **Personal** source: only anime on your selected lists. **Full library**: all airing anime matching filters.
- Season scope: **this season + the next 3** (four seasons total). Format and sort still apply; list anime are prioritized.
- Swipe or tap edges to move to previous/next week (no auto-advance timer).

### Clock & weather behavior

- Clock and weather appear only in **poster mode** (hidden in informative mode and while poster detail is open).
- **Settings → Clock & weather** — toggle clock, weather, temperature unit, and location.
- Weather needs a location: **Use my location** (one-time permission) or search for a city.
- Weather data refreshes periodically while the app is running; no API key required.

## Security

- `local.properties` holds your AniList client secret (gitignored).
- OAuth tokens live in app private storage on the Portal.
- Location is used only for weather lookup when you enable it; coordinates are stored locally with your place label.
- Use `adb install -r` when updating; uninstall only if you want to clear sign-in.

## Screenshots

Captured on Meta Portal (1280×800 landscape).

![Poster mode — centered cover art with title overlay](docs/screenshots/poster-mode.png)

![Detail view — synopsis, rankings, genres, and actions](docs/screenshots/detail-view.png)

![Settings — slideshow source, frame mode, filters, and shuffle](docs/screenshots/settings.png)

## License

MIT — see [LICENSE](LICENSE).
