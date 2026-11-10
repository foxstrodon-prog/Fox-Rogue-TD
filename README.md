# Dual Front TD

A dual-lane tactical Tower Defense game built with Kotlin and Jetpack Compose.

## 📱 Pre-built Android APK & Windows PC Downloads

You can find the ready-to-run builds in this repository:
- **Direct Repository Download (v0.0.0.012 Android APK):** [`apk/Dual-Front-TD-v0.0.0.012-debug.apk`](apk/Dual-Front-TD-v0.0.0.012-debug.apk) (also mirror-saved as [`apk/game-release-v0.0.0.012.apk`](apk/game-release-v0.0.0.012.apk) and [`apk/RogueTD-v0.0.0.012-debug.apk`](apk/RogueTD-v0.0.0.012-debug.apk))
- **Portable Windows PC Launcher (Single Root Executable):** [`Dual Front TD.exe`](Dual%20Front%20TD.exe)
  - Double-click to automatically locate ADB, detect or start an emulator/device, install the APK, and launch the game in one click.
- **Google Play "What's New" Release Notes:** [`GPReadme.md`](GPReadme.md) (formatted for Google Play Store updates with recent patch history)
- **YouTube Release Description & Timestamps:** [`YT_README.md`](YT_README.md) (formatted for video showcases with timestamps and patch notes)
- **Archived Previous Releases (inside `apk/` folder):**
  - **v0.0.0.011:** [`apk/Dual-Front-TD-v0.0.0.011-debug.apk`](apk/Dual-Front-TD-v0.0.0.011-debug.apk) | [`apk/Dual-Front-TD-v0.0.0.011.exe`](apk/Dual-Front-TD-v0.0.0.011.exe)
  - **v0.0.0.010:** [`apk/Dual-Front-TD-v0.0.0.010-debug.apk`](apk/Dual-Front-TD-v0.0.0.010-debug.apk) | [`apk/Dual-Front-TD-v0.0.0.010.exe`](apk/Dual-Front-TD-v0.0.0.010.exe)
  - **v0.0.0.009:** [`apk/game-release-v0.0.0.009.apk`](apk/game-release-v0.0.0.009.apk) | [`apk/Fox-Rogue-TD-v0.0.0.009.exe`](apk/Fox-Rogue-TD-v0.0.0.009.exe)
  - **v0.0.0.008:** [`apk/game-release-v0.0.0.008.apk`](apk/game-release-v0.0.0.008.apk) | [`apk/Fox-Rogue-TD-v0.0.0.008.exe`](apk/Fox-Rogue-TD-v0.0.0.008.exe)
  - **v0.0.0.007:** [`apk/game-release-v0.0.0.007.apk`](apk/game-release-v0.0.0.007.apk) | [`apk/RogueTD-v0.0.0.007.exe`](apk/RogueTD-v0.0.0.007.exe)
  - **v0.0.0.006:** [`apk/RogueTD-v0.0.0.006-debug.apk`](apk/RogueTD-v0.0.0.006-debug.apk) | [`apk/RogueTD-v0.0.0.006.exe`](apk/RogueTD-v0.0.0.006.exe)
  - **v0.0.0.005:** [`apk/RogueTD-v0.0.0.005-debug.apk`](apk/RogueTD-v0.0.0.005-debug.apk) | [`apk/RogueTD-v0.0.0.005.exe`](apk/RogueTD-v0.0.0.005.exe)
- **Automated GitHub Actions:** Every commit and release automatically builds the latest APK under the repository's **Actions** tab as a downloadable artifact.

---

### ✨ What's New in Version 0.0.0.012:

- **Leaderboard Submission Authorization Fix**:
  - Resolved Google Cloud package-restricted API key rejection (`SECURITY_REJECTED` / HTTP 403 Forbidden) by appending `?key=$apiKey` to REST document URLs and attaching the `X-Android-Package: com.aistudio.roguetd.xtkp` request header.
  - Hardened `ScoreSubmissionWorker` to retry pending scores up to 5 times rather than dropping un-uploaded player runs on network or authorization errors.
- **Display Rotation UI Refactoring & Text Layout**:
  - **Top HUD**: Expanded margin padding (10dp horizontal x 6dp vertical) to prevent text/chip clipping upon rotation; reformatted the currency display to stack the Diamond symbol vertically above the numeric scrap count to conserve horizontal width.
  - **Bottom Upgrade Menus & Drawers**: Dynamically reorients across portrait and landscape. In landscape orientation, `BuildTowerDrawer` and `BranchingTowerInspector` stack action cards vertically in full-width rows inside the right sidebar with vertical scrolling, ensuring stats, costs, and buttons remain fully legible and clickable.
