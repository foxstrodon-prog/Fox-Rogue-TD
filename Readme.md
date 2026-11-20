# Dual Front TD – Patch Notes

Welcome to **Dual Front TD**! This is a 100% free indie passion project built for tower defense fans who appreciate deep buildcraft, challenging waves, and zero mobile paywalls. No ads, no microtransactions, and all previous APK builds remain preserved and playable on our GitHub releases page.

- **Direct APK Download:** [`apk/Dual-Front-TD-v0.0.1.052-debug.apk`](apk/Dual-Front-TD-v0.0.1.052-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe)

---

### 🚀 What's New in Version 0.0.1.052 (Build #73)

⬡ **Hexagon Enemies, Quadrant Pad Spacing, Split Pair Auras, Enemy Info Card & Run Telemetry**
- **Hexagon Bruisers & Clean HUD Polish:** Armored tank enemies now render as sharp **Hexagons** matching Heavy Siege towers, teleport link lines are sleeker, rotation tips auto-dismiss after 5 seconds, and rotated surplus gold banners align cleanly.
- **Quadrant-Balanced 10-Pad Maps:** Procedural layouts now distribute `2–3` buildable pads per quadrant (`5/5` Left/Right and Top/Bottom halves) while preserving zero side-by-side clumping and `7/12` dual-lane length balance.
- **Split Pair/Global Tier-3 Auras & 3 New Enemies:** Tier-3 auras now grant `100%` power to your linked twin pad, `35%` globally, and `40%` from the Singularity Vault, paired with a smoother Medium late-game HP curve and 3 new enemies (**Warp Strider**, **Aegis Dreadnought**, **Phase Leviathan**).
- **Tap-to-Inspect Enemy Card & 5-Wave Telemetry:** Tap any enemy on the field to inspect its live HP, Armor, Speed, Bounty, Strengths, and Weaknesses, and view/copy 5-wave run checkpoints directly from the Telemetry dialog.

---

### 📦 Recent Patch History

#### Version 0.0.1.051 (Build #72)

⚡ **Snappy Damage Numbers, Clean Teleport Indicators, Early Progression & Active-Run Save State**
- **Snappy Floating Damage Numbers:** Tightened hit popup lifetimes with damage-priority throttling and FIFO eviction under a strict active cap so late-game bursts never stall or hide fresh hits.
- **Readable Teleport Pads & 1-Marker Badges:** Selected pads now show a crisp white outer corner reticle instead of recoloring the pad, and each linked pair uses a single matching corner marker (4 corners for Pairs 1–4 plus a Crimson star for Pair 5) matched in the Teleport Pads HUD capsule.
- **Early Pacing & `0 / 0 / 1 / 3` Class Unlocks:** **Elemental Void** is now unlocked from Wave 1 alongside **Kinetic Ballistic**, **Heavy Siege** unlocks at `1` Tier-3 tower, and **Stellar Cosmic** unlocks at `3` Tier-3 towers, with tighter Waves 1–3 cadence and softened Wave 5 mini-bosses.
- **Landscape HUD Polish & Active-Run Save State:** Stacked rotated `AUTO` + `BEGIN` into one matching-width capsule, anchored rotated HUD telemetry popups and Round 1 banners cleanly, and added automatic active-run persistence across app close and process death.

---

#### Version 0.0.1.050 (Build #71)

⚡ **5 Color-Coded Cross-Lane Teleport Pairs on All 10 Pads & Singularity Pair Preservation**
- **All 10 Pads Linked Into 5 Cross-Lane Pairs:** Every buildable pad on the 10-slot board now belongs to one of 5 high-contrast, color-coded cross-lane teleport pairs (Emerald, Cyan, Amber, Magenta, Crimson) with clear pair markers for instant readability across both fronts.
- **Pure Tactical Warping (Zero Pad Stat/Gold Buffs):** Removed all pad economy, damage, range, fire-rate, and Harmonic Grid bonuses so pad placement is purely positional—supporting both instant moves to an empty twin and clean swaps between occupied twins.
- **Singularity Collapse Pair Preservation:** Collapsing a maxed 10-tower board now preserves your Capstone OG tower's linked teleport pair (both the OG pad and its twin) onto the newly generated map while regenerating all 5 pairs cleanly.

---

#### Version 0.0.1.049 (Build #70)

📦 **Lean ~2.9 MB APK Footprint, Faster Update Checks & Resilient Cloud Sync**
- **Massive APK Size Drop (~31 MB → ~2.9 MB):** Pruned unused SDK dependencies, stripped unused locales, and enabled R8 code and resource shrinking while preserving seamless in-place upgrades over prior installs.
- **Smarter GitHub Update Checks:** Added `ETag` (`304 Not Modified`) response caching, shared HTTP timeouts, safe JSON fallback parsing, and clear rate-limit feedback so checking for updates uses minimal data and stays snappy.
- **Hardened Cloud Telemetry & Quota Resilience:** Aligned cloud balance telemetry validation, tightened Firestore security bounds, classified daily quota errors cleanly without wasted retries, and added automated network resilience tests.
