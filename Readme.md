# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.047-debug.apk`](apk/Dual-Front-TD-v0.0.1.047-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.047 (Build #68)

🎨 **Procedural Visual Cosmetics Generator, Independent HUD Themes & Canvas Polish**
- **Studio Visual Generation & v2 Recipe Codes:** Enabled **GENERATE** and live **AUDITION** previews for **Turret Skins**, **Map Themes**, and **Interface (HUD)** palettes in the Generator Studio, backed by deterministic legibility guards (contrast, lane luminance gap, and dual-lane hue separation) and shareable `DFTD-SK2-` / `DFTD-MP2-` / `DFTD-HD2-` codes.
- **Independent HUD Equipping:** Decoupled HUD appearance from map themes with **EQUIP** buttons on the Hub's **Interface** tab and persistent selection (while continuing to follow your active map theme if you haven't picked one yet).
- **Canvas Frame-Time Optimizations:** Synced shake and lightning jitter to simulation animation time, added O(1) tile tap lookup, memoized map palette resolution, and eliminated per-tower/per-enemy draw allocations (`286/286` unit tests passing).

---

### 📦 Recent Patch History

#### Version 0.0.1.046 (Build #67)

🎛️ **Curated Customizations Hub (`BETA: ALL UNLOCKED`), Hidden Audio Generator Studio & Low-End Clarity**
- **4-Tab Customizations Hub:** Browse **Towers**, **Maps**, **Interface**, and **Music** with live interactive tower-firing previews across all 4 classes and 7 turret models, instant gunfire sound auditioning, unlock-status badges, and inline 👍/👎 ratings synced to Firebase.
- **Frozen Curated Catalog:** Replaced per-build auto-generation with a frozen curated catalog (`Build #66` baseline) so your equipped cosmetics, per-class skins, and vote histories stay stable across updates.
- **Hidden Generator Studio & Recipe Codes:** Tap the Pause Menu version badge 10 times to open the Generator Studio—generate, name, audition, and share CRC32-checked **BGM** and **SFX** Recipe Codes (`DFTD-...`), backed by smoothed synth crossfades and fade-outs that eliminate low-end speaker crackle (`280/280` unit tests passing).

---

#### Version 0.0.1.045 (Build #66)

🛠️ **Leaderboard & Cross-Version Auto-Updater Restoration, Centered Squircles & Canvas Polish**
- **Clean Flowing Dots & Crimson Red Core Endpoint:** Removed comet trails behind lane flow dots and replaced the Core exit heart icon with a permanent crimson red octagonal nexus.
- **Centered Target & Sell Squircles:** Vertically centered the `First` (Target Priority) and `Sell` squircles on the exact horizontal midline of the upgrade squircles across all screen orientations and fixed Tier-1 `CAP` aura badges.
- **Leaderboard & Cross-Version Update Fixes:** Automatically migrates legacy balance preferences so upgraded installs aren't falsely flagged as `DEV TUNED`, credits sacrificed Singularity towers for MVP, restores live GitHub Release auto-updates across all versions with `/releases` fallback and Pause Menu status feedback, and runs the 63-wave Headless Simulator off the main thread.

---

#### Version 0.0.1.044 (Build #65)

🎯 **Tactical HUD Ergonomics, 10x21 Full-Screen Grid, Portal/Core Visuals & Data-Driven Balance**
- **Combined Wave Capsule & Centered Upgrade Squircles:** Merged `BEGIN` and `AUTO` into a single Split-Action Wave Capsule, flanked `Target` and `Sell` to the screen edges to prevent accidental sell taps while spamming upgrades, and added Active/Banked Aura Pip Badges (`★ NEW`, `● 1x`, `⚡ 2B`, `CAP 1x`) on Tier-2/Tier-3 upgrade previews.
- **10x21 Map Grid & Directional Portals/Core:** Expanded the battlefield to `10x21` tiles, added App-Logo Prism Entry Portals, a Shielded Heart Defense Core exit, and 24 comet-tail lane flow particles, and tuned wave gold decay & HP curves to hit `1 / 2–3 / 4–5` Singularity Collapses across Easy/Medium/Hard.
- **End-of-Run Collapse Readouts, Headless Sim & 4 Fresh Build #65 Variants:** Added `Collapses`, `End Gold`, and `Auras Banked` to Victory/Defeat summaries, a 63-wave Headless Balance Simulator, whole-run tower balance telemetry, and 4 new Build #65 skins/themes/audio variants (`260/260` unit tests passing).

---

#### Version 0.0.1.043 (Build #64)

⚡ **Speed-Invariant 10X Simulation, Smoother Loaded-Board Pacing & New Build #64 Variants**
- **True `10X` Speed Parity:** Replaced coarse simulation sub-stepping and spawn-timer drift with a fixed `20ms` physics accumulator and remainder-preserving wave spawner, ensuring `10X` maintains the exact same wave density and combat difficulty as `1X` even on tower-loaded boards.
- **Smoother High-Speed Frame Pacing:** Batched Compose state updates once per rendered frame and subtracted simulation compute time from frame sleep, eliminating UI stutter at `10X`.
- **4 Fresh Build #64 Variants + Build #63 Archived:** Automatically adds 4 new Turret Skins, Map Themes, BGM Tracks, SFX Packs, and 32 per-class Fire/Upgrade sounds while archiving Build #63 (`255/255` unit tests passing).
