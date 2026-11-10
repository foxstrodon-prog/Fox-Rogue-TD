# Dual Front TD – Patch v0.0.0.013 Release Showcase

🎮 Dual Front TD v0.0.0.013 is officially LIVE! In this update, we deliver a massive audio and visual overhaul: background music now smoothly attenuates upon pause instead of cutting out, in-menu weapon sound testing and track previews are fully restored, background music tracks are extended to 45 seconds with tapered repeat overlaps, alternate skins now completely replace base tower models with dynamic Level 1–4 visual upgrade cues, and towers now feature 15 distinct upgrade audio profiles!

Download the latest Android APK and portable Windows PC runner directly from GitHub:
👉 GitHub Release: https://github.com/Tanner2460/Fox-Rogue-TD/releases
👉 Source Code: https://github.com/Tanner2460/Fox-Rogue-TD

---

⏱️ TIMESTAMPS / VIDEO CHAPTERS:
0:00 - Intro & v0.0.0.013 Audio & Visual Overview
0:40 - Pause Menu Natural Audio Attenuation
1:15 - Direct Sound Menu & Gunfire Audio Testing
1:55 - 45-Second Extended Music Tracks & Tapered Loops
2:40 - Complete Tower Skin Replacements (All 5 Slots)
3:30 - Dynamic Level 1–4 Visual Upgrade Cues
4:15 - 15 Distinct Tower Upgrade Audio Profiles
5:00 - Recent Updates Recap (v0.0.0.012 & v0.0.0.011)
5:45 - How to Download & Play on PC or Android

---

🚀 WHAT'S NEW IN VERSION 0.0.0.013:

🎵 Pause Menu Audio Attenuation & Natural Volume Ducking
• Background music now smoothly lowers in volume (50% / -6dB attenuation) upon opening the Pause Menu instead of cutting out completely.
• Music streams continuously while tuning strategies and restores to full combat volume the instant gameplay resumes.

🔊 Instant Sound Menu & Gunfire Audio Testing
• Repaired testing triggers: tapping any turret gunfire slot immediately auditions that weapon sound variant.
• Music track selection in the sound menu previews and streams directly in the menu without requiring in-game actions.
• Real-time "▶ PREVIEWING" status badge on the active audio stream.

⏳ Extended 45-Second Music Tracks & Tapered Transitions
• All 5 procedural background music tracks now run for a full 45-second duration with rich chord progressions and synthesizer leads.
• Raised-cosine tapered overlaps on repeats and transitions guarantee smooth, click-free looping.

🛡️ Complete Tower Skin Model Replacement & Level 1–4 Visual Cues
• Alternate skins (*Vector Minimal, Blueprint Schematic, Tactical Military, Void Rift, Retro Arcade*) now completely replace the base tower models.
• Every skin features distinctive visual upgrade cues across Level 1 (Base Chassis), Level 2 (Vector Wings), Level 3 (Archetype Twin Barrels / Focus Needles), and Level 4 (Apex Radiant Crown / Corona Halo) that dynamically reflect each tower's class color, vector button color, archetype color, and apex gold.

⚡ 15 Distinct Tower Upgrade Audio Profiles
• Replaced the single generic upgrade sound with 15 unique upgrade audio soundscapes (3 Tower Classes × 5 Skins).
• Kinetic Ballistics, Elemental Void, and Heavy Siege towers each trigger specialized upgrade audio tailored to their active visual skin.
• Full audition test suite integrated into the Pause Menu Audio Studio.

🏆 Leaderboard Submission Authorization Fix
• Resolved package-restricted API key rejection (`security_retract rejected` / 403 Forbidden).
• High scores and wave statistics now upload reliably to global leaderboards with automated retry handling.

🔄 Display Rotation UI Refactoring & Text Layout
• Top Tactical HUD: Expanded margins and stacked currency layout prevent text clipping during device rotation.
• Bottom Upgrade Menus: Adaptive reorientation formats tower build and upgrade cards into full-width stacked rows within the landscape sidebar.

⚙️ Consolidated Settings Hub & Streamlined Dev Menu
• All audio controls, volume sliders, track selector, gunfire audio testing, skin previews, map themes, and procedural sound profiles centralized in the Pause Menu Settings Hub.
• Dev Balance Console stripped of duplicate audio/visual pickers and dedicated purely to unit balancing values and live telemetry.

🔇 Instant Background & Focus Audio Termination
• Audio cuts immediately the millisecond the app transitions out of active focus (task view, Home gesture, minimization, screen lock).
• Zero lingering background music or trailing combat SFX/explosions once focus is lost.
• Clean, glitch-free audio resume when returning to active gameplay.

🎵 Extended Music Rotation & Seamless Track Looping
• Eliminated abrupt 5-second track cycling!
• Music playback per track now targets between 45 seconds and 1 minute 15 seconds (60s default window).
• Procedural synthesizer tracks loop smoothly and transition cleanly without jarring cuts or clicks.

🎚️ Dedicated Tower Upgrade Audio Volume Slider
• Separated tower upgrade sound effects from combat weapons and master SFX channels into an independent bus.
• Live volume slider in the Pause Menu provides real-time auditory feedback as you tune.

🛡️ Inline Turret Skin Visual Preview Thumbnails
• Inspect the visual mesh, chassis style, and firing flourishes before committing to a selection!
• Real-time visual thumbnails rendered directly inside the Pause Menu for all 5 skin slots:
  - Slot 1: Vector Minimal (Default neon cyan chassis)
  - Slot 2: Blueprint Schematic (Technical crosshairs & drafting rings)
  - Slot 3: Tactical Military (Heavy slate armor & corner reinforcement rivets)
  - Slot 4: Void Rift (Astral purple corona & pulsing singularity)
  - Slot 5: Retro Arcade (8-bit stepped pixel corners & golden brackets)

🎨 Strict Map & Lane Theme Contrast Audit
• Complete visual separation across all 5 procedural map biomes:
  - Path 1 (Alpha), Path 2 (Beta), and their intersection zones now use distinct, high-contrast tri-color palettes.
  - Zero shared or mimicking hues between converging conduits or junctions.
  - Dynamic intersection tile glow shifts to match the theme's unique junction color.

---

📦 RECENT UPDATES RECAP:

🔥 Version 0.0.0.011:
• Complete procedural audio synthesis engine with 15 gunfire variants (5 slots across Kinetic, Elemental, and Siege).
• Dynamic combat ducking and automatic pause menu volume attenuation.
• 5 map and lane environment themes with in-game selection studio.
• Dynamic rotating HUD text chips and action buttons synchronized with device tilt.
• Full 39-tower Dev Console with live stat editing and diff-only JSON exports.

🔥 Version 0.0.0.010:
• Official rebrand to "Dual Front TD" with single root Windows executable (`Dual Front TD.exe`).
• Accurate real-time DPS readouts on all defense towers with a Telemetry Reset Window button.
• Expanded 16-row battlefield grid bringing the map flush against the top tactical HUD.
• Firebase Google Services integration with high-reliability global leaderboards.

---

📜 HISTORICAL PATCH NOTES & SOURCE CODE:
Looking for older changelogs (v0.0.0.009 and earlier), developer balance documentation, or source code?
Visit our official repository:
👉 https://github.com/Tanner2460/Fox-Rogue-TD/releases

#DualFrontTD #TowerDefense #IndieGameDev #AndroidGaming #JetpackCompose #Kotlin #MobileGaming #GamingUpdate
