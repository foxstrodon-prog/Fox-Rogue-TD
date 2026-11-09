# Rogue TD: Dual-Front

A dual-lane tactical Tower Defense game built with Kotlin and Jetpack Compose.

## 📱 Pre-built APK Download

You can find the ready-to-install Android APK in this repository:
- **Direct Repository Download (v0.0.0.003):** [`apk/RogueTD-v0.0.0.003-debug.apk`](apk/RogueTD-v0.0.0.003-debug.apk)
- **Automated GitHub Actions:** Every commit and release automatically builds the latest APK under the repository's **Actions** tab as a downloadable artifact.

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
