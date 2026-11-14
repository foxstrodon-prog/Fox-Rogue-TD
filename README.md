# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub Releases.

---

## 📱 Downloads (Android APK & Local ADB Companion)

- **Latest Android APK (`v0.0.1.020`):** [`apk/Dual-Front-TD-v0.0.1.020-debug.apk`](apk/Dual-Front-TD-v0.0.1.020-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe) (located in `apk/` directory)
  - General utility that automatically detects ADB, pairs with your Android device or local emulator, installs the latest staged APK, sets widescreen landscape mode, and launches the game.
- **GitHub Releases:** Only official Android APK releases are distributed on GitHub.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.020

### 🔒 Accessibility Font-Scale Lock, Standardized Dev JSON Metadata & Two-Stage Soft Back Navigation
- **Universal UI Font-Scale Lock (`fontScale = 1.0f`):** Locked `LocalDensity` in `MyApplicationTheme` (`Theme.kt`) and `Configuration.fontScale` in `MainActivity.attachBaseContext()` / `onConfigurationChanged()` to `1.0f`. All tactical HUDs, floating squircles, and popup `Dialog(...)` sub-windows now maintain exact layout proportions regardless of Android system accessibility font-size settings.
- **Standardized Dev Menu & Library JSON Exports:** Updated `DevBalanceConfig.exportDiffJson()` and `SoundManager.exportLibraryJson()` to always include top-level `"appName": "Dual Front TD"`, `"versionName"`, and `"versionCode"` metadata along with a structured `"deltas"` block containing all modified balance, audio, visual, and favorited parameters.
- **Two-Stage Android Soft Back Arrow Navigation:** Added a context-aware `BackHandler` in `RogueTDScreen.kt`:
  - **Stage 1 (Squircles Present):** Pressing Android's soft back arrow while a tower or buildable empty tile is selected cleanly closes the floating squircle bar.
  - **Stage 2 (No Squircles Present):** Pressing Android's soft back arrow when no squircles are open opens the Pause Menu instead of exiting the match.

---

## 🕹️ Previous Releases

### Version 0.0.1.019
- **750-Line Architectural Modularity Law (100% Compliance):** Restructured all 54 production Kotlin source files under `app/src/main/java/com/example/` so every single file strictly adheres to the `<= 750`-line architectural ceiling with high domain cohesion.
- **God-ViewModel Decomposition & Domain Coordinators:** Reduced `RogueTDViewModel.kt` to 596 lines by extracting focused domain coordinators into `engine/coordinator/`:
  - `SimulationCoordinator.kt`: Virtual time scaling, combat simulation ticking, track particle stepping, status effect decay, and sound tick dispatch.
  - `TowerPlacementCoordinator.kt`: Coordinate grid snapping, path clearance verification, scrap transactions, turret construction, and singularity collapse orchestration.
  - `WaveProgressionCoordinator.kt`: Dual-lane spawn timers, wave queue popping, boss multiplier scaling, and wave completion payouts.
- **UI & Canvas Sub-Renderer Decomposition:**
  - Split `GameCanvas.kt` into specialized sub-renderers under `ui/canvas/`: `TerrainRenderer.kt`, `TrackRenderer.kt`, and `EntityRenderer.kt`, eliminating all per-frame `listOf(...)` allocations inside draw loops.
  - Extracted HUD and dialog surfaces into `ui/hud/components/` (`TargetPriorityMenu.kt`, `SingularityWarningDialog.kt`) and `ui/dialog/` (`RunSummaryDialogs.kt`, `TelemetryLeaderboardDialog.kt`, `AudioVisualCustomizationPanel.kt`, `DevMenuComponents.kt`), and cached vector `Path` instances in `TowerUpgradeBar.kt`.
- **Model, Config & Audio Partitioning:** Partitioned core models into `EntityModels.kt`, `MapModels.kt`, `ThemeModels.kt`, and `TowerSynergyConfig.kt`, and split procedural audio synthesis into `ProceduralAudioSynthesizer.kt` and `AudioSynthesizer.kt`.
- **Zero-Allocation Hot-Loop & Spatial Grid Hardening:** Eliminated per-tick collection copies (`toMutableList()`, `.filter`, `.map`, `.associateBy`) across `SimulationCoordinator.kt` and `CombatSimulationEngine.kt`, added reusable `DamageResult` containers in `DamageCalculator.kt`, hardened `SpatialHashGrid.queryNearby` with caller-supplied buffers and in-place predicates, and added strict zero-distance/NaN guards to projectile step math.

