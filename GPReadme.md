# Dual Front TD – Google Play Store Notes & "What's New"

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

---

### 🚀 What's New in Version 0.0.1.018

🏆 **Global Leaderboard Restoration, Simulation Decoupling & Engine Hardening**
- **Global Leaderboard Restoration & UX Navigation:** Fixed navigation routing from the Pause Menu and Post-Run Summary Dialog to route directly to Tab 2 (Global Ranks). Added automatic score refreshing on tab switch via reactive Compose effects, and synchronized background WorkManager score upload completion directly with `LeaderboardRepository` for instant rank updating.
- **REST Identity Toolkit Auth Hardening:** Hardened `AuthRepository` with graceful fallback to Firebase REST Identity Toolkit anonymous authentication when native Firebase Auth initialization is unavailable, preserving strict unauthenticated device UUID fallback contracts. Added real-time score upload status feedback to post-run dialogs.
- **Simulation Decoupling via `CombatSimulationEngine`:** Extracted pure simulation math, ballistics calculation, enemy path progression, Cryo shatterburst effects, and visual particle simulation from `RogueTDViewModel.kt` into `CombatSimulationEngine.kt`, reducing ViewModel complexity and enforcing clean separation of concerns.
- **Spatial Hash AoE Acceleration:** Upgraded explosive munitions sweeps (Mortar artillery, Hellfire blasts, Cluster munitions, and Void singularity splash) from full linear scans to $O(K)$ localized spatial neighborhood queries via `SpatialHashGrid.getPotentialCollisions(x, y, radius)`.
- **Domain Model Partitioning:** Partitioned monolithic `GameModels.kt` by separating tower archetypes, classes, and balances into `TowerBalanceConfig.kt` and enemy roles, shapes, and archetypes into `EnemyBalanceConfig.kt`, maintaining high cohesion and strict file size boundaries.
- **Zero-Allocation Hot-Loop Scratch Buffering:** Implemented pre-allocated double-buffered scratch lists for surviving entities and AoE collision queries in `CombatSimulationEngine`, eliminating per-tick garbage collection pauses during high-density 100+ swarm waves.

---

### 📦 Recent Patch History

#### Version 0.0.1.017

🎯 **Combat Rebalance, Synergy Conduit Agency & Map Expansion**
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

### 📦 Recent Patch History

#### Version 0.0.1.016

⚡ **Performance Optimization & Zero-Allocation Engine Rollout**
- **Zero-Allocation Compose Rendering Engine:** Refactored `GameCanvas.kt` rendering pipeline with singleton path reuse (`scratchPath1`, `scratchPath2`, `lanePathA`, `lanePathB`, `mergedPath`), eliminating all per-frame `Path()` heap allocations across tower skins, badges, enemy vectors, and tactical pointers.
- **Hot-Loop Heap Elimination:** Replaced all high-frequency `UUID.randomUUID().toString()` string instantiations in weapon fire, enemy spawning, and projectiles with an atomic, zero-allocation primitive sequence generator (`"p" + nextEntityId++`).
- **Dirty-Flag Synergy Caching:** Implemented dirty-flag caching (`isSynergyCacheDirty`) for `computeActiveSynergies()`. Active synergy set calculations are cached across ticks and recomputed only on structural state changes (tower build, upgrade, sell, or singularity collapse).
- **$O(1)$ Projectile-Enemy Spatial Indexing & Squared Distance Comparisons:** Pre-indexed surviving enemies by ID (`enemyMap`) in projectile update loops to replace $O(P \cdot E)$ double scans with $O(1)$ hash lookups, and replaced per-tick Euclidean `Math.hypot` calls with squared distance comparisons (`dx * dx + dy * dy <= r * r`).
- **Asymptotic Infinite Wave Ceiling:** Hardened `WaveGenerator.kt` late-game exponential scaling with an asymptotic double-clamp (`minOf(post50, 300.0)` and `minOf(1.0e12f, w50Base * expCurve)`), preventing `Float.POSITIVE_INFINITY` or `NaN` overflow while maintaining exact escalation curves.
- **Global Movement Floor Safeguard:** Clamped hostile slow multipliers (`maxOf(0.15f, 1f - state.globalFrostSlowBonus)`) to prevent reverse-walking enemies under stacked global frost buffs.
- **Modular Simulation Decoupling:** Decoupled `CinematicController.kt` from `RogueTDViewModel.kt`, removing over 500 lines of showcase logic and enforcing single-responsibility architecture across the simulation layer.
- **Architecture & Modularity Standards:** Enforced zero-allocation 60/120 FPS loop invariants, asymptotic math soft-caps, and modular file-size boundaries across game simulation and rendering layers.

