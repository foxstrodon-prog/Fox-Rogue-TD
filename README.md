# Rogue TD: Dual-Front

A dual-lane tactical Tower Defense game built with Kotlin and Jetpack Compose.

## 📱 Pre-built APK Download

You can find the ready-to-install Android APK in this repository:
- **Direct Repository Download (v0.0.0.004):** [`apk/RogueTD-v0.0.0.004-debug.apk`](apk/RogueTD-v0.0.0.004-debug.apk)
- **Automated GitHub Actions:** Every commit and release automatically builds the latest APK under the repository's **Actions** tab as a downloadable artifact.

### ✨ What's New in Version 0.0.0.004:
- **Tier 3 Global Aura Connective Network**: Tier 3 towers emit visual connective laser lines and traveling energy pulse nodes connecting to all empowered towers across the grid map.
- **Radiant Organic Pathway Merging**: Clean, smooth gradient pathways shifting to radiant violet/indigo energy channels at lane junctions and intersection crossover tiles.
- **Harmonic Grid Energy Matrix**: Populating 6+ boosted floor plates activates the Harmonic Grid bonus (+20% damage, +25% income) featuring radiant concentric energy nodes and golden harmony halos.
- **Tactical Tutorial Guidance**: Bouncing directional chevron indicator on Wave 1 highlighting boosted pads for first-time placement.
- **Global Leaderboard & Offline Support**: Integrated REST backend for global high scores with seamless offline-cached leaderboard fallback.
- **Toolchain & Engine Optimizations**: Updated to AGP 9.5.0-rc02 with Java 17 compatibility, optimized visual effect pooling, and enhanced Compose render performance.

### ✨ What's New in Version 0.0.0.003:
- **In-Game Version Display in Pause Menu**: Live build version number (`BuildConfig.VERSION_NAME`) dynamically rendered in the Pause Menu header (`v0.0.0.003`) and dialog footer (`ROGUE TD • BUILD v0.0.0.003`).
- **HUD & UI Transparency**: Reduced visual occlusion with high-opacity transparent top bar (`alpha = 0.65f`). Synergies consolidated into a lightweight `🌟 (count)` dropdown.
- **Accurate Lane Targeting Hitboxes**: Eliminated discrepancies between visual range indicators and projectile triggering on lane curves and intersections.
- **Visual Sleek Heavy Tower Redesign**: Heavy Siege tower redesigned to match the sleek minimalist "thin rail" aesthetic of the primary towers.
- **Custom App Launcher Icon**: Custom thin-rail sentry turret adaptive launcher icon.
- **Omnidirectional Procedural Paths**: Dynamic pathing entering and exiting from all 4 borders (North, South, East, West) with multi-axis crossover merge spines and configurable min/max lane lengths.
- **Expanded Dev Balance Console**: Tap-to-expand unit balance cards for all towers and enemies, inline name editing, tactical identity confirmation & cross-referencing, and "Export New Defaults" button generating clean diff-only delta JSON.

## 🚀 Building from Source

To build the APK locally using Gradle:

```bash
gradle assembleDebug
```

The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```
