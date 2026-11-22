# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.056-debug.apk`](apk/Dual-Front-TD-v0.0.1.056-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.056 (Build #77)

🎛️ **Pause Menu Streamlining, Secret Tools Relocation & Unified Rotating BGM Playlist**
- **Single-Line Pause Menu Music Bar & Unified Playlist:** All 29 unique background music tracks now rotate in a single unified playlist, controlled from a one-line bar in the Pause Menu with **Thumbs Up** (like) and **Thumbs Down** (skip immediately; 3 dislikes removes the track persistently until restored in Music settings).
- **Merged End Run / Generate New Map & Footer:** One adaptive button now switches between **`GENERATE NEW MAP`** pre-run and **`END RUN / GENERATE NEW MAP`** mid-run (with confirmation options to end only or end & generate a new map), paired with a unified bottom update/version footer.
- **Secret 10-Tap Tools Relocation:** Moved **Sound & Skin Vault** and **Dev Balance Console** into the 10-tap **Generator Studio** secret menu.

---

### 📦 Recent Patch History

#### Version 0.0.1.055 (Build #76)

🏛️ **Tile-Contained 3D Pillar/Obelisk Towers, Display-Rotation Base Shift, Hub 3D Rig Selector & Full Run Telemetry**
- **Tile-Contained 3D Pillars & Obelisks:** Shifted 3D tower bases downward into their tile footprint, shortened turret barrels, and added three distinct pedestal silhouettes (**Square Obelisk**, **Rectangle Monolith**, **Circle Fluted Pillar**).
- **Rotation-Aware 3D Bases:** 3D tower bases, shadows, obstacles, and muzzle offsets now shift smoothly when rotating the display orientation.
- **Customizations Hub 3D Rig Selector:** Added an in-hub **3D Pillar & Obelisk Rig** preset selector when Isometric 3D mode is active.
- **Expandable Full Run Telemetry & Tower Ledger:** Inspect wave-by-wave checkpoints, collapse waves, fatal enemy details, and per-tower damage/kill ledgers directly in Telemetry & Leaderboards for both live and completed runs.

---

#### Version 0.0.1.054 (Build #75)

✨ **3D Tower Overhaul, Turret Muzzle Ballistics, Humanoid Units, Epic Singularity Collapse & AUTO Safety**
- **Clean 3D Paths & Elevated Pedestal Turrets:** Eliminated dark horizontal lines across 3D pathways and redesigned all 3D towers onto uniform elevated octagonal pedestals topped by each class's signature firing assembly (Circle, Triangle, Hexagon, 5-Point Star) complete with orbiting energy dots, recoil, and muzzle flashes.
- **True Turret Muzzle Gunfire:** Projectiles, laser beams, and chain lightning now launch directly from each turret's barrel tip at its true 2D/3D height instead of the pad center.
- **Walking Humanoid Units:** Replaced bug limbs with animated bipedal humanoids—infantry carry glowing energy shields matching their shape weakness and element affinity, while bosses, heavies, and agile raiders sport bold shape emblems directly on their armored torsos.
- **Epic Singularity Collapse & Cleaner Synergy Links:** Singularity collapses now trigger converging vortex dots from every sacrificed tower, earthquake shake, radiating board fractures, flying shatter shards, and a Capstone reconstitution halo. Global synergy lines are darker for visual calm, while linked twin-pad lines adopt their shared pad's color.
- **Safer AUTO-Wave Behavior:** Exiting, backgrounding, or updating the app now toggles **`AUTO`** off so relaunching never auto-starts a new wave unexpectedly (active mid-waves still resume seamlessly).

---

#### Version 0.0.1.053 (Build #74)

🧊 **Opt-In 3D Isometric Renderer, Multi-Part Procedural Enemies & Studio Rig Customization**
- **Opt-In 2D / 3D Render Mode:** Switch live between classic Tactical 2D and extruded 3D Isometric views from the Pause Menu or Customizations Hub (purely cosmetic and leaderboard-safe) with zero hitbox drift.
- **Multi-Part Procedural Enemy Creatures:** Enemies now animate with articulated parts, 4 threat tiers (`Swarm`, `Standard`, `Elite`, `Boss`), and dual-layer class-weakness + element-affinity coloring while preserving Circle / Triangle / Hexagon / Star silhouettes.
- **Generator Studio 3D Towers & Enemies Tabs:** Generate, share, and equip versioned `DFTD-T32-` 3D turret rigs and shape-locked `DFTD-EN2-` creature archetypes directly from the Studio.

---

#### Version 0.0.1.052 (Build #73)

⬡ **Hexagon Enemies, Quadrant Pad Spacing, Split Pair Auras, Enemy Info Card & Run Telemetry**
- **Hexagon Bruisers & Clean HUD Polish:** Armored tank enemies now render as sharp **Hexagons** matching Heavy Siege towers, teleport link lines are sleeker, rotation tips auto-dismiss after 5 seconds, and rotated surplus gold banners align cleanly.
- **Quadrant-Balanced 10-Pad Maps:** Procedural layouts now distribute `2–3` buildable pads per quadrant (`5/5` Left/Right and Top/Bottom halves) while preserving zero side-by-side clumping and `7/12` dual-lane length balance.
- **Split Pair/Global Tier-3 Auras & 3 New Enemies:** Tier-3 auras now grant `100%` power to your linked twin pad, `35%` globally, and `40%` from the Singularity Vault, paired with a smoother Medium late-game HP curve and 3 new enemies (**Warp Strider**, **Aegis Dreadnought**, **Phase Leviathan**).
- **Tap-to-Inspect Enemy Card & 5-Wave Telemetry:** Tap any enemy on the field to inspect its live HP, Armor, Speed, Bounty, Strengths, and Weaknesses, and view/copy 5-wave run checkpoints directly from the Telemetry dialog.
