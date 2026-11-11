# Dual Front TD – Google Play Store Notes & "What's New"

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

---

### 🚀 What's New in Version 0.0.1.002

🌟 **Stellar Star Class & 52-Tower Matrix**
- Unlock Class 4: Stellar Star by constructing 3 Tier-3 defense towers. Features 9 brand-new archetypes including Supernova Pulsar, Singularity Bastion, and Graviton Lance.
- Cosmic damage pierces all creep barriers with an innate +15% damage bonus.
- Encounter new Star creep variants: Stellar Probes, Cosmic Weavers, and Astral Colossi.

⚡ **Booster Pad Teleportation & Ergonomics**
- Added 6 color-paired booster pads (Green, Teal, Orange) on the battlefield.
- Tap the edge buttons to instantly teleport turrets across matching pads on the fly with a 750ms tactical cooldown.
- Earn +3% scrap bounty per occupied booster pad.
- Sleek floating bullet HUD with +25% transparency margin on rotation and fixed text orientation.
- Pause integrity fixed: opening telemetry or settings no longer unpauses the wave.

🔊 **Audio Polish & Low-Pass Synth Filtering**
- Applied smooth 600–1000ms gradual fades when pausing or toggling audio.
- Low-pass filtered ambient background music (<650Hz) to keep background tracks calm and mellow, reserving higher frequencies for punchy combat sound effects.
- New sound and skin asset favorites library with JSON export.

🛡️ **Platform Stability & Re-Balanced Economy**
- Fixed Firebase `SECURITY_REJECTED` (HTTP 403) on leaderboard score submissions with proper SHA-1 certificate verification and an offline retry queue.
- Re-balanced early wave scrap velocity (waves 1–100) and reduced starting fire rates to reward deliberate tactical planning.

---

### 📦 Recent Patch History

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
- **Landscape Drawer Optimization:** Full-width scrolling action cards ensure all upgrade stats remain readable in landscape orientation.

#### Version 0.0.0.010
- **Official Dual Front TD Launch:** Updated app branding and added the one-click Windows companion launcher (`Dual Front TD.exe`).
- **Live Combat DPS:** Real-time damage-per-second readouts on inspected towers with an in-depth telemetry reset window.

---

### 📜 Source Code & Historical APK Archive
Looking for full technical documentation or previous APK versions?
Every release is preserved and open on GitHub:
👉 **[GitHub Releases & APK Archive](https://github.com/Tanner2460/Fox-Rogue-TD/releases)**  
👉 **[Source Code & Developer Docs](https://github.com/Tanner2460/Fox-Rogue-TD)**
