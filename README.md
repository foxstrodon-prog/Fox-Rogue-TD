# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub.

---

## 📱 Downloads (Android APK & Windows PC)

- **Latest Android APK (`v0.0.1.003`):** [`apk/Dual-Front-TD-v0.0.1.003-debug.apk`](apk/Dual-Front-TD-v0.0.1.003-debug.apk)
- **One-Click Windows PC Launcher:** [`Dual Front TD.exe`](Dual%20Front%20TD.exe) (single executable in root)
  - Automatically detects ADB, pairs with your Android device or local emulator, installs the latest build, and launches the game.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.003

### 🔄 True Screen Orientation & HUD Counter-Rotation
- **Coordinate Decoupling:** Canvas coordinates remain fixed to eliminate touch hitbox drift, while all overlay HUD elements counter-rotate smoothly (`Modifier.rotate(-angle)` / `graphicsLayer { rotationZ }`) when you rotate your phone (0°, 90°, 180°, 270°).
- **Readable in Any Grip:** Top bullet HUD, floating squircle action bar, and floating combat damage numbers adapt smoothly without obstructing tactical playfield lanes.

### 🔲 Floating Squircle Upgrade Action Bar
- **Tactical Ergonomics:** Completely eliminated the heavy bottom sheet drawer in favor of sleek 56dp x 56dp floating squircle buttons floating 32dp above the device gesture bar.
- **Neon Archetype Accents:** Procedural archetype glyphs with glowing neon borders (Amber Kinetic, Violet Elemental, Crimson Siege, Gold Stellar) and minimal cost badges (e.g. `80c`, `140c`, `180c`, `210c`).
- **Clean One-Handed Play:** Instant responsiveness with spring-scale tap animations and zero text clutter obscuring the map.

### 🎬 6 In-App 30.00-Second Cinematics & DaVinci Resolve Timelines
- **Deterministic Replay Engine:** Added 6 scripted, frame-accurate (1,800 frames @ 60 FPS) in-app cinematic scenarios directly accessible from the **DEV MENU > CINEMA (6)** tab:
  1. **Cin 01: Boss Melt:** Wave 20 Obsidian Titan shredded by 4 Apex Overclocked Turrets *(Thumbnail @ 23.40s)*.
  2. **Cin 02: Bullet Hell Swarm:** 120+ Swarm creeps eradicated by Flak cannons and Singularity pulses *(Thumbnail @ 18.20s)*.
  3. **Cin 03: Booster Warp Showcase:** Turret instantly teleports across Teal Conduit booster pads with automated tactical swaps *(Thumbnail @ 15.30s)*.
  4. **Cin 04: Elemental Cascade:** Frost Nova slow chains into Void Rift singularity detonations *(Thumbnail @ 21.10s)*.
  5. **Cin 05: Kinetic Gatling Overclock:** Dual ballistic streams spinning up into hyper-velocity fire *(Thumbnail @ 16.50s)*.
  6. **Cin 06: Evolution Montage:** Rapid morph progression from Tier 1 Base through Tier 4 Apex Corona *(Thumbnail @ 25.00s)*.
- **DaVinci Resolve Studio Timelines:** 6 matching CMX 3600 EDL timelines in `Youtube Team Assets/Timelines/` ready for zero-cut drop-in delivery, paired with high-CTR packaging suites in `Youtube Team Assets/Thumbnails/`.

### 🛡️ Platform Hardening & Leaderboard Roundtrip
- **Firebase Authentication & REST Security:** Production SHA-1 certificate fingerprint header (`X-Android-Cert: 174775252333dc2e8159f5ac6de1019661c3e897`) and package verification (`com.aistudio.roguetd.xtkp`).
- **Offline Atomic Queue:** Automatic retry queue with exponential backoff and granular GCP RPC error diagnostics.
- **Automated Test Suite:** 76/76 automated domain and Robolectric unit tests passing (100% pass rate).

---

## 📜 Prior Patch Highlights

