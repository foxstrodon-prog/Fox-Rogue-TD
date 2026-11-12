# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub Releases.

---

## 📱 Downloads (Android APK & Local ADB Companion)

- **Latest Android APK (`v0.0.1.010`):** [`apk/Dual-Front-TD-v0.0.1.010-debug.apk`](apk/Dual-Front-TD-v0.0.1.010-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe) (located in `apk/` directory)
  - General utility that automatically detects ADB, pairs with your Android device or local emulator, installs the latest staged APK, sets widescreen landscape mode, and launches the game.
- **GitHub Releases:** Only official Android APK releases are distributed on GitHub.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.010

### 🛡️ Leaderboard Engine Resilience & Self-Healing Fallback
- **Zero-Failure Index Fallback:** If cloud composite index building is in progress, the leaderboard client seamlessly falls back to single-metric streaming with client-side dual-sort, guaranteeing 100% scoreboard uptime without server errors.
- **Auth-Score Synchronizer:** Strictly binds score submissions to authenticated Firebase Anonymous identity tokens (`request.auth.uid`), eliminating 403 authorization rejects.
- **Offline Zero-Loss Queue:** Hardened WorkManager background worker to preserve all pending run submissions during network transitions and server setup.

### 👤 Native Anonymous Authentication & Zero-Friction Identity (v0.0.1.009)
- **Silent Player Identity:** Integrated native Firebase Anonymous Authentication for seamless player identity. Every commander receives a persistent, secure cryptographic identity without sign-up forms, passwords, or personal data collection.
- **Score Authorship Protection:** Security rules strictly verify that leaderboard submissions match the authenticated user identity while keeping scores tamper-proof and immutable.

### 🏆 Dual-Metric Composite Global Leaderboards
- **Composite Ranking Query:** Implemented dual-criteria composite ranking sorted primarily by `maxWave` descending, using `highScore` as an instant tie-breaker, backed by dedicated cloud composite indexing for minimal read overhead.
- **Local Commander Highlight:** Dynamically flags and highlights the active commander's rank entry on the global scoreboard.

### 🌐 Real-Time Remote Balancing (Remote Config)
- **Dynamic Wave & Economy Multipliers:** Bound game engine balance curves to cloud configuration parameters, enabling on-the-fly adjustments to enemy health scaling, scrap bounties, boss health multipliers, and harmonic grid damage boosts.

### 📊 Churn Telemetry & Difficulty Spike Pinpointing
- **Failure State Analytics:** Instrumented granular failure event logging (`level_fail`) capturing the exact failure wave, tower count, remaining scrap currency, and MVP tower class to identify overtuned difficulty cliffs.
- **FTUE Funnel Metrics:** Added telemetry tracking across initial hint engagement and wave progression milestones.

---

## 🕹️ Previous Updates (Version 0.0.1.008)

### 🔐 Persistent Signing Certificate Parity & Conflict Resolution
- **Canonical Keystore Auto-Restoration:** Hardened `app/build.gradle.kts` and GitHub Actions release workflow with an embedded canonical debug keystore decoder. Ensures all local developer builds, CI runners, and GitHub release binaries are signed with the exact same cryptographic key, permanently resolving "App not installed as package conflicts with an existing package" signature mismatches on in-app updates.
- **Self-Healing ADB Companion:** Enhanced `Dual-Front-TD-ADB.exe` with automatic signature conflict detection; cleanly uninstalls mismatched legacy certificates and installs new packages in a single seamless step.

### 🎮 Tactical Ergonomics & UI Rotation Fluidity (v0.0.1.007 / v0.0.1.006 Highlights)
- **Booster Pad Dropdown Capsule:** Converted the wide booster pad sub-strip into an interactive dropdown capsule (`⚡ X/6`) alongside aura synergies detailing scrap boost bonuses, the 6/6 Harmonic Grid buff (+20% Tower Damage / +25% Scrap across all lanes), and dual-thumb repositioning controls.
- **Dynamic Orientation Stacking:** In portrait mode, Core HP and Scrap render inline horizontally to minimize top HUD thickness. In landscape rotation, Core HP, Scrap, and Auto-Wave dynamically stack vertically into thin pills to preserve horizontal track clearance.
- **Side-by-Side Target Priority & Sell Squircles:** In portrait, Target Priority and Sell buttons stack vertically in a 44dp column to match squircle height (reducing dock width to 296dp). In landscape rotation, they deploy side-by-side.
- **Tier 0 Compound Upgrade Naming:** Vector upgrade buttons now use unified compound naming and archetype theme colors (`Kinetic Offense`, `Void Defense`, `Siege Support`, `Stellar Offense`).
- **Clean Settings Gear:** Removed the opaque container box behind the settings gear cog for a clean borderless floating icon.
- **Tactical Map Expansion (+2 Rows: 10x18):** Expanded battlefield map height by 2 rows with synchronized spatial hash indexing for deeper choke-point positioning.

### 🚀 CI/CD Pipeline & GitHub Actions Hardening
- **Node.js 24 & Action Modernization:** Migrated `actions/setup-java` to `@v5` and `gradle/actions/setup-gradle` to `@v4` (targeting native Node.js 24 runners and resolving upstream Node 20 deprecation notices).
- **Graceful Cache Resilience:** Upgraded Gradle cache integration to prevent build interruptions during transient GitHub Actions cache service outages or HTTP 400/503 responses.
- **Idempotent Release Pipeline & Concurrency Guards:** Added workflow concurrency groups (`group: release-${{ github.ref_name }}`) and resilient release upload logic, eliminating parallel release creation collisions when pushing branches and tags simultaneously.
- **Runner Pin Stability:** Pinned build runner environment to `ubuntu-24.04` LTS to ensure deterministic builds and avoid automated OS image shift warnings.

### 🎬 In-Game Feature Showcase Polish & Touch Lock
- **Automated Feature Demonstrations:** In-game cinematics now execute real-time gameplay demonstrations (targeting reticles, booster pad teleportation maneuvers, and squircle upgrade bar inspections).
- **Playback Input Protection:** Complete screen touch locking during cinematic playback ensures zero accidental interruptions.
- **Direct Clipboard Integration:** Launching any showcase copies its standard clip reference (`v001004_CinematicX.mp4`) to the device clipboard.

---

## 📜 Prior Patch Highlights

### Version 0.0.1.003
- **True Screen Orientation & HUD Counter-Rotation:** Coordinate decoupling keeps tactical game canvas fixed, while top Bullet HUD (`HudTopBar.kt`), floating squircle action bar (`TowerUpgradeBar.kt`), and combat damage numbers smoothly counter-rotate (`Modifier.rotate(-angle)` / `graphicsLayer`) so all text and metrics stay readable in 0°, 90°, 180°, and 270°.
- **Floating Squircle Upgrade Action Bar:** Replaced heavy bottom drawer sheet with sleek 56dp x 56dp squircle buttons floating 32dp above gesture bar. Features glowing neon borders, minimal cost badges (e.g. `80c`, `140c`, `180c`, `210c`), spring scale tap feedback, and zero multi-paragraph clutter.
- **6 In-Game Feature Cinematics:** Built-in deterministic simulation engine running at locked 60 FPS (1,800 frames per scenario) accessible via the DEV menu showcasing peak combat and tactical mechanics.
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
- Android APK: `app/build/outputs/apk/debug/app-debug.apk` (and staged under `apk/Dual-Front-TD-v0.0.1.008-debug.apk`)
- Local ADB Runner: `apk/Dual-Front-TD-ADB.exe` (general ADB companion shell)
