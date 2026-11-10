# Dual Front TD – Google Play Release Notes & "What's New"

Welcome Commanders to **Dual Front TD**! Here is the latest update log directly from the engineering squad:

---

### 🚀 What's New in Version 0.0.0.012

🏆 **Leaderboard Submission Authorization Fix**
- Resolved token validation and package-restricted API key rejection (`security_retract rejected` / 403 Forbidden).
- Your hard-fought high scores and wave clears now upload cleanly and reliably to global leaderboards with automated background retries.

🔄 **Display Rotation UI Refactoring & Text Layout**
- Top Tactical Bar: Expanded margins and reformatted currency into an icon-stacked display to prevent text clipping upon rotation.
- Bottom Upgrade Menus: Adaptive reorientation! In landscape, tower build and upgrade cards format into full-width stacked rows within the sidebar, ensuring all buttons, stats, and costs remain visible and easy to tap.

⚙️ **Consolidated Settings Hub & Streamlined Dev Menu**
- All audio controls, volume sliders, track selector, gunfire audio testing, skin previews, map themes, and procedural sound profiles are now centralized in the Pause Menu Settings Hub.
- The Dev Balance Console is now dedicated purely to unit balancing values and live telemetry.

🔇 **Instant Focus & Background Audio Termination**
- Audio cuts immediately the instant the app transitions out of active focus (task view, Home gesture, minimization, screen lock).
- Zero lingering background music or trailing combat gunfire/explosions once focus is lost.
- Seamless, glitch-free audio resume when returning to active gameplay.

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