---

## 🕹️ Previous Releases

### Version 0.0.1.018
- **Global Leaderboard Restoration & UX Navigation:** Fixed navigation routing from the Pause Menu and Post-Run Summary Dialog to route directly to Tab 2 (Global Ranks). Added automatic score refreshing on tab switch via reactive Compose effects, and synchronized background WorkManager score upload completion directly with `LeaderboardRepository` for instant rank updating.
- **REST Identity Toolkit Auth Hardening:** Hardened `AuthRepository` with graceful fallback to Firebase REST Identity Toolkit anonymous authentication when native Firebase Auth initialization is unavailable, preserving strict unauthenticated device UUID fallback contracts. Added real-time score upload status feedback to post-run dialogs.
- **Simulation Decoupling via `CombatSimulationEngine`:** Extracted pure simulation math, ballistics calculation, enemy path progression, Cryo shatterburst effects, and visual particle simulation from `RogueTDViewModel.kt` into `CombatSimulationEngine.kt`, reducing ViewModel complexity and enforcing clean separation of concerns.
- **Spatial Hash AoE Acceleration:** Upgraded explosive munitions sweeps (Mortar artillery, Hellfire blasts, Cluster munitions, and Void singularity splash) from full linear scans to $O(K)$ localized spatial neighborhood queries via `SpatialHashGrid.getPotentialCollisions(x, y, radius)`.
- **Domain Model Partitioning:** Partitioned monolithic `GameModels.kt` by separating tower archetypes, classes, and balances into `TowerBalanceConfig.kt` and enemy roles, shapes, and archetypes into `EnemyBalanceConfig.kt`, maintaining high cohesion and strict file size boundaries.
- **Zero-Allocation Hot-Loop Scratch Buffering:** Implemented pre-allocated double-buffered scratch lists for surviving entities and AoE collision queries in `CombatSimulationEngine`, eliminating per-tick garbage collection pauses during high-density 100+ swarm waves.

---

## 🕹️ Previous Releases

### Version 0.0.1.017
- **Combat Cadence & Level 1–3 Progression Salience:** Rebalanced baseline starter weapon fire rates by ~40–50% (Gatling 1.2/s, Cryo 1.6/s, Tesla 0.7/s, Mortar 0.40/s, Laser 1.8/s, Void 0.6/s, Stellar 0.9/s) and scaled per-shot base damage up (+80% to +130%) so early-game towers deliver punchy, measured strikes rather than hose-like sprays. Scaled upgrade progression through Tier 1 (+20% SPD, +15% DMG), Tier 2 (+40% SPD, +35% DMG), and Tier 3 (+70% SPD, +60% DMG) so evolutions feel truly transformative.
- **Physical Ballistics & Projectile Travel Time:** Tuned ballistic velocities (Gatling 8.5, Mortar 4.2, Stellar 6.0) so munitions travel across the grid with realistic flight physics and artillery arcs, allowing players to track ordnance flight time.
- **Player-Chosen Synergy Conduit & Permanent Teaser Squircle:**
  - On Tier 3 maxed towers, the 4th upgrade slot now houses an interactive Conduit dock.
  - When the grid is incomplete, it acts as a progress teaser showing ready counts (`X/Y Ready`), with tapping triggering floating HUD guidance.
  - When the grid is 100% full with Tier 2/3 towers, it illuminates in electric gold (`[★ FORGE ★ CONDUIT]`), allowing players to choose *which specific tower* collapses the board into the Synergy Conduit monument.
  - **Strictly ONE Synergy Conduit Invariant:** Sacrificing an older Conduit into a newly chosen tower absorbs all past banked vaults and generation ranks, keeping exactly one monument on the battlefield.
- **Tier 3 Squircle Dynamic Cost Synchronization:** Fixed cost desynchronization where the upgrade UI displayed a flat 280 scrap while the engine charged dynamic multi-upgrade costs.
- **Expanded Map Grid & Tactical Lanes:** Expanded default map height from 18 to 20 (+2 rows, 200 tiles, 14–18 buildable pads) and widened procedural lane paths (`minLaneLength = 16, maxLaneLength = 32`) with corner legs touching outer edges before converging at the nexus spine.

