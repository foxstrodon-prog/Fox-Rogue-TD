# Dual Front TD

A dual-lane tactical Tower Defense game built with Kotlin and Jetpack Compose.

## 📱 Pre-built Android APK & Windows PC Downloads

You can find the ready-to-run builds in this repository:
- **Direct Repository Download (v0.0.0.010 Android APK):** [`apk/Dual-Front-TD-v0.0.0.010-debug.apk`](apk/Dual-Front-TD-v0.0.0.010-debug.apk) (also mirror-saved as [`apk/game-release-v0.0.0.010.apk`](apk/game-release-v0.0.0.010.apk) and [`apk/RogueTD-v0.0.0.010-debug.apk`](apk/RogueTD-v0.0.0.010-debug.apk))
- **Portable Windows PC Launcher (Single Root Executable):** [`Dual Front TD.exe`](Dual%20Front%20TD.exe)
  - Double-click to automatically locate ADB, detect or start an emulator/device, install the APK, and launch the game in one click.
- **Archived Previous Releases (inside `apk/` folder):**
  - **v0.0.0.009:** [`apk/game-release-v0.0.0.009.apk`](apk/game-release-v0.0.0.009.apk) | [`apk/Fox-Rogue-TD-v0.0.0.009.exe`](apk/Fox-Rogue-TD-v0.0.0.009.exe)
  - **v0.0.0.008:** [`apk/game-release-v0.0.0.008.apk`](apk/game-release-v0.0.0.008.apk) | [`apk/Fox-Rogue-TD-v0.0.0.008.exe`](apk/Fox-Rogue-TD-v0.0.0.008.exe)
  - **v0.0.0.007:** [`apk/game-release-v0.0.0.007.apk`](apk/game-release-v0.0.0.007.apk) | [`apk/RogueTD-v0.0.0.007.exe`](apk/RogueTD-v0.0.0.007.exe)
  - **v0.0.0.006:** [`apk/RogueTD-v0.0.0.006-debug.apk`](apk/RogueTD-v0.0.0.006-debug.apk) | [`apk/RogueTD-v0.0.0.006.exe`](apk/RogueTD-v0.0.0.006.exe)
  - **v0.0.0.005:** [`apk/RogueTD-v0.0.0.005-debug.apk`](apk/RogueTD-v0.0.0.005-debug.apk) | [`apk/RogueTD-v0.0.0.005.exe`](apk/RogueTD-v0.0.0.005.exe)
- **Automated GitHub Actions:** Every commit and release automatically builds the latest APK under the repository's **Actions** tab as a downloadable artifact.

---

### ✨ What's New in Version 0.0.0.010:

- **Official Rebrand to "Dual Front TD"**:
  - Rebranded application name to **Dual Front TD** across manifest, strings, and build artifacts (`Dual-Front-TD-v0.0.0.010-debug.apk`).
- **Complete 39-Tower Dev-Console Coverage**:
  - Fully dynamic dev menu support for **all 39 towers** (3 Level 1 base towers, 9 Level 2 specializations, and 27 Level 3 final forms).
  - Includes family and tier filter chips, custom renaming text fields, real-time stat summaries, individual reset buttons, and granular sliders for Base Damage (5–450 DMG), Targeting Range (1.5–7.5 cells), Attack Rate (0.2–8.0/s), and Deployment Cost (40–600 Credits).
  - "Export Defaults" button generates strict diff-only delta JSON capturing only modified unit settings.
- **Fixed Canvas Orientation & Dynamic Rotating HUD Overlays**:
  - Locked `MainActivity` canvas to portrait, ensuring map grid coordinates, hitboxes, and curve bezier paths never shift or distort during physical phone rotation.
  - Dynamically rotates HUD text chips and action buttons to match device tilt angle (0°, 90°, 180°, 270°) with fluid animation.
- **Procedural Audio Synthesizer & Skins Studio**:
  - Procedural PCM audio synthesis engine (`SoundManager.kt`) with zero asset overhead and 100% copyright freedom.
  - Master, SFX, and Music volume sliders.
  - 4 selectable procedural sound profiles: *Cyber Synth, Retro 8-Bit Arcade, Heavy Acoustic Impact, and Sci-Fi Laser Grid*, with live interactive test buttons (*Kinetic, Elemental, Siege, Upgrade*).
  - 4 procedural ambient synthesizer music tracks: *Sector Zero Synth, Dual Front Pulse, Sub-Void Drone, and Mute*.
  - 7 customizable tower & map themes: *Vector Blueprint, Cyberpunk Neon, Solarized Military, Deep Void, Retro 8-Bit Arcade, Clean Minimalist, and Gold Elite*.
