# Rogue TD: Dual-Front

A dual-lane tactical Tower Defense game built with Kotlin and Jetpack Compose.

## 📱 Pre-built Android APK & Windows PC Downloads

You can find the ready-to-run builds in this repository:
- **Direct Repository Download (v0.0.0.006 Android APK):** [`apk/RogueTD-v0.0.0.006-debug.apk`](apk/RogueTD-v0.0.0.006-debug.apk)
- **Portable Windows PC Launcher (v0.0.0.006 Widescreen Runner):** [`RogueTD-v0.0.0.006.exe`](RogueTD-v0.0.0.006.exe) (also mirror-saved in [`apk/RogueTD-v0.0.0.006.exe`](apk/RogueTD-v0.0.0.006.exe))
  - Double-click to automatically locate ADB, detect or start an emulator/device, install the APK, lock the display to **Widescreen Mode (Landscape)**, and launch the game in one click.
- **Previous Releases:**
  - **v0.0.0.005:** [`apk/RogueTD-v0.0.0.005-debug.apk`](apk/RogueTD-v0.0.0.005-debug.apk) | [`RogueTD-v0.0.0.005.exe`](RogueTD-v0.0.0.005.exe)
  - **v0.0.0.004:** [`apk/RogueTD-v0.0.0.004-debug.apk`](apk/RogueTD-v0.0.0.004-debug.apk)
- **Automated GitHub Actions:** Every commit and release automatically builds the latest APK under the repository's **Actions** tab as a downloadable artifact.

---

### ✨ What's New in Version 0.0.0.006:

- **Multi-Dimensional Combat Matrix (`DamageCalculator`)**:
  - Implemented full tactical rock-paper-scissors elemental affinities (`RED` > `GREEN` > `YELLOW` > `RED`), dealing +25% advantage damage or suffering -20% resist penalty.
  - Deep geometric silhouette synergy: Kinetic towers match Circle enemies, Elemental Void matches Triangles, and Heavy Siege crushes Squares (+35% match bonus vs -20% mismatch resistance).
- **Heading-Oriented Creep Motion**:
  - Triangle striker enemies and directional drones dynamically rotate to their true path tangent heading (`atan2(dy, dx)`) during segment interpolation, eliminating static East-facing sprites on curved lanes.
- **Combat Range & Visualizer Perfect Synchronization**:
  - Tower selection range circles in `GameCanvas` are now dynamically synchronized with `globalRangeBonus` from active sniper and orbital kinetic aura upgrades, ensuring 1:1 visual fidelity with Euclidean combat targeting (`dist <= effectiveRange + enemy.type.hitRadius`).
- **Archetype Vector Resolution & Tier-1 Ring Alignment**:
  - `TowerInstance.affinity` and `tier1Vector` now evaluate `archetype?.vector` before falling back to base tower type defaults, ensuring custom archetypes correctly project their designated vector aura rings (Red = Offensive, Green = Defensive, Yellow = Support).
- **Floating Combat Text Engine Overhaul**:
  - Resolved a simulation loop bug where stale state snapshots were erasing combat damage numbers mid-tick.
  - Refined floating text thresholds to eliminate routine number clutter while emphasizing high-impact events: critical strikes (`CRIT!`), elemental weak points (`WEAK!`), shape resists (`RESIST`), and devastating blasts (>60 damage).
- **Minimal Top HUD Layout Stabilization**:
  - Replaced individual child `graphicsLayer` rotation with stable layout measurements in `MinimalTopHud`, eliminating element overflow, clipping, and button overlap across device rotations.
- **Lifecycle & Background Pause Protection**:
  - Automatically invokes `viewModel.openPauseMenu()` in `MainActivity.onStop()`, ensuring matches are paused when the device screen locks or the application is backgrounded, eliminating unintended loss or battery drain.
- **Comprehensive Automated Test Suite**:
  - 35 automated unit and Robolectric tests validating damage matrices, range synchronization, Euclidean hitboxes, lane continuity, and lifecycle state management.

