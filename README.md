# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub.

---

## 📱 Downloads (Android APK & Windows PC)

- **Latest Android APK (`v0.0.1.002`):** [`apk/Dual-Front-TD-v0.0.1.002-debug.apk`](apk/Dual-Front-TD-v0.0.1.002-debug.apk)
- **One-Click Windows PC Launcher:** [`Dual Front TD.exe`](Dual%20Front%20TD.exe) (single executable in root)
  - Automatically detects ADB, pairs with your Android device or local emulator, installs the latest build, and launches the game.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.002

### 🌟 52-Tower Evolution Matrix & Stellar Star Class
- **Class 4: Stellar Cosmic (`STELLAR_COSMIC`):** Unlocked after deploying any three Tier-3 towers. Introduces cosmic beam, flare, and singularity mechanics with a native +15% damage bonus against shielded targets.
- **Full 4 -> 12 -> 36 Unit Hierarchy:** Expands the evolution tree from 39 to 52 units, introducing 9 new Final Form archetypes (Supernova Pulsar, Graviton Lance, Celestial Comet, Event Horizon, Nebula Aegis, Singularity Bastion, Astral Beacon, Dyson Relay, Quasar Battery).
- **Star Creep Wave Variants:** Introducing Stellar Probes, Cosmic Weavers, and Astral Colossi with unique 5-pointed geometric rendering and distinct speed curves.

### ⚡ Booster Pad Teleportation & Ergonomics Overhaul
- **Instant Tactical Swap:** 6 booster pads arranged in 3 color-coded pairs (Green AMPLIFIER, Teal HIGH_GROUND, Orange CONDUIT). Tap the one-handed edge buttons to swap or move turrets between matching pads with a 750ms tactical cooldown.
- **Harmonic Pad Economy:** Earn +3% bonus scrap bounty per occupied booster pad, rewarding aggressive board utilization.
- **Floating Bullet-Style HUD:** Redesigned top HUD into a sleek floating capsule with a +25% transparency margin on rotation. Accommodates 6-digit scrap counts and tracks active booster pads (`PADS: X/6`).
- **Fixed HUD Orientation Glitch:** Removed inverted child rotation bugs so tower labels, stats, and text chips remain readable across landscape and portrait shifts.
- **Pause Menu State Integrity:** Navigating into Telemetry, Settings, or Dev Menu from the Pause Menu now strictly preserves paused game state.

### 🔊 Audio Polish & Ambient Filtering
- **Gradual Volume Interpolation:** Volume toggles and pause attenuation now use smooth 600–1000ms gradual fades.
- **Subtle Ambient Synth:** Applied a steep low-pass filter (<650Hz) to background music tracks, keeping synth tones mellow so sharp high frequencies are reserved for weapon impacts and tactical cues.
- **Procedural Stellar SFX:** Tailored laser, flare, and beam synthesis for Stellar towers and upgrade milestones.
- **Sound & Skin Asset Library:** Favorite custom audio and visual setups with Heart/Star markers and export your personalized roster as `exported_skins_sounds_library.json`.

### 🛡️ Platform Hardening & Balance Re-Calibration
- **Firebase Leaderboard Fix:** Resolved the persistent `SECURITY_REJECTED` (HTTP 403) error by attaching the production SHA-1 certificate fingerprint header (`X-Android-Cert`), parsing granular GCP RPC error codes, and implementing an atomic offline retry queue.
- **Early-Game Scrap Velocity Nerf:** Rebalanced bounties across waves 1–100 (Drone: 5, Soldier: 8, Speedster: 10, Bruiser: 20, Disruptor: 18, Boss: 90; wave completion: $35 + 6 \times \text{wave}$) to keep early decisions tense and deliberate.
- **Dev Menu Precision Sliders:** Added direct numeric text input to balance sliders for quick parameter tweaks.
- **Automated Test Suite:** 66/66 automated domain and Robolectric unit tests passing.

---

## 📜 Prior Patch Highlights

### Version 0.0.1.001
- **Spatial Hash Grid Collision Engine (`SpatialHashGrid.kt`):** Replaced brute-force projectile checks with a uniform spatial partitioning grid, locking smooth 60/120 FPS performance during massive swarm waves.
- **Modular UI Refactor:** Separated HUD, upgrade bar, and menus into single-responsibility components.
- **39-Tower Hierarchy:** Completed initial 3 -> 9 -> 27 branching upgrade progression across Kinetic, Elemental, and Siege families.
- **Procedural Level 1–4 Visual Upgrade Cues:** Procedural chassis, stabilizer wings, focus barrels, and Apex radiant coronas.

---

## 📜 Prior Patch Highlights

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
- **Testing:** Robolectric, JUnit 4, AndroidX Test Runner (66/66 passing)
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
- Android APK: `app/build/outputs/apk/debug/app-debug.apk` (and staged under `apk/Dual-Front-TD-v0.0.1.002-debug.apk`)
- Windows Companion: `Dual Front TD.exe` (and staged under `apk/Dual-Front-TD-v0.0.1.002.exe`)
