# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.058-debug.apk`](apk/Dual-Front-TD-v0.0.1.058-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.058 (Build #79)

🎛️ **Pause Menu Layout, Volume Badge, Mandatory Save Reset & 3D Preview Stage**
- **Mandatory Score Submission on Active Run Reset:** Resetting an active run after Wave 1 now asks for confirmation and always saves your score before generating a new map (pre-Wave-1 resets still regenerate immediately without logging empty runs).
- **Live 3D Tower Previews in Customizations:** Toggling **3D Isometric** in Audio & Visual Customization now renders live 3D pillar/obelisk tower models right inside the tower showcase and skin previews, complete with a `2D` / `3D` badge.
- **Front-and-Center Volume Badge:** Moved the Volume Mixer out of the Music tab into a clickable **`VOLUME`** badge next to the version number below `"GAME PAUSED"`.
- **Streamlined Pause Menu Order:** Placed **`END RUN & GENERATE NEW MAP`** right under **`RESUME BATTLE`**, and moved **`AUDIO & VISUAL CUSTOMIZATION`** just above the **`Dual Front TD (Build #79) Check for Updates`** button.

---

### 📦 Recent Patch History

#### Version 0.0.1.057 (Build #78)

🎛️ **Pause Menu Polish & Cleaner Controls**
- **Single-Line Update & Build Button:** Combined the bottom Pause Menu footer into one clean button (`"Dual Front TD (Build #78) Check for Updates"`).
- **Simplified Difficulty & Action Labels:** Renamed the difficulty section header to **`DIFFICULTY`**, removed the extra descriptive text beneath it, and unified the map-reset button label as **`END RUN & GENERATE NEW MAP`** (preserving pre-run instant generation and mid-run confirmation safety).
- **2D / 3D Toggle in Customization Hub:** Moved the 2D / 3D render mode switch solely into **Audio & Visual Customization** with a compact inline **`RENDER MODE`** label.

---

### 📦 Recent Patch History

#### Version 0.0.1.056 (Build #77)

🎛️ **Pause Menu Streamlining, Secret Tools Relocation & Unified Rotating BGM Playlist**
- **Single-Line Pause Menu Music Bar & Unified Playlist:** All 29 unique background music tracks now rotate in a single unified playlist, controlled from a one-line bar in the Pause Menu with **Thumbs Up** (like) and **Thumbs Down** (skip immediately; 3 dislikes removes the track persistently until restored in Music settings).
- **Merged End Run / Generate New Map & Footer:** One adaptive button now switches between **`GENERATE NEW MAP`** pre-run and **`END RUN / GENERATE NEW MAP`** mid-run (with confirmation options to end only or end & generate a new map), paired with a unified bottom update/version footer.
- **Secret 10-Tap Tools Relocation:** Moved **Sound & Skin Vault** and **Dev Balance Console** into the 10-tap **Generator Studio** secret menu.

---

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