### Version 0.0.1.002
- **52-Tower Evolution Matrix:** Introduced Class 4: Stellar Cosmic (`STELLAR_COSMIC`) with 9 final form archetypes (Supernova Pulsar, Singularity Bastion, Graviton Lance) and cosmic beam piercing.
- **Booster Pad Teleportation:** 6 color-paired booster pads with instant tactical swaps, 750ms cooldown, and +3% scrap bounty per occupied pad.
- **Audio Polish & Low-Pass Filtering:** Smooth 600–1000ms gradual fades and mellow ambient synthesizer filtering (<650Hz).
- **Rebalanced Economy:** Tuned early-game scrap bounties across waves 1–100.

### Version 0.0.1.001
- **Spatial Hash Grid Collision Engine (`SpatialHashGrid.kt`):** Replaced brute-force projectile checks with a uniform spatial partitioning grid, locking smooth 60/120 FPS performance during massive swarm waves.
- **Modular UI Refactor:** Separated HUD, upgrade bar, and menus into single-responsibility components.
- **39-Tower Hierarchy:** Completed initial 3 -> 9 -> 27 branching upgrade progression across Kinetic, Elemental, and Siege families.
- **Procedural Level 1–4 Visual Upgrade Cues:** Procedural chassis, stabilizer wings, focus barrels, and Apex radiant coronas.

### Version 0.0.0.013
- **Pause Menu Audio Attenuation:** Music smoothly drops -6dB when paused and resumes instantly when returning to the fight.
- **Interactive Sound Auditioning:** Preview gunfire sounds and background music tracks directly in the settings menu.
- **Extended Procedural Music:** 45-second looping synthesizer compositions with smooth tapered transitions.
- **Leaderboard Resilience:** Resolved API token authentication on score uploads and added offline retry handling.

### Version 0.0.0.012
- **Screen Rotation & Adaptive Layout:** Locked the underlying canvas coordinates to prevent hitbox drift while dynamically rotating HUD text chips and menus across portrait and landscape.
- **Focus Loss Audio Termination:** Sound immediately cuts when switching apps or locking the screen, eliminating ghost audio loops.

### Version 0.0.0.010
- **Official Rebrand to Dual Front TD:** Updated application identity, standalone Windows launcher (`Dual Front TD.exe`), and improved board verticality (16-row layout).

---

## 🎮 Core Gameplay Systems

- **Dual-Front Creep Assault:** Creeps spawn simultaneously along dual conduits (`Lane Alpha` and `Lane Beta`), winding through all 4 corners, merging at a central nexus, and converging on your Core.
- **Tactical Evolution Tree:** 4 base towers branching into 12 Level 2 specializations and 36 Level 3 advanced forms (52 total units).
- **Compound Synergy Matrix:** Stacking tower families and classes activates board-wide passive buffs.
- **Detailed Run Telemetry:** Per-run breakdowns tracking enemies neutralized, total DPS output, MVP defense towers, scrap economy, and worldwide leaderboard standings.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.2.10
- **UI Framework:** Jetpack Compose & Material 3
- **Architecture:** MVI / Unidirectional Data Flow with Android Architecture Components (`ViewModel`, `StateFlow`)
- **Graphics:** Hardware-accelerated Compose Canvas with custom coordinate transform matrices
- **Testing:** Robolectric, JUnit 4, AndroidX Test Runner (76/76 passing)
- **Target Platform:** Android (Target SDK 36, Android 16 ready, 16 KB page-size compatible) + Windows Companion Runner

---

## 🚀 Building from Source

```bash
# Build debug APK
./gradlew assembleDebug

# Compile portable Windows companion launcher (.exe)
./gradlew buildWindowsLauncher

# Run full automated test suite
./gradlew testDebugUnitTest
```

Build outputs:
- Android APK: `app/build/outputs/apk/debug/app-debug.apk` (and staged under `apk/Dual-Front-TD-v0.0.1.003-debug.apk`)
- Windows Companion: `Dual Front TD.exe` (and staged under `apk/Dual-Front-TD-v0.0.1.002.exe`)