- **Settings Menu Consolidation & Dev Menu Cleanup**:
  - Centralized all audio controls, volume sliders, track selector, gunfire audio testing, skin previews, map themes, and procedural sound profiles into a unified Settings Hub inside the in-game Pause Menu.
  - Stripped all redundant audio sliders and visual skin pickers from the Dev Balance Console, keeping it strictly dedicated to gameplay balance values and unit telemetry.
- **Instant Background & Focus Audio Termination**:
  - Immediately cuts all audio streams the instant the app transitions out of active focus (task view, Home gesture, minimization, screen lock).
  - Eliminates lingering background music or trailing gunfire/explosion sound effects once focus is lost.
  - Seamless, glitch-free audio resume when returning to active gameplay.
- **Extended Music Track Duration & Seamless Looping**:
  - Eliminated abrupt 5-second track cycling; music per track now targets between 45 seconds and 1 minute 15 seconds (60s default cycle).
  - Procedural synthesizer tracks loop smoothly and transition cleanly without jarring cuts or clicks.
- **Dedicated Upgrade Sound Volume Slider**:
  - Independent volume slider dedicated specifically to tower upgrade sound effects, separating upgrade audio from combat gunfire and master SFX channels.
  - Exposed directly in the Pause Menu Audio Customization Studio.
- **Turret Skin Visual Preview Thumbnails**:
  - Real-time inline sprite thumbnails rendered next to each of the 5 skin slots in the Pause Menu, enabling players to inspect chassis designs and firing flourishes before committing to a selection.
- **Map & Lane Theme Strict Contrast Audit**:
  - Enforced strict visual separation across all 5 map themes: Path 1 (Alpha), Path 2 (Beta), and their intersection zones now use distinct, high-contrast tri-color palettes with zero shared or mimicking hues.
  - Intersection tile glow dynamically synchronizes with the theme's unique junction accent color.
- **YouTube Release Documentation Artifact (`YT_README.md`)**:
  - Automated release pipeline deliverable generating YouTube-ready descriptions with chapters/timestamps, patch notes, and GitHub referral links.

- **Complete Sound System Overhaul & Audio Architecture**:
  - **Music Player Track Switching Fix**: Fixed audio buffer lock-in so selecting tracks in the UI immediately swaps the active playback stream seamlessly.
  - **Dynamic Combat Ducking**: Implemented dynamic volume ducking on the background music bus during heavy turret firing and explosions to prevent audio clipping.
  - **Pause Menu Attenuation & Real-Time Slider Linking**: Background music automatically drops to 35% attenuation upon pause; adjusting the volume slider while paused provides immediate real-time auditory feedback.
  - **Rolling Playlist Support**: 5 background music tracks (*Sector Zero Synth, Dual Front Pulse, Sub-Void Drone, Hyper-Grid Ambient, and Mute*) cycling smoothly.
  - **Turret Gunfire Audio Restoration & Voice Concurrency Caps**: Restored firing sound triggers across all 3 turret archetypes (Kinetic, Elemental, Siege) with 65ms rate limiting to prevent volume blow-out.
  - **Turret SFX Variation Testing Slots**: 5 gunfire audio candidate slots per turret type (15 total procedural SFX variants) with live preview test triggers.
- **Aesthetic Lab & Visual Skin Pipelines**:
  - **5 Turret Visual Skin Slots**: Default Production mesh plus 4 experimental skins (*Vector Minimal, Cyber Neon, Heavy Industrial, Void Glyph*) with distinct geometric silhouettes and animated firing flourishes.
  - **5 Map & Lane Environment Theme Slots**: Default Cyber Grid plus 4 procedural biome palettes (*Neon Grid, Solarized Desert, Deep Void, Obsidian Magma*) altering conduit lines, background styling, and boosted tile glows.
  - **Pause Menu Customization Studio**: Elevated sound and skin selection out of the hidden Dev Menu into an accessible, player-facing Pause Menu subsection.
  - **Delta-State Balance & Asset Export**: Corrected boolean serialization in diff-only delta JSON export.
- **Google Play Store Release Documentation**:
  - Created [`GPReadme.md`](GPReadme.md) formatted specifically for Google Play Store updates ("What's New") showing a rolling window of recent patches with redirection to GitHub for older changelogs.
- **Release Verification & Governance Hardening**:
  - Codified the Lead Developer Manual Release Gate, ensuring all release candidate artifacts are tested personally before publishing.

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
apk/Dual-Front-TD-v0.0.0.012-debug.apk
apk/Dual Front TD.exe
apk/Dual-Front-TD-v0.0.0.012.exe
```
