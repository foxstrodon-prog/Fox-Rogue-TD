# Dual Front TD – Google Play Store Notes & "What's New"

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

---

### 🚀 What's New in Version 0.0.1.012

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
👉 **[GitHub Releases & APK Archive](https://github.com/Tanner2460/Fox-Rogue-TD/releases)**  
👉 **[Source Code & Developer Docs](https://github.com/Tanner2460/Fox-Rogue-TD)**
