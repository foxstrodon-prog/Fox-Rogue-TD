# Dual Front TD – Google Play Store Notes & "What's New"

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

---

### 🚀 What's New in Version 0.0.1.004

⚙️ **Build System Hardening & Modernized CI Pipeline**
- Upgraded the automated build and release pipeline to native Node 24 actions (`actions/setup-java@v5` and `gradle/actions/setup-gradle@v4`).
- Added build runner pinning to `ubuntu-24.04` LTS to ensure deterministic builds.
- Implemented workflow concurrency groups and idempotent release uploading to eliminate duplicate release collisions.

🎬 **In-Game Feature Showcase Mode Polish**
- In-game feature cinematics now feature automated gameplay demonstrations (targeting reticles, booster pad swaps, and squircle upgrade inspections).
- Added full screen touch locking during showcase playback to guarantee smooth, uninterruptible presentations.

---

### 📦 Recent Patch History

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
