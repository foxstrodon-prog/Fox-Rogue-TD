# DaVinci Resolve Studio — Programmatic Stop-Motion Video Pack

This package generates a pixel-perfect, 4K 60 FPS vertical YouTube Short for **Dual Front TD** directly inside **DaVinci Resolve Studio** using real game assets—without recording gameplay.

All files are self-contained inside `Youtube Team Assets/`.

---

## 🚀 3-Click Workflow (FCPXML Timeline Import)

1. **Open DaVinci Resolve Studio.**
2. Go to: **File $\rightarrow$ Import $\rightarrow$ Timeline...** (or press `Ctrl+Shift+I` on Windows).
3. Select the timeline file:
   ```
   Youtube Team Assets/Timelines/DualFrontTD_ViralShort_22s.fcpxml
   ```
4. **Done!** DaVinci will instantly build a vertical **1080x1920 (9:16) @ 60.0 FPS** timeline titled:
   `Dual Front TD - 22s Viral Loop [4K 60FPS]`

---

## 📂 Folder Structure

```
Youtube Team Assets/
├── README_DAVINCI_STUDIO.md         <- This quick-start guide
├── build_davinci_pack.ps1           <- Regeneration automation script
├── Timelines/
│   ├── DualFrontTD_ViralShort_22s.fcpxml   <- Apple FCPXML 1.8 timeline (importable in Resolve)
│   └── DualFrontTD_TacticalDopamine.comp   <- DaVinci Fusion procedural node composition
├── Assets/
│   ├── bg_grid.png                  <- 1080x1920 Cyber grid arena with dual circuit lanes
│   ├── booster_pad_cyan.png         <- Cyan glowing teleport pad
│   ├── booster_pad_magenta.png      <- Magenta glowing teleport pad
│   ├── turret_gatling.png           <- Kinetic Ballistic Gatling turret
│   ├── turret_stellar.png           <- Level 4 Cosmic Stellar Star turret
│   ├── creep_swarm.png              <- Glowing biomechanical swarm creep
│   ├── shockwave_ring.png           <- Golden radiant Apex Corona shockwave
│   └── hud_squircle_button.png      <- Floating tactical squircle action bar
├── Thumbnails/                      <- Thumbnail frame anchors and exports
└── Concepts/                        <- Viral short packaging trifectas
```

---

## 🎬 Timeline Track Layout & Keyframe Progression

| Video Track | Element | Timeline Span | Keyframe Action |
| :--- | :--- | :--- | :--- |
| **Track 1** | `bg_grid.png` | 00:00 – 00:22 | Cyber tactical grid & converging dual lanes |
| **Track 2** | `booster_pad_cyan.png` & `magenta` | 00:00 – 00:22 | Teleportation anchor points on left/right lanes |
| **Track 3** | `creep_swarm.png` | 00:00 – 00:14 | Swarm pushing down left lane toward the core |
| **Track 4** | `turret_gatling.png` | 00:00 – 00:11 | Kinetic turret holding right lane, teleport-swapping |
| **Track 4** | `turret_stellar.png` | 00:11 – 00:22 | Ascends to Level 4 Cosmic Stellar Star |
| **Track 5** | `shockwave_ring.png` | 00:14 – 00:17 | Radiant gold Apex Corona ring wipes the screen |
| **Track 6** | `hud_squircle_button.png` | 00:06 – 00:12 | Floating tactical squircle upgrade action bar |

---

## 🎵 Audio Sync & Deliver

1. **Add Music:** Drop your trending audio track into **Audio Track A1**.
2. **Align the Beat Drop:** Align the primary bass kick to hit at **Frame 840 (00:14.00)**, the exact split-second the golden shockwave detonates and vaporizes the swarm.
3. **Export:**
   - Switch to the **Deliver** tab at the bottom of DaVinci Resolve.
   - Select the **YouTube** preset (Resolution: `1080 x 1920 Vertical`, Frame Rate: `60 fps`).
   - Click **Add to Render Queue** $\rightarrow$ **Render All**.

---

## 🔄 Refreshing / Regenerating Assets

Whenever new tower models, particle shapes, or balance updates occur, run:
```powershell
powershell -ExecutionPolicy Bypass -File "Youtube Team Assets/build_davinci_pack.ps1"
```
This regenerates all 4K sprites in `Youtube Team Assets/Assets/` and updates the timeline automatically.
