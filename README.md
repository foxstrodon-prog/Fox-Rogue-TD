# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub.

---

## 📱 Downloads (Android APK & Windows PC)

- **Latest Android APK (`v0.0.1.004`):** [`apk/Dual-Front-TD-v0.0.1.004-debug.apk`](apk/Dual-Front-TD-v0.0.1.004-debug.apk)
- **One-Click Windows PC Launcher:** [`Dual Front TD.exe`](Dual%20Front%20TD.exe) (single executable in root)
  - Automatically detects ADB, pairs with your Android device or local emulator, installs the latest build, and launches the game.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.004

### 🚀 CI/CD Pipeline & GitHub Actions Hardening
- **Node.js 24 & Action Modernization:** Migrated `actions/setup-java` to `@v5` and `gradle/actions/setup-gradle` to `@v4` (targeting native Node.js 24 runners and resolving upstream Node 20 deprecation notices).
- **Graceful Cache Resilience:** Upgraded Gradle cache integration to prevent build interruptions during transient GitHub Actions cache service outages or HTTP 400/503 responses.
- **Idempotent Release Pipeline & Concurrency Guards:** Added workflow concurrency groups (`group: release-${{ github.ref_name }}`) and resilient release upload logic, eliminating parallel release creation collisions when pushing branches and tags simultaneously.
- **Runner Pin Stability:** Pinned build runner environment to `ubuntu-24.04` LTS to ensure deterministic builds and avoid automated OS image shift warnings.

---

## 📜 Prior Patch Highlights

### Version 0.0.1.003
- **True Screen Orientation & HUD Counter-Rotation:** Coordinate decoupling keeps tactical game canvas fixed, while top Bullet HUD (`HudTopBar.kt`), floating squircle action bar (`TowerUpgradeBar.kt`), and combat damage numbers smoothly counter-rotate (`Modifier.rotate(-angle)` / `graphicsLayer`) so all text and metrics stay readable in 0°, 90°, 180°, and 270°.
- **Floating Squircle Upgrade Action Bar:** Replaced heavy bottom drawer sheet with sleek 56dp x 56dp squircle buttons floating 32dp above gesture bar. Features glowing neon borders, minimal cost badges (e.g. `80c`, `140c`, `180c`, `210c`), spring scale tap feedback, and zero multi-paragraph clutter.
- **6 In-App 30.00-Second Scripted Cinematics:** Built-in deterministic simulation engine running at locked 60 FPS (1,800 frames per scenario) accessible via the DEV menu CINEMA tab.
- **DaVinci Resolve Studio Timelines & Viral Packaging:** 6 matching CMX 3600 EDL timelines in `Youtube Team Assets/Timelines/` ready for zero-cut drop-in delivery, paired with high-CTR packaging suites in `Youtube Team Assets/Thumbnails/`.
- **Firebase Leaderboard Roundtrip Hardened:** Validated REST submission with SHA-1 cert fingerprint `174775252333dc2e8159f5ac6de1019661c3e897`, RPC diagnostics, and offline retry queue.

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
- Android APK: `app/build/outputs/apk/debug/app-debug.apk` (and staged under `apk/Dual-Front-TD-v0.0.1.004-debug.apk`)
- Windows Companion: `Dual Front TD.exe` (and staged under `apk/Dual-Front-TD-v0.0.1.002.exe`)
