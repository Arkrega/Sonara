---v1.5.1
# SONARA 1.5.1

Crystal-clear transparent Liquid Glass tuning and complete app-wide Liquid Glass coverage across every surface.

## Highlights
- Re-tuned Liquid Glass clarity with ultra-light micro-blur (1.5dp-3.5dp), crystal-transparent glass tinting, sharp edge lens refraction, and luminous specular rims so background content stays crisp and clearly visible
- Extended Liquid Glass across the entire app when enabled: TopAppBar and status badges, floating bottom dock with zero-blur chromatic droplet, navigation rail, mini-player, all player controls and dock buttons, live lyrics strip, lyrics/queue panels, search bar, filter/choice chips, settings groups, integration cards, release notes, modal bottom sheets, action grids, menus, FABs, switches, and all dialogs
- Improved contrast on Liquid Glass Play button, primary buttons, and selected chips while preserving full monochrome flat UI whenever Liquid Glass is toggled off

---v1.5.0
# SONARA 1.5.0

Real-time Liquid Glass UI powered by AndroidLiquidGlass and PrismalAGSL with an instant Appearance toggle.

## Highlights
- Added live Liquid Glass optical refraction, blur, chromatic dispersion, and specular highlights powered by Kyant0/AndroidLiquidGlass and styropyr0/PrismalAGSL across the bottom navigation dock, mini-player, player buttons, audio/video pill, lyrics/queue panels, switches, FABs, and dialogs
- Upgraded the floating bottom navigation bar with an interactive draggable Liquid Glass droplet using PrismalGlassBottomTabs
- Added a Liquid Glass on/off toggle under Appearance settings (enabled by default) with instant live switching and automatic frosted fallback on older Android versions

---v1.4.2
# SONARA 1.4.2

Smart 7-day automatic cache cleanup and dedicated music video disk caching for zero-buffering replays.

## Highlights
- Added automatic cache expiration (default: 7 days, configurable to Off / 3 / 7 / 14 / 30 days) under Storage settings that removes unused cached songs, videos, and images while keeping frequently played tracks and offline downloads untouched
- Added a dedicated Music Video disk cache (512 MB default, configurable up to 4 GB) so replayed music videos start immediately without re-buffering
- Added one-tap Clear all cache and Clear video cache actions in Storage settings alongside existing song and image cache controls

---v1.4.1
# SONARA 1.4.1

Native in-player music video playback for YouTube Music video tracks with instant Audio/Video switching and adaptive quality control.

## Highlights
- Video tracks now play smoothly inside a rounded 16:9 widescreen frame in the player instead of staying frozen on the thumbnail
- Added a quick Audio / Video segmented toggle right inside the player so you can switch between video and album art anytime without interrupting audio playback
- Added Play music videos toggle and Video quality selector (Auto 720p Wi-Fi / 480p cellular, 360p, 480p, 720p, 1080p) under Player settings
- Automatically pauses and releases video decoding when minimized, in Lyrics/Queue view, or when the screen turns off while keeping background audio uninterrupted

---v1.4.0
# SONARA 1.4.0

Full SONARA rebrand across the codebase, instant history tracking for every play, and two-way YouTube Music personalization.

## Highlights
- Completed full internal rebrand from legacy Metrolist packages, styles, widgets, resources, and shortcuts to SONARA
- Every played song now enters History immediately even when skipped or not finished, while still updating actual listening duration when playback completes
- Added automatic two-way listen history sync with YouTube Music when signed in to a Google account
- Upgraded Home personalization across Quick Picks, Daily Discover, Keep Listening, Similar Recommendations, and Community Playlists so recommendations adapt directly from recent plays both locally and with Google login

---v1.3.4
# SONARA 1.3.4

Updates now explain themselves, and going offline is no longer a mystery.

## Highlights
- Opening SONARA without a connection shows a notice, plus a quiet Offline chip until the network is back
- The update chip asks for the install permission first, with instructions and a shortcut to the setting
- The download now shows real progress, checksum verification, and clear failures with a retry
- Songs already downloaded keep playing while offline

---v1.3.3
# SONARA 1.3.3

The player's home on the web now lives at sonara.my.id.

## Highlights
- About opens the official site at https://sonara.my.id
- The site is now available in Indonesian and English
- Version bump only; no playback, library, or layout changes

---v1.3.2
# SONARA 1.3.2

The website moved, so the in-app links follow it.

## Highlights
- About now opens the new site on Vercel
- The update sheet keeps downloading and installing inside the app
- Version bump only; no playback, library, or layout changes

---v1.3.1
# SONARA 1.3.1

Bahasa Indonesia, from the first launch screen to the update prompt.

## Highlights
- Full Indonesian translation: onboarding, Profile, Mood & genres, About and support, Reverb, playback speed, and the new update flow all speak Bahasa Indonesia now
- Pick it in Settings -> Content -> App language, or let the app follow an Indonesian system language
- Version bump only; no playback, library, or layout changes

---v1.3.0
# SONARA 1.3.0

Updates now find you inside the app, once a day, and install without a detour.

## Highlights
- New **New Update** chip in the top right of Home appears as soon as a newer release exists; tap it to see what changed
- The update prompt shows at most **once a day**, so a dismissal is respected until tomorrow
- Updates download and install straight from the app: the APK is verified against its SHA-256 checksum, then the system installer opens — no browser, no GitHub page
- Tapping the update notification opens the same in-app sheet instead of a web link
- Download problems are reported in-app with a clear message instead of silently bouncing to a browser

---v1.2.0
# SONARA 1.2.0

Support the project straight from the app, with a simpler About page behind it.

## Highlights
- About now offers **Support the developer**: a QRIS code you can scan with any bank or e-wallet app (GoPay, OVO, DANA, ShopeePay, BCA, Mandiri, and more)
- About carries a single credit — the Discord server and Telegram channel links are gone, along with the old coffee link
- No playback, library or playback-UI changes in this release

---v1.1.1
# SONARA 1.1.1

Maintenance release that turns the Last.fm integration on.

## Highlights
- Last.fm scrobbling, now playing and love/unlove are wired to the app credentials, so the integration works out of the box
- Version bump only: no UI or playback changes since 1.1.0

---v1.1.0
# SONARA 1.1.0

Polish release for the SONARA identity: a cleaner launch, a redesigned Mood & genres wall, and account actions moved into a dedicated Profile tab.

## Highlights
- Intro shortened from 5.5s to 3.2s and the Home screen no longer flashes before it starts
- Mood & genres rebuilt as artwork cards with generated visuals instead of plain colour boxes
- New Profile tab in the navigation dock; the top bar on Home is now just the wordmark
- Opening Search no longer pops the keyboard up automatically
- Simpler account sheet: the duplicate Settings entry is gone (Settings lives in Profile)
- Home keeps its Mood & genres section even when the explore feed omits it
- Quieter first launch: SQLite PRAGMA settings no longer log an error on every cold start

---v1.0.0
# Welcome to SONARA 1.0.0

Initial stable release of SONARA — an independently branded, high-performance Android music player.

## Highlights
- Independent application identity (`com.arkrega.sonara`)
- Clean, stable YouTube Music playback engine powered by AndroidX Media3
- Full synchronized lyrics support
- Offline music caching and downloads
- Equalizer and advanced audio processing
- Dedicated update channel with SHA-256 integrity verification
- Modern Material 3 user interface
