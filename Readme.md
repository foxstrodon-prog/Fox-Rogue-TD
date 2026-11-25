# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Latest Android APK (`v0.0.1.061`):** [`apk/Dual-Front-TD-v0.0.1.061-debug.apk`](apk/Dual-Front-TD-v0.0.1.061-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.061 (Build #82)

🎯 **Compact HUD Gestures, Upper Tip Slot, Twin Pad Links & Milestone Tips**
- **Compact HUD Stack, Slide & Clock Shortcuts:**
  - Enlarged the HUD clock touch target for reliable single-tap Pause/Settings access, and added double-tap on the clock to shrink the top HUD (double-tap the credits/health row to expand it back).
  - Reorganized the compact HUD with the wave control pill stacked above side-by-side credits and Core HP capsules, and enabled vertical sliding while keeping clear of the teleport edge tabs.
- **Unified Upper Tip Slot & Queued Coaching:**
  - Moved all tactical tips and the surplus credits reminder to the upper quarter of the screen across every rotation and orientation, automatically stepping clear of HUDs, cards, banners, and edge tabs.
  - Tapping any tip—including the surplus credits reminder—dismisses it cleanly, while back-to-back milestone tips queue in order instead of getting dropped.
- **Tier-3 Milestone & Round 4 Tips:**
  - Added dedicated tips when reaching your 1st, 2nd, and 3rd max-level towers (**Heavy** unlocked, gold pulse & synergy links, and **Stellar** unlocked) plus a Round 4 tip introducing the compact HUD double-tap shortcut.
- **Linked Teleport Pad Preview & Core Card Polish:**
  - Selecting a non-max tower on a teleport pad now draws a dashed line and ring to its matching twin pad so you can preview teleport swaps before tapping.
  - Updated **Singularity Core** incoming enemy rows to display across two lines cleanly with scrollable overflow in compact landscape layouts.

---

### 📦 Recent Patch History

#### Version 0.0.1.060 (Build #81)

🌀 **Singularity Core Card, Adaptive Wave HUD, Tower Info Cards & Early-Game Coach Marks**
- **Singularity Core Card & Open Portal Visuals:**
  - Restyled the lane-end **Singularity Core** in both 2D and 3D modes as an open crimson prism portal with inward-flowing approach chevrons.
  - Tap the Singularity Core to inspect live Core HP, upcoming wave enemies with shape weaknesses, the `x1.2` / `x0.5` shape multiplier rule, and rotating tactical tips without blocking adjacent tower or pad taps.
- **Maxed Tower Info Card & Larger Upgrade Squircles:**
  - Selecting any Tier-3, Final Form, or **Synergy Conduit** tower displays a compact inspection card showing class, shape, and element strengths, weaknesses, active aura counts, and a direct button to open the **Synergies Menu**.
  - Enlarged build and upgrade squircles for easier tapping and moved **Target Priority** and **Sell** controls into a cleaner vertical action column.
- **Single Adaptive Wave Button & Compact Top HUD:**
  - Unified the Wave Launch and `AUTO` controls into a single adaptive wave pill, and added a compact HUD collapse/expand toggle (`-` / `+`) that trims the top bar down to wave state, pause gear, and Synergy Conduit status.
  - Opening the **Synergies Menu** now pauses the battlefield cleanly and remains accessible even while the top HUD is collapsed.
- **Smart Early-Game Coach Marks:**
  - Added context-aware, non-intrusive tips highlighting a clear pair of teleport pads before Wave 1, pointing to the edge teleport tabs after placing your first tower, reminding you to warp idle towers across fronts, and explaining Heavy/Stellar unlock milestones at 6 and 8 towers.
- **Visual Clarity & Bug Fixes:**
  - Updated user-visible capstone terminology to **Synergy Conduit** and **Synergies Menu**, slowed synergy link pulse dots for clearer readability, and fixed floating damage text lingering when paused or between waves.

---

### 📦 Recent Patch History

#### Version 0.0.1.059 (Build #80)

✨ **Single-Line Pause Header, Upgraded Synergy Beams & Numbered Cosmetics**
- **Single-Line Pause Header:** Moved the version badge and a cleaner percentage-free **`VOLUME`** / **`MUTED`** badge directly inline onto the **`GAME PAUSED`** header row next to the **`X`** button.
- **Upgraded Synergy & Linked-Twin Beams:** All **global aura** beams now use the sleek dual-layer beam style with larger traveling pulse dots, while **matching-pad twin** links use the thicker rounded teleport-beam style.
- **Sequential `#1..#N` Numbering & Cleaner Catalogs:** Removed repeating `2–5` slot numbers and all version/build numbers from skins, map themes, HUD themes, SFX packs, gunfire sounds, and music tracks—numbering everything cleanly `#1..#N`.

---

#### Version 0.0.1.058 (Build #79)

🎛️ **Pause Menu Layout, Volume Badge, Mandatory Save Reset & 3D Preview Stage**
- **Mandatory Score Submission on Active Run Reset:** Confirming a mid-run reset now prompts for confirmation and executes **`"SAVE SCORE & GENERATE NEW MAP"`**, while pre-Wave-1 zero-score resets continue regenerating immediately without saving empty entries.
- **Live 3D Tower Previews in Customizations Hub:** Switching `RENDER MODE` to **3D Isometric** inside Audio & Visual Customization renders live 3D pillar/obelisk rigs directly inside the main showcase and multi-class skin preview tiles, accompanied by a **`2D`** / **`3D`** mode indicator badge.
- **Front-and-Center Pause Menu Volume Badge & Cleaner Hierarchy:** Moved the Volume Mixer into a prominent clickable **`VOLUME`** badge, placed **`"END RUN & GENERATE NEW MAP"`** directly beneath **`"RESUME BATTLE"`**, and moved **`"AUDIO & VISUAL CUSTOMIZATION"`** immediately above the footer update check button.

---

#### Version 0.0.1.057 (Build #78)

🎛️ **Pause Menu Polish & Cleaner Controls**
- **Single-Line Update & Build Button:** Combined the bottom Pause Menu footer into one clean button.
- **Simplified Difficulty & Action Labels:** Renamed the difficulty section header to **`DIFFICULTY`**, removed the extra descriptive text beneath it, and unified the map-reset button label as **`END RUN & GENERATE NEW MAP`**.
- **2D / 3D Toggle in Customization Hub:** Moved the 2D / 3D render mode switch into **Audio & Visual Customization** with a compact inline **`RENDER MODE`** label.