---

### ✨ What's New in Version 0.0.0.005:

- **Portable Windows PC Runner & Default Widescreen Launcher**: Added a standalone, zero-dependency Windows executable ([`RogueTD-v0.0.0.005.exe`](RogueTD-v0.0.0.005.exe)) that automatically locates ADB, detects active devices or boots the local AVD emulator, installs/updates the game, locks orientation into **Widescreen Landscape Mode**, and launches the game in one click. Integrated directly into Gradle via `./gradlew buildWindowsLauncher`.
- **Full-Map 4-Corner Procedural Lane Generation & Guaranteed Continuity**: Overhaul of the map generator guaranteeing that generated creep lanes route through and touch all four outer corners of the map (`Top-Left`, `Top-Right`, `Bottom-Left`, and `Bottom-Right`). Eliminates perimeter dead zones and replaces legacy leg concatenation with strict step-by-step obstacle and path avoidance, ensuring 100% continuous 4-way orthogonal Manhattan adjacency (`|Δx| + |Δy| == 1`) without gaps, diagonal jumps, or creep teleportation.
- **Display-Aware Turret Alignment & Barrel Aiming**:
  - Reconciled device accelerometer tilt angles with Android display window rotation (`Surface.ROTATION_*`), ensuring tower chassis, radiant halos, and status badges remain perfectly upright across portrait and landscape orientations without sideways double-rotation.
  - Isolated chassis counter-rotation from weapon barrel transforms so kinetic rotary cannons, elemental emitters, and heavy dual-rails aim precisely along true target trajectories in screen coordinates.
- **In-App GitHub Release Auto-Updater**: Zero-friction sideload updates outside Google Play. The game checks the GitHub Releases API for published release tags, displays an adaptive in-game progress banner, manages background downloads via Android's native `DownloadManager` with concurrency guards, and triggers prompt-based installation via `FileProvider` (`REQUEST_INSTALL_PACKAGES`) with automatic internal storage fallback.
- **Global Online Leaderboard (Ktor REST via Dreamlo)**: Worldwide competition subsystem powered by a coroutine-native Ktor HTTP client. Automatically submits post-run tactical scores with robust URL-decoding for all metadata attributes (`scrap`, `uuid`, `waves`, `dev`), manages persistent anonymous commander callsigns (`Commander-<UUID>`), features top-50 ranked standings with medal displays (🥇, 🥈, 🥉), developer tuning indicators (`[DEV]`), and resilient offline-cached fallback.
- **Aspect-Ratio Preserved Game Canvas & Letterbox Coordinate Mapping**: Centralized canvas translation preserves grid proportions across phones, tablets, and foldables. Touch inputs, interactive tower placements, and floating combat text share unified reciprocal coordinate transformations, guaranteeing pixel-perfect alignment.
- **Ergonomic Responsive Tactical HUD**: Window configuration-driven layout switching docks the `TowerBuildUpgradeDrawer` as a full-width bottom sheet in portrait mode and transitions it into a scrollable 340dp side drawer in landscape mode.
- **Security & Battery Hardening**:
  - Scoped `OrientationEventListener` to `onStart()` / `onStop()` lifecycles to eliminate sensor battery drain when the application is backgrounded.
  - Eliminated hardcoded private administrative credentials from client code.
  - Replaced automatic installation popups with user-confirmed install actions and download debouncing to prevent interrupting live gameplay waves.
  - Restricted `FileProvider` path definitions in `file_paths.xml` to least-privilege download directories (`<files-path>` and `<external-files-path>`).
- **Comprehensive Automated Test Suite**: 29 automated Robolectric unit tests covering 4-corner path continuity, core reachability, coordinate transforms, leaderboard serialization, and update lifecycle states.

---

