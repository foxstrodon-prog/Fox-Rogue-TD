# Dual Front TD – Google Play Release Notes & "What's New"

Welcome Commanders to **Dual Front TD**! Here is the latest update log directly from the engineering squad.

---

### 🚀 What's New in Version 0.0.0.011

🎵 **Complete Sound System Overhaul & Audio Architecture**
- **Seamless Music Switching**: Swapping tracks in the menu now instantly switches the active playback stream with zero buffer locks.
- **Dynamic Combat Ducking**: Background music automatically ducks during heavy gunfire and explosive impacts to preserve crisp audio clarity.
- **Pause Menu Attenuation & Real-Time Slider Linking**: Background music drops to an ambient level when entering the pause state, and moving the volume sliders provides real-time auditory feedback.
- **Rolling Music Playlist**: 5 procedural synth tracks (*Sector Zero Synth, Dual Front Pulse, Sub-Void Drone, Hyper-Grid Ambient, and Mute*) cycle seamlessly.
- **Turret Gunfire Audio Restoration**: Restored dedicated firing sound effects across all 3 turret archetypes with voice concurrency limits to prevent audio clipping.
- **15 Procedural Gunfire SFX Testing Slots**: 5 gunfire audio candidate slots per turret type with instant test triggers.

🎨 **Aesthetic Lab & Visual Skin Pipelines**
- **5 Turret Visual Skin Slots**: Default Production meshes plus 4 experimental skins (*Vector Minimal, Cyber Neon, Heavy Industrial, Void Glyph*) with distinct geometric silhouettes and animated firing flourishes.
- **5 Map & Lane Environment Theme Slots**: Default Cyber Grid plus 4 procedural biome palettes (*Neon Grid, Solarized Desert, Deep Void, Obsidian Magma*).
- **Pause Menu Customization Studio**: Elevated audio and skin customization out of the hidden Dev Menu into an accessible, player-facing Pause Menu subsection.
- **Delta-State Balance & Asset Export**: Export strict diff-only JSON containing exclusively your modified unit and asset overrides.

🔄 **Fixed Canvas Orientation & Rotating Tactical HUD**
- Map grid and combat pathways now stay firmly locked in place while rotating your device, preventing disorientation during tense waves.
- HUD chips and commander action buttons dynamically rotate to match your screen orientation (0°, 90°, 180°, 270°) with silky-smooth animations.

🛠️ **Complete 39-Tower Dev Console Coverage**
- Live fine-tuning sliders for all 39 towers across Tier 1, Tier 2, and Tier 3 evolutions (Damage, Range, Attack Rate, Deployment Cost).

⏱️ **Decoupled Simulation Clock (3.5x, 5x, 10x Turbo Speeds)**
- Weapon fire cooldowns and tactical environmental elements now synchronize seamlessly at ultra-fast gameplay speeds.

---

### 📦 Recent Updates Recap

#### Version 0.0.0.010
- **Official Rebrand to Dual Front TD**: Updated application identity, branding, and clean single-root Windows executable.
- **Accurate Live Combat DPS & Telemetry Reset**: Real-time DPS calculation and a dedicated "Reset Window" button to diagnose tower telemetry over focused test windows.
- **16-Row Battlefield & Smart Spawner Advice**: Expanded grid verticality and intelligent Wave 1 deployment hints.
- **Firebase Google Services Integration**: Hardened global leaderboards and analytics.

#### Version 0.0.0.009
- **Lifecycle & Stability**: Hardened reflective instantiation safety across all ViewModels and background sync workers (`@JvmOverloads`).
- **Launcher Resilience**: Enhanced PC desktop launcher device detection for wireless and mDNS ADB configurations.

---

### 📜 Older Patch Notes & Source Code
Looking for historical patch notes (v0.0.0.008 and earlier), developer balance documentation, or source code?
Visit our official GitHub repository and releases page:
👉 **[GitHub Releases: Dual Front TD](https://github.com/Tanner2460/Fox-Rogue-TD/releases)**  
👉 **[Source Code & Docs](https://github.com/Tanner2460/Fox-Rogue-TD)**
