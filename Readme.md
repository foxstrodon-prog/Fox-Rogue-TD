# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.054-debug.apk`](apk/Dual-Front-TD-v0.0.1.054-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.054 (Build #75)

✨ **3D Tower Overhaul, Turret Muzzle Ballistics, Humanoid Units, Epic Singularity Collapse & AUTO Safety**
- **Clean 3D Paths & Elevated Pedestal Turrets:** Eliminated dark horizontal lines across 3D pathways and redesigned all 3D towers onto uniform elevated octagonal pedestals topped by each class's signature firing assembly (Circle, Triangle, Hexagon, 5-Point Star) complete with orbiting energy dots, recoil, and muzzle flashes.
- **True Turret Muzzle Gunfire:** Projectiles, laser beams, and chain lightning now launch directly from each turret's barrel tip at its true 2D/3D height instead of the pad center.
- **Walking Humanoid Units:** Replaced bug limbs with animated bipedal humanoids—infantry carry glowing energy shields matching their shape weakness and element affinity, while bosses, heavies, and agile raiders sport bold shape emblems directly on their armored torsos.
- **Epic Singularity Collapse & Cleaner Synergy Links:** Singularity collapses now trigger converging vortex dots from every sacrificed tower, earthquake shake, radiating board fractures, flying shatter shards, and a Capstone reconstitution halo. Global synergy lines are darker for visual calm, while linked twin-pad lines adopt their shared pad's color.
- **Safer AUTO-Wave Behavior:** Exiting, backgrounding, or updating the app now toggles **`AUTO`** off so relaunching never auto-starts a new wave unexpectedly (active mid-waves still resume seamlessly).

---

### 📦 Recent Patch History

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

---

#### Version 0.0.1.051 (Build #72)

⚡ **Snappy Damage Numbers, Clean Teleport Indicators, Early Progression & Active-Run Save State**
- **Snappy Floating Damage Numbers:** Tightened hit popup lifetimes with damage-priority throttling and FIFO eviction under a strict active cap so late-game bursts never stall or hide fresh hits.
- **Readable Teleport Pads & 1-Marker Badges:** Selected pads now show a crisp white outer corner reticle instead of recoloring the pad, and each linked pair uses a single matching corner marker (4 corners for Pairs 1–4 plus a Crimson star for Pair 5) matched in the Teleport Pads HUD capsule.
- **Early Pacing & `0 / 0 / 1 / 3` Class Unlocks:** **Elemental Void** is now unlocked from Wave 1 alongside **Kinetic Ballistic**, **Heavy Siege** unlocks at `1` Tier-3 tower, and **Stellar Cosmic** unlocks at `3` Tier-3 towers, with tighter Waves 1–3 cadence and softened Wave 5 mini-bosses.
- **Landscape HUD Polish & Active-Run Save State:** Stacked rotated `AUTO` + `BEGIN` into one matching-width capsule, anchored rotated HUD telemetry popups and Round 1 banners cleanly, and added automatic active-run persistence across app close and process death.
