# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.045-debug.apk`](apk/Dual-Front-TD-v0.0.1.045-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.045

🛠️ **Leaderboard & Cross-Version Auto-Updater Restoration, Centered Squircles & Canvas Polish**
- **Clean Flowing Dots & Crimson Red Core Endpoint:** Removed comet trails behind lane flow dots and replaced the Core exit heart icon with a permanent crimson red octagonal nexus.
- **Centered Target & Sell Squircles:** Vertically centered the `First` (Target Priority) and `Sell` squircles on the exact horizontal midline of the upgrade squircles across all screen orientations and fixed Tier-1 `CAP` aura badges.
- **Leaderboard & Cross-Version Update Fixes:** Automatically migrates legacy balance preferences so upgraded installs aren't falsely flagged as `DEV TUNED`, credits sacrificed Singularity towers for MVP, restores live GitHub Release auto-updates across all versions with `/releases` fallback and Pause Menu status feedback, and runs the 63-wave Headless Simulator off the main thread.

---

### 📦 Recent Patch History

#### Version 0.0.1.044

🎯 **Tactical HUD Ergonomics, 10x21 Full-Screen Grid, Portal/Core Visuals & Data-Driven Balance**
- **Combined Wave Capsule & Centered Upgrade Squircles:** Merged `BEGIN` and `AUTO` into a single Split-Action Wave Capsule, flanked `Target` and `Sell` to the screen edges to prevent accidental sell taps while spamming upgrades, and added Active/Banked Aura Pip Badges (`★ NEW`, `● 1x`, `⚡ 2B`, `CAP 1x`) on Tier-2/Tier-3 upgrade previews.
- **10x21 Map Grid & Directional Portals/Core:** Expanded the battlefield to `10x21` tiles, added App-Logo Prism Entry Portals, a Shielded Heart Defense Core exit, and 24 comet-tail lane flow particles, and tuned wave gold decay & HP curves to hit `1 / 2–3 / 4–5` Singularity Collapses across Easy/Medium/Hard.
- **End-of-Run Collapse Readouts, Headless Sim & 4 Fresh Build #65 Variants:** Added `Collapses`, `End Gold`, and `Auras Banked` to Victory/Defeat summaries, a 63-wave Headless Balance Simulator, whole-run tower balance telemetry, and 4 new Build #65 skins/themes/audio variants (`260/260` unit tests passing).

---

#### Version 0.0.1.043

⚡ **Speed-Invariant 10X Simulation, Smoother Loaded-Board Pacing & New Build #64 Variants**
- **True `10X` Speed Parity:** Replaced coarse simulation sub-stepping and spawn-timer drift with a fixed `20ms` physics accumulator and remainder-preserving wave spawner, ensuring `10X` maintains the exact same wave density and combat difficulty as `1X` even on tower-loaded boards.
- **Smoother High-Speed Frame Pacing:** Batched Compose state updates once per rendered frame and subtracted simulation compute time from frame sleep, eliminating UI stutter at `10X`.
- **4 Fresh Build #64 Variants + Build #63 Archived:** Automatically adds 4 new Turret Skins, Map Themes, BGM Tracks, SFX Packs, and 32 per-class Fire/Upgrade sounds while archiving Build #63 (`255/255` unit tests passing).

---

#### Version 0.0.1.042

🔄 **Automatic Build-Driven Sound & Skin Generation + Multi-Build Archives**
- **4 Fresh Variants Every Build:** Every `versionCode` increment (`Build #63 / v0.0.1.042` and beyond) now automatically generates 4 new Turret Skins, 4 Map & Lane Themes, 4 BGM Tracks, 4 SFX Packs, 16 per-class Turret Fire sounds, and 16 Turret Upgrade chimes while keeping your Default Core untouched.
- **Automatic Multi-Build Archives:** Previous releases (`Build #60`, curated `Build #61`, `Build #62`, and beyond) automatically move into the Sound & Skin Vault's **Archives** tab with build badges and full live audio/skin preview, equip, and rating support.
- **Dynamic Customization & Rolling Playlist:** Audio & Visual Customization headers, rolling BGM playlists, and `SoundPool` preloading now track the active build dynamically (`252/252` unit tests passing).

---

#### Version 0.0.1.041

⚛️ **Singularity Remaps, Synergy HUD & Skin Previews, Balance Overhaul & Hexagon Siege**
- **Bullet HUD Synergies & Singularity Tracking:** The synergy pill now counts duplicate tower sources, splits Active Board Synergies from the Singularity Tower's vaulted stacks in a dedicated secondary section, and scrolls cleanly across all 4 screen rotations.
- **Singularity New Map & Booster Preservation:** Collapsing into a Singularity now generates a brand-new battlefield around your preserved anchor spot (or both linked pads if built on a Booster Pair) and safely resets active wave enemies to the new spawners.
- **Live Skin & Map Theme Previews:** Preview all 4 tower shapes side-by-side for any Turret Skin and inspect dual-lane Map & Lane Environment themes live before equipping in Customization and the Sound & Skin Vault.
- **Faster 1X Pace & Rebalanced Difficulty:** Increased baseline `1X` game speed (`1.85x`) with anti-tunneling sub-stepping, curbed late-game scrap inflation, and steepened Easy/Medium/Hard scaling while updating Kinetic Tier-4 aura pulses, Heavy Siege Hexagon (`⬢`) silhouettes, and glowing blank buildable pads (`250/250` unit tests passing).