---

## 🕹️ Previous Releases

### Version 0.0.1.016
- **Zero-Allocation Compose Rendering Engine:** Refactored `GameCanvas.kt` rendering pipeline with singleton path reuse (`scratchPath1`, `scratchPath2`, `lanePathA`, `lanePathB`, `mergedPath`), eliminating all per-frame `Path()` heap allocations across tower skins, badges, enemy vectors, and tactical pointers.
- **Hot-Loop Heap Elimination:** Replaced all high-frequency `UUID.randomUUID().toString()` string instantiations in weapon fire, enemy spawning, and projectiles with an atomic, zero-allocation primitive sequence generator (`"p" + nextEntityId++`).
- **Dirty-Flag Synergy Caching:** Implemented dirty-flag caching (`isSynergyCacheDirty`) for `computeActiveSynergies()`. Active synergy set calculations are cached across ticks and recomputed only on structural state changes (tower build, upgrade, sell, or singularity collapse).
- **$O(1)$ Projectile-Enemy Spatial Indexing & Squared Distance Comparisons:** Pre-indexed surviving enemies by ID (`enemyMap`) in projectile update loops to replace $O(P \cdot E)$ double scans with $O(1)$ hash lookups, and replaced per-tick Euclidean `Math.hypot` calls with squared distance comparisons (`dx * dx + dy * dy <= r * r`).
- **Asymptotic Infinite Wave Ceiling:** Hardened `WaveGenerator.kt` late-game exponential scaling with an asymptotic double-clamp (`minOf(post50, 300.0)` and `minOf(1.0e12f, w50Base * expCurve)`), preventing `Float.POSITIVE_INFINITY` or `NaN` overflow while maintaining exact escalation curves.
- **Global Movement Floor Safeguard:** Clamped hostile slow multipliers (`maxOf(0.15f, 1f - state.globalFrostSlowBonus)`) to prevent reverse-walking enemies under stacked global frost buffs.
- **Modular Simulation Decoupling:** Decoupled `CinematicController.kt` from `RogueTDViewModel.kt`, removing over 500 lines of showcase logic and enforcing single-responsibility architecture across the simulation layer.
- **Architecture & Modularity Standards:** Enforced zero-allocation 60/120 FPS loop invariants, asymptotic math soft-caps, and modular file-size boundaries across game simulation and rendering layers.

---

## 🕹️ Previous Releases

### Version 0.0.1.015
- **The Synergy Singularity (Board-Merge Capstone Loop):** Fill and max 100% of the buildable map grid (Tier ≥ 2 Archetype or Apex) to unlock the high-stakes `[★ SINGULARITY CAPSTONE]` squircle on your oldest anchor tower. Activating it permanently banks all active global synergies and auras into a multi-generational `BankedSynergyVault`, clearing the remaining tiles for drafting fresh generations of towers.
- **Mid-Wave Tactical High-Alert:** Singularity activation is supported unrestricted—even during intense mid-wave engagements—via a tactical confirmation modal displaying sacrificed tower counts and exact scrap refund.
- **20% Singularity Scrap Bounty:** Sacrificing your board refunds 20% of all invested scrap across all sacrificed towers directly into the treasury (minimum 250c), enabling instant drafting of 2–3 starter towers for Generation N+1.
- **Persistent Multi-Generational Monument:** Banked synergies stack cumulatively generation after generation inside the single Capstone monument, creating massive compounding aura curves.
- **4-Tier Difficulty Modes:** Choose your challenge level in the Pause Menu—`EASY` (Wave 30 milestone), `MEDIUM` (Wave 50 milestone), `HARD` (Wave 100 milestone), and `INFINITE` (endless escalation).
- **True Exponential Escalation Math:** Hard mode scales exponentially past Wave 30 ($1.35 \times 1.045^{\text{wave}-30}$) and Infinite mode scales with an aggressive $1.08^{\text{wave}-50}$ curve past Wave 50.
- **Asymptotic Engine Safeguards:** Clamped enemy slow factors to a minimum of 15% speed (preventing 0% freeze locks) and enforced a 50ms turret firing cooldown floor (20 shots/sec cap) to safeguard CPU performance under multi-generational synergy overclocking.

---

## 🕹️ Previous Releases

