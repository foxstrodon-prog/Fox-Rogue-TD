# Dual Front TD – Google Play Release Notes & "What's New"

Welcome Commanders to **Dual Front TD**! Here is the latest update log directly from the engineering squad:

---

### 🚀 What's New in Version 0.0.0.013

🎵 **Pause Menu Audio Attenuation & Natural Volume Ducking**
- Background music now smoothly lowers in volume (50% / -6dB attenuation) upon opening the Pause Menu instead of muting completely.
- Music streams continuously while tuning strategies and restores to full combat volume the instant gameplay resumes.

🔊 **Instant Sound Menu & Gunfire Audio Testing**
- Repaired testing triggers: tapping any turret gunfire slot immediately auditions that weapon sound variant.
- Music track selection in the sound menu previews and streams directly in the menu without requiring in-game actions.
- Real-time "▶ PREVIEWING" status badge on the active audio stream.

⏳ **Extended 45-Second Music Tracks & Tapered Transitions**
- All 5 procedural background music tracks now run for a full 45-second duration with rich chord progressions and synthesizer leads.
- Raised-cosine tapered overlaps on repeats and transitions guarantee smooth, click-free looping.

🛡️ **Complete Tower Skin Model Replacement & Level 1–4 Visual Cues**
- Alternate skins (*Vector Minimal, Blueprint Schematic, Tactical Military, Void Rift, Retro Arcade*) now completely replace the base tower models.
- Every skin features distinctive visual upgrade cues across Level 1 (Base Chassis), Level 2 (Vector Wings), Level 3 (Archetype Twin Barrels / Focus Needles), and Level 4 (Apex Radiant Crown / Corona Halo) that dynamically reflect each tower's class color, vector button color, archetype color, and apex gold.

⚡ **15 Distinct Tower Upgrade Audio Profiles**
- Replaced the single generic upgrade sound with 15 unique upgrade audio soundscapes (3 Tower Classes × 5 Skins).
- Kinetic Ballistics, Elemental Void, and Heavy Siege towers each trigger specialized upgrade audio tailored to their active visual skin.
- Full audition test suite integrated into the Pause Menu Audio Studio.

---

### 📦 Recent Updates Recap

#### Version 0.0.0.012
- **Leaderboard Authorization Fix**: Resolved token validation and package-restricted API key rejection (`security_retract rejected`).
- **Display Rotation UI Refactoring**: Top bar margin expansion, stacked currency icon, and landscape adaptive drawer layouts.
- **Settings Menu Consolidation**: Centralized audio controls and skin pickers in the Pause Menu Settings Hub.
- **Instant Focus Audio Termination**: Audio cuts immediately when app transitions out of active focus.

🎵 **Extended Music Rotation & Seamless Track Looping**
- Eliminated abrupt 5-second track cycling!
- Music playback per track now targets between 45 seconds and 1 minute 15 seconds (60-second default cycle).
- Procedural synthesizer tracks loop smoothly and transition cleanly without jarring cuts or clicks.

🎚️ **Dedicated Tower Upgrade Audio Volume Slider**
- Separated tower upgrade sound effects from combat weapons and master SFX channels into an independent channel.
- Live volume slider in the Pause Menu provides real-time auditory feedback as you tune.

🛡️ **Inline Turret Skin Visual Preview Thumbnails**
- Inspect the visual mesh, chassis style, and firing flourishes before committing to a selection!
- Real-time visual thumbnails rendered directly inside the Pause Menu for all 5 skin slots:
  - Slot 1: Vector Minimal (Default neon cyan chassis)
  - Slot 2: Blueprint Schematic (Technical crosshairs & drafting rings)
  - Slot 3: Tactical Military (Heavy slate armor & corner reinforcement rivets)
  - Slot 4: Void Rift (Astral purple corona & pulsing singularity)
  - Slot 5: Retro Arcade (8-bit stepped pixel corners & golden brackets)

🎨 **Strict Map & Lane Theme Contrast Audit**
- Complete visual separation across all 5 procedural map biomes:
  - Path 1 (Alpha), Path 2 (Beta), and their intersection zones now use distinct, high-contrast tri-color palettes.
  - Zero shared or mimicking hues between converging conduits or junctions.
  - Dynamic intersection tile glow shifts to match the theme's unique junction color.

---

### 📦 Recent Updates Recap

#### Version 0.0.0.011
- **Procedural Sound & Skins Studio**: 15 gunfire variants (5 slots across Kinetic, Elemental, and Siege), dynamic combat ducking, and pause menu attenuation.
- **Dynamic Rotating HUD**: Text chips and action buttons dynamically rotate to match device orientation with smooth animation.
- **Complete 39-Tower Dev Console Coverage**: Live fine-tuning sliders for all 39 towers with diff-only JSON exports.
- **Decoupled Simulation Clock**: Turret cooldowns synchronize seamlessly at 3.5x, 5x, and 10x turbo speeds.

#### Version 0.0.0.010
- **Official Rebrand to Dual Front TD**: Updated application branding with single-root Windows executable (`Dual Front TD.exe`).
- **Accurate Live Combat DPS & Telemetry Reset**: Real-time DPS calculation and a dedicated "Reset Window" button to diagnose tower telemetry over focused test windows.
- **16-Row Battlefield & Smart Spawner Advice**: Expanded grid verticality and intelligent Wave 1 deployment hints.
- **Firebase Google Services Integration**: Hardened global leaderboards and analytics.

---

### 📜 Older Patch Notes & Source Code
Looking for historical patch notes (v0.0.0.009 and earlier), developer balance documentation, or source code?
Visit our official GitHub repository and releases page:
👉 **[GitHub Releases: Dual Front TD](https://github.com/Tanner2460/Fox-Rogue-TD/releases)**  
👉 **[Source Code & Docs](https://github.com/Tanner2460/Fox-Rogue-TD)**
