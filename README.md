# Portal Ani

AniList-powered anime screensaver for **Meta Portal**. Fullscreen landscape slideshow with cover art, rankings, trailers, and optional AniList sign-in.

Inspired by [portal-gphotos](https://github.com/ram-nat/portal-gphotos).

**Package:** `com.portal.portalani`  
**Version:** 0.8.2

## Features

- **Poster mode** (default) — large centered poster; tap to flip and reveal full details
- **Informative mode** — poster plus synopsis, genres, and actions side by side
- **Personal** — your AniList lists (watching, planning, completed, …)
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
| **More info** (poster mode) | Tap the poster — flips to informative layout; tap again to collapse |

On first launch, subtle on-screen hints walk through swipe, hold-for-settings, and tap-poster. They fade away after you try each gesture or after a short pause.

## Security

- `local.properties` holds your AniList client secret (gitignored).
- OAuth tokens live in app private storage on the Portal.
- Use `adb install -r` when updating; uninstall only if you want to clear sign-in.

## License

MIT — see [LICENSE](LICENSE).