### ✨ What's New in Version 0.0.0.004:
- **Tier 3 Global Aura Connective Network**: Tier 3 towers emit visual connective laser lines and traveling energy pulse nodes connecting to all empowered towers across the grid map.
- **Radiant Organic Pathway Merging**: Clean, smooth gradient pathways shifting to radiant violet/indigo energy channels at lane junctions and intersection crossover tiles.
- **Harmonic Grid Energy Matrix**: Populating 6+ boosted floor plates activates the Harmonic Grid bonus (+20% damage, +25% income) featuring radiant concentric energy nodes and golden harmony halos.
- **Tactical Tutorial Guidance**: Bouncing directional chevron indicator on Wave 1 highlighting boosted pads for first-time placement.
- **Global Leaderboard & Offline Support**: Integrated REST backend for global high scores with seamless offline-cached leaderboard fallback.
- **Toolchain & Engine Optimizations**: Updated to AGP 9.5.0-rc02 with Java 17 compatibility, optimized visual effect pooling, and enhanced Compose render performance.

---

### ✨ What's New in Version 0.0.0.003:
- **In-Game Version Display in Pause Menu**: Live build version number (`BuildConfig.VERSION_NAME`) dynamically rendered in the Pause Menu header (`v0.0.0.003`) and dialog footer (`ROGUE TD • BUILD v0.0.0.003`).
- **HUD & UI Transparency**: Reduced visual occlusion with high-opacity transparent top bar (`alpha = 0.65f`). Synergies consolidated into a lightweight `🌟 (count)` dropdown.
- **Accurate Lane Targeting Hitboxes**: Eliminated discrepancies between visual range indicators and projectile triggering on lane curves and intersections.
- **Visual Sleek Heavy Tower Redesign**: Heavy Siege tower redesigned to match the sleek minimalist "thin rail" aesthetic of the primary towers.
- **Custom App Launcher Icon**: Custom thin-rail sentry turret adaptive launcher icon.
- **Omnidirectional Procedural Paths**: Dynamic pathing entering and exiting from all 4 borders (North, South, East, West) with multi-axis crossover merge spines and configurable min/max lane lengths.
- **Expanded Dev Balance Console**: Tap-to-expand unit balance cards for all towers and enemies, inline name editing, tactical identity confirmation & cross-referencing, and "Export New Defaults" button generating clean diff-only delta JSON.

---

## 🎮 Core Gameplay Systems

- **Dual-Front Creep Assault**: Enemies spawn along dual conduits (`Lane Alpha` and `Lane Beta`), winding through all 4 map corners, merging across a shared central nexus, and converging on your Core.
- **Tactical Tower Evolution**: 6 base tower archetypes, each supporting 3 distinct Tier-1 branching paths and 9 specialized Final Forms (54 total final forms).
- **Synergy Matrix**: Pair towers of differing elemental and tactical classes to trigger global combat bonuses (Amplifier, High Ground, Conduit matrices).
- **Deep Telemetry & Scoring**: Detailed per-run breakdown tracking enemies neutralized, total DPS output, MVP defense towers, scrap economy, and worldwide leaderboard standings.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVI / Unidirectional Data Flow with Android Architecture Components (`ViewModel`, `StateFlow`)
- **Networking**: Ktor 3.0.3 with OkHttp Engine
- **Graphics**: Hardware-accelerated Compose Canvas with `withTransform` translation/rotation matrices
- **Testing**: Robolectric, JUnit 4, AndroidX Test Runner

---

## 🚀 Building from Source

To build the APK locally using Gradle:

```bash
# Build debug APK
./gradlew assembleDebug

# Compile portable Windows widescreen launcher (.exe)
./gradlew buildWindowsLauncher

# Run unit and Robolectric tests
./gradlew testDebugUnitTest
```

The output artifacts will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
RogueTD-v0.0.0.006.exe
```
Or find the versioned distribution files in:
```
apk/RogueTD-v0.0.0.006-debug.apk
apk/RogueTD-v0.0.0.006.exe
```