- **Accurate Live Combat DPS & Telemetry Reset Window**:
  - Integrated `getEffectiveDamage()`, `getEffectiveFireRate()`, and `getEffectiveDps()` calculation into tower inspection.
  - Prominent "RESET WINDOW" button on the Telemetry tab in the Dev Console to capture and diagnose tower performance over focused test windows.
- **Global Economy Tuning Sliders**:
  - Added macro sliders for overall tower fire rate multiplier (0.2x–3.0x), overall enemy health multiplier (0.5x–3.0x), overall enemy speed multiplier (0.2x–3.0x), starting credits (100–600 C), and enemies per round (0.5x–3.0x).
- **Decoupled Simulation Clock (3.5x / 5x / 10x Synchronization)**:
  - Decoupled tower weapon cooldowns from real-world wall clock milliseconds to virtual simulation time (`virtualSimTimeMs`), perfectly synchronizing weapon fire cadences with fast-forward gameplay speeds.
- **Expanded Map Grid & Smart Spawner Recommendation**:
  - Expanded map height from 14 to **16 rows** (`DEFAULT_WIDTH = 10`), extending the board upward to the HUD.
  - Smart Wave 1 placement recommendation prioritizes proximity to spawners, center column alignment, and early lane reach.
- **Windows .exe Consolidation**:
  - Root directory contains only the updated standalone [`Dual Front TD.exe`](Dual%20Front%20TD.exe). All past executables archived in `apk/`.
- **Firebase & Google Services Integration**:
  - Added `google-services.json` integration, Google Services Gradle plugin 4.5.0, and Firebase BoM 35.1.0 with Analytics and Firestore REST leaderboard transport.

---

### ✨ What's New in Version 0.0.0.009:

- **Reflective Instantiation Safety & Lifecycle Fixes**:
  - Added `@JvmOverloads` annotations to `RogueTDViewModel`, `ScoreSubmissionWorker`, and `FirestoreLeaderboardApi`.
  - Resolves runtime crash (`NoSuchMethodException: RogueTDViewModel.<init> [class android.app.Application]`) under Android's default `ViewModelProvider.AndroidViewModelFactory` and WorkManager's `WorkerFactory`.
- **Widescreen Windows Launcher Resilience**:
  - Hardened `RogueTDLauncher.cs` device serial parsing to properly handle wireless / mDNS TLS ADB devices containing whitespace or parenthesized index names.
  - Expanded APK resolution to automatically match any valid project APK pattern.

---

## 🎮 Core Gameplay Systems

- **Dual-Front Creep Assault**: Enemies spawn along dual conduits (`Lane Alpha` and `Lane Beta`), winding through all 4 map corners, merging across a shared central nexus, and converging on your Core.
- **Tactical Tower Evolution**: 3 base towers branching into 9 Level 2 specializations and 27 Level 3 advanced forms (39 total units).
- **Synergy Matrix**: Pair towers of differing elemental and tactical classes to trigger global combat bonuses (Amplifier, High Ground, Conduit matrices).
- **Deep Telemetry & Scoring**: Detailed per-run breakdown tracking enemies neutralized, total DPS output, MVP defense towers, scrap economy, and worldwide leaderboard standings.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVI / Unidirectional Data Flow with Android Architecture Components (`ViewModel`, `StateFlow`)
- **Backend / Services**: Google Services Gradle Plugin 4.5.0, Firebase BoM 35.1.0, Firestore REST, Firebase Analytics
- **Graphics**: Hardware-accelerated Compose Canvas with `withTransform` translation/rotation matrices
- **Testing**: Robolectric, JUnit 4, AndroidX Test Runner

---

## 🚀 Building from Source

To build the APK locally using Gradle:

```bash
# Build debug APK
./gradlew assembleDebug

# Compile portable Windows launcher (.exe)
./gradlew buildWindowsLauncher

# Run unit and Robolectric tests
./gradlew testDebugUnitTest

# Compile APK, Windows runner, and publish release to GitHub
./gradlew uploadGitHubRelease
```

The output artifacts will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
Dual Front TD.exe
```
Versioned distribution files:
```
apk/Dual-Front-TD-v0.0.0.010-debug.apk
apk/Dual Front TD.exe
apk/Dual-Front-TD-v0.0.0.010.exe
```