---

### 📦 Recent Patch History

#### Version 0.0.1.015

🌌 **The Synergy Singularity & 4-Tier Non-Visual Escalation Engine**
- **The Synergy Singularity (Board-Merge Capstone Loop):** Fill and max 100% of the buildable map grid (Tier ≥ 2 Archetype or Apex) to unlock the high-stakes `[★ SINGULARITY CAPSTONE]` squircle on your oldest anchor tower. Activating it permanently banks all active global synergies and auras into a multi-generational `BankedSynergyVault`, clearing the remaining tiles for drafting fresh generations of towers.
- **Mid-Wave Tactical High-Alert:** Singularity activation is supported unrestricted—even during intense mid-wave engagements—via a tactical confirmation modal displaying sacrificed tower counts and exact scrap refund.
- **20% Singularity Scrap Bounty:** Sacrificing your board refunds 20% of all invested scrap across all sacrificed towers directly into the treasury (minimum 250c), enabling instant drafting of 2–3 starter towers for Generation N+1.
- **Persistent Multi-Generational Monument:** Banked synergies stack cumulatively generation after generation inside the single Capstone monument, creating massive compounding aura curves.
- **4-Tier Difficulty Modes:** Choose your challenge level in the Pause Menu—`EASY` (Wave 30 milestone), `MEDIUM` (Wave 50 milestone), `HARD` (Wave 100 milestone), and `INFINITE` (endless escalation).
- **True Exponential Escalation Math:** Hard mode scales exponentially past Wave 30 ($1.35 \times 1.045^{\text{wave}-30}$) and Infinite mode scales with an aggressive $1.08^{\text{wave}-50}$ curve past Wave 50.
- **Asymptotic Engine Safeguards:** Clamped enemy slow factors to a minimum of 15% speed (preventing 0% freeze locks) and enforced a 50ms turret firing cooldown floor (20 shots/sec cap) to safeguard CPU performance under multi-generational synergy overclocking.

---

### 📦 Recent Patch History

#### Version 0.0.1.014

🎯 **Tactical HUD Evolution, 52-Tower Unique Matrix & Ergonomics**
- **Standardized Rotated Bullet HUD:** All rotated indicators (Wave, HP, Scrap, Synergies, Teleportation Pads, Auto-Wave, and Begin) standardized to a sleek, uniform `28.dp x 38.dp` layout, providing generous vertical headroom that eliminates text clipping in landscape orientation.
- **Streamlined Launch & Rotation Parity:** Streamlined Begin button with a single play arrow and vertical icon/label stacking under rotation; decoupled the Settings gear from rotation; added automatic system rotation lock (`ACCELEROMETER_ROTATION`) compliance with an on-tilt suggestion trigger.
- **Full-Width Edge-Anchored Landscape Dropdowns:** Overhauled Synergies and Teleportation Pads menus in landscape to use full-width unconstrained dialogs (`usePlatformDefaultWidth = false`) anchored directly adjacent to the Top HUD on the active edge, providing 320–360dp width so all mechanics and 6/6 Harmonic Grid details display clearly without clipping.
- **100% Unique 52-Tower Palette & Squircles:** Every single tower across all 4 classes, 12 Tier 1 vectors, and 36 Tier 2 archetypes now has a completely unique, non-overlapping hex color. Upgraded squircles feature centered title/subtitle typography matching the tower's unique hex.
- **In-Place Target Priority & Sell Ergonomics:** Restored muscle-memory ergonomics by pinning Target Priority and Sell together on the right flank adjacent to upgrade squircles, counter-rotating in place without jumping across the screen.
- **Deselect-on-Reselect Toggle:** Tapping an already selected tower or tile immediately dismisses the bottom upgrade dock without requiring players to tap a neutral track tile.

---

### 📦 Recent Patch History

#### Version 0.0.1.013

🏆 **Operational Global Leaderboard & Dev-Tuning Separation**
- **Dev-Tuning Disqualification Resolution:** Decoupled volume levels, mute state, music track preferences, turret skins, and visual themes from dev-tuning detection via `isGameplayModified()`. Honest gameplay is never disqualified from global rankings due to audio or cosmetic customizations.
- **Cold-Start Network Resiliency:** Opening the telemetry modal or in-game pause leaderboard directly bypasses query throttles (`forceRefresh = true`) and auto-retries initial network socket handshakes during cold boot.
- **Verified Uplink Empty-State UX:** Cleanly differentiated between connected empty scoreboards (`GLOBAL UPLINK VERIFIED • CONNECTED`) and actual network drops, providing an instant `"Refresh Rankings"` affordance.
- **Google Identity Toolkit REST Fallback:** Hardened `AuthRepository` with an automated REST fallback for Anonymous Authentication, ensuring valid JWT tokens on all Android devices and emulators.

