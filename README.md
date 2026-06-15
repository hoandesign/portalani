# Portal Ani

AniList-powered anime screensaver for **Meta Portal**. Fullscreen landscape slideshow with cover art, rankings, trailers, and optional sign-in to browse your personal AniList library.

Inspired by [portal-gphotos](https://github.com/ram-nat/portal-gphotos).

**Package:** `com.portal.portalani`  
**Current version:** 0.6.9

## Features

- Cinematic slideshow with banner parallax, poster, synopsis, and genre tags
- **Personal** mode — your AniList lists (watching, planning, completed, etc.)
- **Full library** mode — browse the public catalog with season, format, and sort filters
- AniList OAuth sign-in (rate, favourite, add to list)
- All-time rank badges, community score, YouTube trailers
- Shuffle and configurable slide interval
- Offline cache of the last loaded feed
- Registers as Portal idle screensaver (`DreamService`)

## Quick start

1. Follow **[docs/SETUP.md](docs/SETUP.md)** — AniList app, Android build, and Portal deploy.
2. Build: `GRADLE_OPTS="-Xmx2g" ./gradlew assembleDebug`
3. Deploy: `bash scripts/deploy.sh --build`

## Usage on Portal

| Action | Gesture |
|--------|---------|
| Settings | Tap center of screen |
| Next slide | Swipe left or tap right edge |
| Previous slide | Swipe right or tap left edge |
| Rate / Favourite / List | Icon cluster (bottom-right of info panel) |

When the Portal sleeps, the slideshow should start automatically if screensaver registration succeeded during deploy.

## Project layout

```
portalani/
├── app/src/main/java/com/portal/portalani/
│   ├── MainActivity.kt / MainViewModel.kt   # App shell & state
│   ├── AnimeDreamService.kt                 # Portal screensaver entry
│   ├── AniListOAuthActivity.kt              # In-app OAuth WebView
│   ├── data/                                # AniList API, cache, settings
│   └── ui/                                  # Compose UI
├── scripts/deploy.sh                          # Build, install, screensaver setup
├── docs/SETUP.md                            # Full setup guide
└── local.properties.example                 # Secrets template (do not commit real file)
```

## Requirements

- **Portal:** Meta Portal family (see [supported devices](docs/SETUP.md#supported-devices)) with **Settings → Debug → ADB Enabled** and USB-C to your computer
- **Dev machine:** JDK 11+, Android SDK, `adb` (or [hzdb](https://www.npmjs.com/package/@meta-quest/hzdb))
- **AniList:** OAuth application ([developer settings](https://anilist.co/settings/developer))

## Security

- `local.properties` holds your AniList **client secret** — it is gitignored.
- OAuth tokens are stored in app private storage on the Portal.
- Use `adb install -r` (replace) when updating; **do not uninstall** unless you want to clear sign-in.

## API

Uses [AniList GraphQL](https://graphql.anilist.co) and OAuth2 authorization code flow ([docs](https://docs.anilist.co/guide/introduction)). Access tokens last about one year; sign in again when expired.

## License

MIT — see [LICENSE](LICENSE).
