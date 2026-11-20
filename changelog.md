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