---

### 📦 Recent Patch History

#### Version 0.0.1.012

🏆 **Canonical Leaderboards, Dual-Path Security & Observability**
- **Canonical Routing & Cross-Path Failover:** Automatic path normalization guarantees that all player runs submit and query through canonical `/leaderboards` with automated cross-path failover to `/leaderboard`.
- **Dual-Path Security Alignment:** Firestore security rules seamlessly authorize both `/leaderboards/{runId}` and `/leaderboard/{scoreId}` with unified verification logic, preventing permission rejections.
- **Structured Device Telemetry:** Added end-to-end Logcat instrumentation for network requests, repository states, and WorkManager background syncing.
- **Resilient UI State Feedback:** Round-end status displays informative "QUEUED FOR UPLOAD (OFFLINE)" indicators whenever scores are held locally in safe storage.

---

### 📦 Recent Patch History

#### Version 0.0.1.011

🏆 **Production Composite Leaderboard Architecture & Secure Credentialing**
- **Production Index Alignment:** Live cloud composite indexing enabled for sorting by maximum wave reached (`maxWave` DESC), high score tie-breaking (`highScore` DESC), and timestamp ordering.
- **Dynamic Secure Key Resolution:** Enhanced build security with dynamic client credential resolution, completely decoupling API keys from git-tracked source files.
- **High Availability Query Engine:** Resilient dual-tier querying provides instant, flicker-free score streaming across both local offline caches and cloud rankings.
- **Zero-Loss Identity & Offline Queue:** Cryptographically signed submissions paired with background WorkManager queues ensure no completed runs are ever lost.

---

### 📦 Recent Patch History

#### Version 0.0.1.010

🛡️ **Leaderboard Engine Resilience & Self-Healing Query Engine**
- **Zero-Failure Index Fallback:** Integrated resilient fallback querying that serves global leaderboards instantaneously even while cloud composite indexes are provisioning, preventing connection dropouts and server errors.
- **Synchronized Auth-to-Score Binding:** Fixed identity payload mapping in background score submission workers to ensure anonymous player cryptographic tokens cleanly pass security rules.
- **Offline Zero-Loss Queue:** Unsubmitted run logs are safely held in local storage and automatically flushed as soon as network and cloud services are active.

---

### 📦 Recent Patch History

#### Version 0.0.1.009

👤 **Native Anonymous Authentication & Secure Identity**
- **Zero-Friction Identity:** Added native Firebase Anonymous Authentication for secure, persistent player identification without passwords, emails, or personal data collection.
- **Score Authorship:** Secure cloud rules verify that submitted high scores match the authenticated player identity, locking submissions against unauthorized tampering.

🏆 **Dual-Metric Composite Global Leaderboards**
- **Composite Leaderboard Indexing:** Sorted primarily by `maxWave` descending, with `highScore` as an instant tie-breaker. Fully backed by cloud composite index queries.
- **Local Commander Highlight:** Live scoreboard dynamically identifies and highlights the active player's rank.

🌐 **Real-Time Remote Balancing (Remote Config)**
- **Cloud-Tuned Difficulty Curves:** Bound enemy health, scrap bounties, boss health multipliers, and harmonic grid damage boosts to remote configuration parameters for live balance adjustments.

📊 **Churn Telemetry & Difficulty Spike Pinpointing**
- **Defeat State Telemetry:** Added `level_fail` event analytics capturing wave number, tower count, remaining scrap currency, and MVP tower class to pinpoint overtuned waves.
- **FTUE Funnel Checkpoints:** Tracks initial hint engagement and wave progression milestones.

---

### 📦 Recent Patch History

#### Version 0.0.1.008

🔐 **Persistent Signing Certificate Parity & Conflict Resolution**
- **Canonical Keystore Auto-Restoration:** Hardened build configuration with an embedded canonical debug keystore decoder. Ensures all local developer builds, CI runners, and GitHub release binaries share the exact same signing key, permanently fixing "App not installed as package conflicts with an existing package" errors during in-app updates.
- **Self-Healing ADB Companion:** Enhanced `Dual-Front-TD-ADB.exe` with automatic signature conflict detection; cleanly uninstalls mismatched legacy certificates and installs new packages in a single seamless step.