### Version 0.0.1.014
- **Standardized Rotated Bullet HUD:** All rotated indicators (Wave, HP, Scrap, Synergies, Teleportation Pads, Auto-Wave, and Begin) standardized to a sleek, uniform `28.dp x 38.dp` layout, providing generous vertical headroom that eliminates text clipping in landscape orientation.
- **Streamlined Launch & Rotation Parity:** Streamlined Begin button with a single play arrow and vertical icon/label stacking under rotation; decoupled the Settings gear from rotation; added automatic system rotation lock (`ACCELEROMETER_ROTATION`) compliance with an on-tilt suggestion trigger.
- **Full-Width Edge-Anchored Landscape Dropdowns:** Overhauled Synergies and Teleportation Pads menus in landscape to use full-width unconstrained dialogs (`usePlatformDefaultWidth = false`) anchored directly adjacent to the Top HUD on the active edge, providing 320–360dp width so all mechanics and 6/6 Harmonic Grid details display clearly without clipping.
- **100% Unique 52-Tower Palette & Squircles:** Every single tower across all 4 classes, 12 Tier 1 vectors, and 36 Tier 2 archetypes now has a completely unique, non-overlapping hex color. Upgraded squircles feature centered title/subtitle typography matching the tower's unique hex.
- **In-Place Target Priority & Sell Ergonomics:** Restored muscle-memory ergonomics by pinning Target Priority and Sell together on the right flank adjacent to upgrade squircles, counter-rotating in place without jumping across the screen.
- **Deselect-on-Reselect Toggle:** Tapping an already selected tower or tile immediately dismisses the bottom upgrade dock without requiring players to tap a neutral track tile.

---

## 🕹️ Previous Releases

### Version 0.0.1.013
- **Dev-Tuning Disqualification Resolution:** Completely disentangled audio volumes, mute toggles, music track selections, turret skins, and visual themes from dev-tuning detection via `isGameplayModified()`. Legitimate player runs are never disqualified from global rankings by player preferences or cosmetic customizations.
- **Cold-Start Network Resiliency:** Opening the telemetry modal or in-game pause leaderboard directly bypasses query throttles (`forceRefresh = true`) and auto-retries initial network socket handshakes during cold boot.
- **Verified Uplink Empty-State UX:** Cleanly differentiated between connected empty scoreboards (`GLOBAL UPLINK VERIFIED • CONNECTED`) and actual network drops, providing an instant `"Refresh Rankings"` affordance.
- **Google Identity Toolkit REST Fallback:** Hardened `AuthRepository` with an automated REST fallback for Anonymous Authentication, ensuring valid JWT tokens on all Android devices and emulators.
- **Canonical Collection Normalization & Fail-Safe Fallback:** All client requests are automatically sanitized and normalized to canonical `/leaderboards`, with seamless cross-path failover to `/leaderboard` to ensure 100% submission and query reliability.
- **Dual-Path Security Rules Alignment:** Cloud security rules strictly authorize and validate both `/leaderboards/{runId}` and `/leaderboard/{scoreId}` with identical field schemas, eliminating HTTP 403 `PERMISSION_DENIED` errors.
- **Structured Logcat Telemetry:** Integrated verbose, structured diagnostic logging across `[FirestoreLeaderboardApi]`, `[LeaderboardRepository]`, and `[ScoreSubmissionWorker]` for full real-time device observability.
- **Graceful UI State Protection:** Round-end score submission cleanly displays `QUEUED FOR UPLOAD (OFFLINE)` whenever scores are safely staged in the local queue, preventing false alarm failure banners.

---

## 🕹️ Previous Releases

### Version 0.0.1.011
- **Production Index Alignment:** Fully aligned Firestore composite indexing sorting primarily by highest wave reached (`maxWave` DESC), with tie-breaking by high score (`highScore` DESC) and submission timestamp.
- **Dynamic Secure Key Resolution:** Build configuration dynamically resolves Firebase credentials via environment variables, local configuration, or `google-services.json`, keeping repositories completely clean of hardcoded keys.
- **Resilient Fallback Query Engine:** Self-healing query pipeline with client-side dual-sort ensures scoreboard availability across all network conditions and provisioning states.
- **Strict Identity & Offline Zero-Loss Queue:** Submissions are cryptographically bound to authenticated Firebase Anonymous player tokens with WorkManager offline retry queueing.

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
- **Testing:** Robolectric, JUnit 4, AndroidX Test Runner (95/95 passing)
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