🎮 **Tactical Ergonomics & Dynamic UI Rotation (v0.0.1.007 Highlights)**
- **Booster Pad Dropdown Capsule:** Replaced the wide text sub-strip with an interactive dropdown capsule (`⚡ X/6`) alongside aura synergies explaining scrap boost bonuses, the 6/6 Harmonic Grid buff (+20% Tower Damage / +25% Scrap), and dual-thumb repositioning controls.
- **Dynamic Orientation Stacking:** In portrait, Core HP and Scrap render inline horizontally for minimal vertical HUD height. In landscape rotation, Core HP, Scrap, and Auto-Wave dynamically stack vertically into thin pills.
- **Side-by-Side Target Priority & Sell Buttons:** In portrait, Target Priority and Sell buttons stack vertically in a 44dp column to match squircle height (reducing dock width to 296dp). In landscape rotation, they deploy side-by-side.
- **Tier 0 Compound Upgrade Naming:** Vector upgrade buttons now use unified compound naming and archetype theme colors (`Kinetic Offense`, `Void Defense`, `Siege Support`, `Stellar Offense`).
- **Clean Settings Gear:** Borderless floating gear cog icon integrated cleanly into the top HUD.
- **Tactical Map Expansion (+2 Rows: 10x18):** Expanded battlefield map height by 2 rows with synchronized spatial hash indexing for deeper choke-point positioning.

---

### 📦 Recent Patch History

#### Version 0.0.1.004
- **Build System Hardening:** Upgraded the automated build and release pipeline to native Node 24 actions and Ubuntu 24.04 LTS runners.
- **Showcase Polish:** In-game cinematics now feature automated gameplay demonstrations with full touch lock during playback.

#### Version 0.0.1.003
- **True Screen Orientation & Smooth Counter-Rotation:** Canvas coordinates remain fixed while top bullet HUD, floating squircle buttons, and combat damage numbers counter-rotate cleanly across portrait and landscape.
- **Floating Squircle Upgrade Buttons:** 56dp squircle buttons floating 32dp above the device gesture bar with vibrant neon accents and minimal cost badges.
- **6 In-Game Feature Cinematics:** Deterministic 30.00-second showcase scenarios at locked 60 FPS in Dev Menu highlighting peak combat and tactical mechanics.
- **Leaderboard Security Roundtrip:** Validated REST submissions with production certificate fingerprinting and atomic offline retries.

#### Version 0.0.1.002
- **Stellar Star Class & 52-Tower Matrix:** Added Class 4 with 9 cosmic evolutions piercing barriers with innate +15% damage bonus.
- **Booster Pad Teleportation:** 6 color-paired booster pads with instant tactical swaps, 750ms cooldown, and +3% scrap bounty per occupied pad.
- **Audio Polish & Low-Pass Filtering:** Smooth 600–1000ms gradual fades and mellow ambient synthesizer filtering (<650Hz).
- **Rebalanced Economy:** Tuned early-game scrap bounties across waves 1–100.

#### Version 0.0.1.001
- **Spatial Hash Collision Grid:** Locked 60/120 FPS performance during heavy swarm waves.
- **Modular UI Refactor:** Separated HUD, upgrade bar, and menus into single-responsibility components.
- **39 Tower Hierarchy:** Completed the initial 3 -> 9 -> 27 branching upgrade progression across Kinetic, Elemental, and Siege families.
- **Procedural Level 1–4 Visual Cues:** Dynamic chassis, wings, focus barrels, and Apex radiant coronas.

#### Version 0.0.0.013
- **Natural Pause Attenuation:** Music drops 50% in volume when paused and resumes seamlessly when returning to gameplay.
- **In-Menu Sound Auditions:** Preview weapon sounds and background synth tracks directly in the audio settings tab.
- **Extended Music Tracks:** 45-second procedural synthwave tracks with smooth tapered loops.
- **Leaderboard Retry Hardening:** Added automatic retries to ensure offline and delayed scores upload safely.

#### Version 0.0.0.012
- **Rotation Support:** Device rotation smoothly adjusts HUD text chips and menus while locking the underlying map coordinates to eliminate hitbox drift.
- **Immediate Audio Cut:** Sound cuts instantly when minimizing the app or locking your screen—no lingering background audio.

#### Version 0.0.0.010
- **Official Dual Front TD Launch:** Updated app branding and added the local ADB companion runner (`apk/Dual-Front-TD-ADB.exe`).
- **Live Combat DPS:** Real-time damage-per-second readouts on inspected towers with an in-depth telemetry reset window.

---

### 📜 Source Code & Historical APK Archive
Looking for full technical documentation or previous APK versions?
Every release is preserved and open on GitHub:
👉 **[GitHub Releases & APK Archive](https://github.com/foxstrodon-prog/Fox-Rogue-TD/releases)**  
👉 **[Source Code & Developer Docs](https://github.com/foxstrodon-prog/Fox-Rogue-TD)**
