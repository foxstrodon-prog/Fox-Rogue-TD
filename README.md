# Dual Front TD

A fast-paced, dual-lane tactical tower defense game built natively for Android with Kotlin and Jetpack Compose. 

Created by an indie developer who just loves tower defense games and wanted to build something deep, responsive, and completely honest. 

- **100% Free:** No ads, no microtransactions, no paywalls, no energy bars.
- **Hardware Optimized:** Locked 60/120 FPS simulation loop using custom spatial hashing.
- **Deep Buildcraft:** 4 core towers branching into 52 total evolutions with procedural tier 1–4 visual cues.
- **Full Historical Preservation:** Every past APK remains archived and downloadable on GitHub Releases.

---

## 📱 Downloads (Android APK & Local ADB Companion)

- **Latest Android APK (`v0.0.1.044`):** [`apk/Dual-Front-TD-v0.0.1.044-debug.apk`](apk/Dual-Front-TD-v0.0.1.044-debug.apk)
- **Local ADB Companion Runner:** [`apk/Dual-Front-TD-ADB.exe`](apk/Dual-Front-TD-ADB.exe) (located in `apk/` directory)
  - General utility that automatically detects ADB, pairs with your Android device or local emulator, installs the latest staged APK, sets widescreen landscape mode, and launches the game.
- **GitHub Releases:** Only official Android APK releases are distributed on GitHub.
- **Google Play "What's New" Notes:** [`GPReadme.md`](GPReadme.md)
- **Release Archives:** Download any previous release directly from the GitHub **Releases** tab.

---

## 🕹️ What's New in Version 0.0.1.044

### 🎯 Tactical HUD Ergonomics, 10x21 Full-Screen Grid, Portal/Core Visuals & Data-Driven Balance Pass (`versionCode = 65`)
- **Split-Action Wave Capsule & Flanked Bottom Squircle Bar (`HudTopBar.kt`, `HudStatusWidgets.kt`, `TowerUpgradeBar.kt`, `TargetPriorityMenu.kt`, `RogueTDScreen.kt`):**
  - Combined `BEGIN` and `AUTO` into a unified, orientation-aware `HudWaveControlCapsule`.
  - Flanked `Target Priority` to the left edge and `Sell Tower` to the right edge of the bottom dock so upgrade squircles remain strictly centered across Tiers `0..3` without accidental sell mis-taps, and added **Active / Banked Aura Pip Badges** (`★ NEW`, `● 1x`, `⚡ 2B`, `CAP 1x`) on Tier-2 and Tier-3 upgrade previews.
- **Logo-Inspired Entry Portals, Shielded Heart Core & 10x21 Map Grid (`TerrainRenderer.kt`, `TrackRenderer.kt`, `MapGenerator.kt`, `TelemetryManager.kt`, `WaveGenerator.kt`, `SimulationCoordinator.kt`):**
  - Expanded default map height to `10x21` (with automatic `SharedPreferences` schema migration so existing `10x20` installs aren't flagged as `DEV TUNED`), upgraded lane spawners to directional Prism Portals and the Defense Core to a Shielded Heart Emblem with 24 comet-tail flow particles, and tuned wave-based gold decay and difficulty HP curves against the `1 / 2–3 / 4–5` Singularity Collapse benchmarks.
- **Headless Balance Simulator & Whole-Run "Balance by Data" Telemetry (`HeadlessBalanceSimulator.kt`, `DevProbesAndCinemaTabs.kt`, `RunSummaryDialogs.kt`, `FirestoreLeaderboardApi.kt`):**
  - Added end-of-run `Total Collapses`, `Unspent Gold at End`, and `Unique Auras Banked` readouts, a deterministic 63-wave `HeadlessBalanceSimulator` in the Dev Console, whole-run sacrificed/sold tower telemetry tracking, non-blocking `run_telemetry` cloud upload, and 4 new Build #65 procedural audio/skin variants (`260/260` unit tests passing).

---

## 🕹️ Previous Releases

### Version 0.0.1.043

### ⚡ Speed-Invariant 10X Simulation & CI Plugin Cache Hardening (`versionCode = 64`)
- **Fixed-Timestep Accumulator & Batched UI Publication (`RogueTDViewModel.kt`, `SimulationCoordinator.kt`):**
  - Replaced coarse variable sub-stepping (`MAX_PHYSICS_SUBSTEPS = 8`) with a deterministic fixed `20ms` (`0.020f` s) physics accumulator (`MAX_PHYSICS_SUBSTEPS = 48`), frame-budget sleep pacing (`16L - workMs`), and per-frame batched `publishUiState` updates so `10X` scales smoothly without bloated physics steps or 8×–48× redundant Compose snapshots on loaded tower boards.
- **Remainder-Preserving Wave Spawner (`WaveProgressionCoordinator.kt`):**
  - Replaced single-spawn-per-substep `if` logic with a remainder-preserving `while` loop (`nextSpawnTimeMs += nextDelay`), eliminating high-speed spawn-timer drift so `1X` and `10X` maintain identical enemy density and combat parity (`SpeedScalingInvarianceTest.kt`).
- **CI / Gradle Release Pipeline Hardening (`.github/workflows/release.yml`, `settings.gradle.kts`, `gradle/libs.versions.toml`):**
  - Scoped `setup-gradle@v5` cache writes to `main`, disabled CI configuration-cache storage on transient failures (`--no-configuration-cache`), added a `--refresh-dependencies` retry fallback, prioritized `gradlePluginPortal()` ahead of `mavenCentral()`, and bumped AGP to stable `9.5.1` (`255/255` unit tests passing, plus 4 new Build #64 procedural audio/skin variants).

---

## 🕹️ Previous Releases

### Version 0.0.1.042

### 🔄 Automatic Build-Driven Sound & Skin Generation (`versionCode = 63`)
- **BuildConfig-Driven Asset Generation (`BuildArchiveVisualCatalog.kt`, `BuildArchiveCatalog.kt`, `ArchivedAudioSynthesizer.kt`):**
  - Replaced hardcoded Build #61 identity with runtime `BuildConfig.VERSION_CODE` / `BuildConfig.VERSION_NAME`, automatically generating 4 brand-new deterministic variants (`Slot 2..5`) across **Turret Skins**, **Map & Lane Themes**, **BGM Tracks**, **SFX Packs**, **Turret Fire** (16 per-class sounds), and **Turret Upgrades** (16 per-class chimes) on every `versionCode` increment while keeping the Default Core (`Slot 1`) untouched.
  - Preserved Build #61's curated skins, themes, and synth recipes intact under `b61_*` and generalized procedural HSV/style and parametric WAV synthesis for Build #62, #63 (`v0.0.1.042`), and every future build.
- **Multi-Build Vault Archives & Dynamic Playback (`SoundArchiveStore.kt`, `SoundManager.kt`, `MusicAndPreviewPlayer.kt`, `AssetVaultDialog.kt`, `AssetVaultCoordinator.kt`):**
  - Automatically archives all prior builds (`60 until CURRENT_BUILD_CODE`, plus `Pre-Build #60`) into the Vault's **Archives** tab with per-build badges, keeping every archived sound, skin, and map theme previewable and equippable (`SoundPool` dynamic preload + upgrade-pair resolution).
  - Updated `AudioVisualCustomizationPanel.kt` headers, rolling BGM playlist resolution, and selection highlights dynamically to the active build, with `252/252` unit tests passing (including consecutive simulated build `62/63/64` tests).

---

### Version 0.0.1.041

### ⚛️ Singularity Remap, Synergy HUD & Skin Previews, Speed/Economy Balance & Tower Polish (`versionCode = 62`)
- **Bullet HUD Synergies (`HudSynergyCapsule.kt`, `HudTelemetryPopup.kt`, `TowerUpgradeManager.kt`, `HudTopBar.kt`):**
  - Separated live **Active Board Synergies** from the **Singularity Tower** vault in a two-section telemetry popup, included duplicate `sourceCount` stacks in the HUD pill and headers, and added orientation-aware `BoxWithConstraints` scrolling across all 4 rotations (`0°/90°/180°/270°`) with `CLOSE` pinned.
- **Singularity Collapse New Map & Mid-Round Safety (`TowerPlacementCoordinator.kt`, `MapGenerator.kt`, `WaveProgressionCoordinator.kt`, `SingularityWarningDialog.kt`):**
  - Collapsing into a Singularity now generates a fresh dual-lane map while preserving the anchor tower's tile—or both linked pads when anchored on an Amplifier, High Ground, or Conduit booster pair—and cleanly resets/stagger-redispatches living mid-round enemies from the new spawners.
- **Interactive Skin & Theme Previews (`AudioVisualCustomizationPanel.kt`, `AssetVaultDialog.kt`):**
  - Added live multi-class Turret Skin previews (Kinetic Circle, Elemental Triangle, Heavy Hexagon, Stellar Star) and dual-lane Map & Lane Theme previews with Preview-vs-Equip support in both Customization and the Sound & Skin Vault, and removed unused `towerVisualSkin`.
- **1X Simulation Pace & Difficulty/Economy Rebalance (`RogueTDViewModel.kt`, `GameModels.kt`, `WaveGenerator.kt`, `SimulationCoordinator.kt`):**
  - Boosted baseline `1X` simulation pace (`BASE_SIM_SPEED_MULTIPLIER = 1.85f`) with fixed physics sub-stepping to prevent high-speed tunneling, capped additive kill-bounty inflation, added late-wave density decay, tightened wave completion scrap, and steepened Easy/Medium/Hard HP & speed scaling.
- **Tower & Pad Visual Upgrades (`EntityRenderer.kt`, `TowerUpgradeBar.kt`, `TowerBalanceConfig.kt`, `TerrainRenderer.kt`):**
  - Added Tier-4 aura pulsing and upgraded layered geometry to `KINETIC_BALLISTIC` across all 5 draw styles, switched `HEAVY_SIEGE` visuals and silhouettes to **Hexagon (`⬢`)** while preserving `EnemyShape.SQUARE` combat interactions, and added a glowing perimeter outline and subtle fade pulse to unoccupied blank buildable pads (`250/250` unit tests passing).

---

### Version 0.0.1.040

### 🗂️ Versioned Sound & Skin Archive Library (`versionCode = 61`)
- **Build-Over-Build Versioned Asset Registry (`AssetCatalogModels.kt`, `BuildArchiveCatalog.kt`, `BuildArchiveVisualCatalog.kt`, `ArchivedAudioSynthesizer.kt`):**
  - Preserved the canonical Default (`Slot 1`) assets untouched across all categories while introducing 4 new Build #61 variants for **Turret Fire** (16 new across 4 classes), **Upgrades** (16 new), **BGM Tracks** (4 new), **SFX Packs** (4 new), **Turret Skins** (Crimson Vanguard, Arctic Prism, Astral Eclipse, Phosphor 1984), and **Map Themes** (Crimson Caldera, Glacial Schematic, Astral Horizon, Emerald CRT).
  - Assigned immutable build-scoped IDs (`pre60_*`, `b60_*`, `b61_*`, `default_*`) with seamless vote and config migration from `v0.0.1.039` so Likes, Dislikes, and Stars never collide across builds.
  - Refactored `EntityRenderer.kt` and `GameCanvas.kt` to consume data-driven `TurretSkinDef` and `MapThemeDef` styles with zero per-frame allocations, and wired lazy on-demand `Dispatchers.IO` synthesis for archived audio items.
- **Revamped "Archives" Vault & Legacy Pre-Build #60 Preservation (`AssetVaultDialog.kt`, `SoundArchiveStore.kt`, `AudioVisualCustomizationPanel.kt`):**
  - Renamed `"Archived WAVs"` to **"Archives"** with 6 section filter chips (`All Archives`, `Turret Fire`, `Upgrades`, `BGM & SFX`, `Turret Skins`, `Map Themes`), build-label badges (`Pre-Build #60`, `Build #60 (v0.0.1.039)`, `Build #61 (v0.0.1.040)`), and full **PLAY**, **USE**, and **👍 / 👎 / ★** controls on every archived sound, skin, and map theme.
  - Snapshotted all Build #60 (`v0.0.1.039`) assets into the Build #60 Archive and tagged legacy `SkinTheme` presets and unrecognized on-disk `.wav` files under `Pre-Build #60`.
- **Spark-Safe Firebase Scaling & Persistence (`AssetVoteSyncManager.kt`, `firestore.rules`, `TelemetryManager.kt`, `VersionedArchiveVaultTest.kt`):**
  - Scaled `asset_votes/{userId}` document capacity from `200` to `1500` entries with `buildCode` metadata while maintaining single-document-per-user Spark efficiency, and persisted equipped active/archived asset IDs cleanly across restarts (`237/237` unit tests passing).

---

## 🕹️ Previous Releases

### Version 0.0.1.039

### 🔊 Off-Main-Thread Audio Pipeline, Atomic WAV Writes & Vote Sync Ordering (`versionCode = 60`)
- **Zero-Blocking Audio Initialization & Asset Persistence (`SoundManager.kt`, `MusicAndPreviewPlayer.kt`, `AssetVaultCoordinator.kt`):**
  - Moved `SoundManager` startup/profile SFX `.wav` synthesis, `populateAssetLibrary()`, `MusicAndPreviewPlayer.resumeMusic()` / rolling-playlist `updateTick()` track preparation, and `equipAsset()` library JSON exports off the Main thread onto `Dispatchers.IO`, with non-blocking `release()` flushing.
  - Serialized BGM track switches with synchronous `trackGeneration` reservation + job cancellation and added per-track atomic temp-file + rename `.wav` generation (`getOrCreateMusicFile`) with `0f..1f` preview volume clamping so `0%` volume mutes previews properly.
- **In-Flight Vote Preservation & Redundant-Sync Prevention (`AssetVoteSyncManager.kt`, `AssetVaultCoordinator.kt`, `RogueTDViewModel.kt`, `app/build.gradle.kts`):**
  - Added revision tracking in `AssetVoteSyncManager` so votes cast while a Firestore PATCH is in flight preserve `hasPendingSync`, released `isSyncInFlight` before `onResult` Main-thread callbacks, and drained/rescheduled follow-up syncs in `AssetVaultCoordinator`.
  - Short-circuited empty no-op vote syncs before checking the 10-second Spark cooldown, updated `closeAssetVault()` to call `syncPendingVotesIfNeeded()`, and aligned `stageDebugApk` output naming dynamically with `android.defaultConfig.versionName` (`231/231` unit tests passing).

### Version 0.0.1.038

### 🔊 Audio Threading, MediaPlayer Hardening, Vault Safety & Vote Sync Polish (`versionCode = 59`)
- **Off-Main-Thread Audio & Atomic Persistence (`SoundManager.kt`, `SoundArchiveStore.kt`, `MusicAndPreviewPlayer.kt`):**
  - Offloaded startup/setter BGM synthesis, `ARCHIVED_WAV` preview preparation, and serialized rating JSON exports onto `Dispatchers.IO` with deterministic `release()` flushing and atomic temp-file + rename writes (`exported_skins_sounds_library.json.tmp`).
  - Synchronized `MediaPlayer` lifecycle, volume fades, and rolling playlist state under `playerLock`, added `OnErrorListener` + exception cleanup for preview/BGM `MediaPlayer` instances, enforced `isSuspended()`, and invalidated stale async previews via generation checks.
  - Stopped active BGM/archive previews before SFX previews, encapsulated turret slot arrays, and switched `SoundManager` & `AssetVaultCoordinator` reads to atomic `assetLibraryFlow.value` snapshots with immutable `CustomAssetRecord` fields.
- **Dialog Guarding, Vote Sync Concurrency & Stellar Slot Consolidation (`RogueTDViewModel.kt`, `RogueTDScreen.kt`, `AssetVoteSyncManager.kt`, `ThemeModels.kt`):**
  - Prevented `PauseMenuDialog` and `BackHandler` from stacking over `AssetVaultDialog` and reset `isAssetVaultOpen` on `startNewRun()`.
  - Added an `AtomicBoolean` in-flight sync guard in `AssetVoteSyncManager`, scoped the 10-second Spark cooldown strictly to completed Firestore HTTP attempts, and preserved pending cloud sync status during cooldown.
  - Consolidated Stellar Cosmic gunfire labels into `TurretSfxSlot.stellarName` across `SoundArchiveStore` and `AudioVisualCustomizationPanel` (`225/225` unit tests passing).

---

### Version 0.0.1.037

### 🔥 Firebase REST Vote Sync & Auth Fallback Hardening (`versionCode = 58`)
- **Unified Firestore REST Vote Sync (`AssetVoteSyncManager.kt`):**
  - Migrated `asset_votes/{userId}` cloud telemetry writes from the native `FirebaseFirestore` SDK to Firestore REST full-document overwrite `PATCH` (no `updateMask`) authenticated via `AuthRepository.ensureAuthenticated()` + `AuthRepository.getAuthenticatedSession()`.
  - Ensures Sound & Skin Vault ratings sync seamlessly on both native `FirebaseAuth` and Identity Toolkit REST fallback sessions without `request.auth == null` `PERMISSION_DENIED` mismatches, and includes `X-Android-Package` / `X-Android-Cert` / `X-Goog-Api-Key` headers matching `FirestoreLeaderboardApi`.
- **Automatic Token Refresh & Actionable Sync Diagnostics (`AssetVoteSyncManager.kt`, `SoundVaultAndVoteSyncTest.kt`):**
  - Retries once with a forced token refresh on HTTP `401`/`403` and surfaces clear diagnostic reasons (such as undeployed `firestore.rules`, disabled Anonymous Auth, or Android SHA-1 key blocks) directly in the Vault sync status banner (`221/221` unit tests passing).

---

### Version 0.0.1.036

### 🔊 Sound & Skin Archive Vault, Permanent WAV Storage & Spark-Safe Vote Sync (`versionCode = 57`)
- **Permanent Sound Archive & Legacy WAV Discovery (`SoundArchiveStore.kt`, `SoundManager.kt`, `MusicAndPreviewPlayer.kt`):**
  - Migrated procedural `.wav` storage from volatile `cacheDir` to persistent `filesDir/sound_archive`, with non-blocking `Dispatchers.IO` migration of stranded legacy `.wav` files and immediate `StateFlow` refresh.
  - Registered all 5 BGM tracks, generic `SoundFx`, **20 Turret Gunfire variants** (`4 TowerClass × 5 slots`, including `STELLAR_COSMIC`), **20 Tower Upgrade variants**, **5 Turret Skins**, **5 Map Themes**, and any archived `.wav` files on disk (`ARCHIVED_WAV`).
  - Added `previewAsset(assetId)` / `stopPreview()` with single-slot transient playback that pauses active BGM during music/archive previews and resumes cleanly on completion.
- **In-Game Sound & Skin Vault UI & Stellar Cosmic Menu Wiring (`AssetVaultDialog.kt`, `AudioVisualCustomizationPanel.kt`, `PauseMenuDialog.kt`, `RogueTDScreen.kt`):**
  - Added the **Sound & Skin Vault** dialog accessible from both the Pause Menu and Audio & Visual Customization panel, featuring category filters, skin/theme preview badges, 2-row squircle cards for narrow `360.dp` portrait and landscape screens, one-tap playback, equip buttons, and tri-state Like (`+1`) / Dislike (`-1`) / Star (`★`) toggles.
  - Added `stellarSfxSlot` across `DevBalanceConfig`, `GameUIState`, `TelemetryManager`, `SimulationCoordinator`, and `AudioVisualCustomizationPanel` so Stellar Cosmic gunfire and upgrade sounds can be A/B tested and equipped in-game.
- **Spark-Safe Firebase Vote Sync & Security Rules (`AssetVoteSyncManager.kt`, `AssetVaultCoordinator.kt`, `firestore.rules`):**
  - Persists votes locally to `filesDir/exported_skins_sounds_library.json` (`importLibraryJson` + `exportLibraryJson`) and batches ratings into a single `asset_votes/{userId}` document write on Vault close or manual Sync using full-document `.set(payload)` overwrite (eliminating stale-vote keys when resetting back to Neutral) with a 10-second Spark cooldown and strict `request.auth` schema rules (`219/219` unit tests passing).

---

### Version 0.0.1.035

### 🔄 Rotation Layout Polish, Safe-Drawing Popups & Narrow-Viewport HUD Scroll (`versionCode = 56`, includes v0.0.1.034 HUD Fixes)
- **Dynamic Banner Shift & Swapped Rotation Bounds (`RogueTDScreen.kt`, `Round1Banners.kt`, `HudStatusWidgets.kt`):**
  - Applied `dynamicBannerShiftPx` vertical offset to portrait `UpdateBanner` so dragging `MinimalTopHud` down never covers it.
  - Replaced raw `graphicsLayer` rotation on `Round1TileHintBanner`, `Round1LaunchBanner`, and `UpdateBanner` with `hudCapsuleContainer` (driven by `isQuarterTurnRotation`), swapping layout dimensions at `90°`/`270°` to prevent top-edge clipping and HUD overlap.
  - Updated `Round1TileHintBanner` alignment to `BiasAlignment(0.5f, 0f)` at `90°`, `BiasAlignment(-0.5f, 0f)` at `270°`, and `BiasAlignment(0f, -0.5f)` at `180°` so the Round-1 hint stays at ~3/4 visual depth in every orientation.
- **Rotated Target Priority & Safe-Area Telemetry Popups (`TargetPriorityMenu.kt`, `TowerUpgradeBar.kt`, `HudTelemetryPopup.kt`):**
  - Fixed `FloatingSquircleUpgradeBar`'s `isRotated` check to quarter-turn angles only (`-90f` / `90f`) so `180°` stays vertically stacked, and arranged `TargetPriorityMenu` pills side-by-side via `hudCapsuleContainer` when rotated so priority (`STRONGEST`) and sell buttons never overlap and fit inside a `360.dp` dock.
  - Added `WindowInsets.safeDrawing` padding (with `decorFitsSystemWindows = false`) inside `HudTelemetryPopup`'s scrim container and increased `0°` top clearance (`48.dp`) so popups clear notches, cutouts, system bars, and `MinimalTopHud`.
- **Graceful Narrow-Screen Top HUD Scrolling (`HudTopBar.kt`):**
  - Wrapped `MinimalTopHud`'s row in `BoxWithConstraints` + `horizontalScroll(rememberScrollState()).widthIn(min = maxWidth)`, preserving `Arrangement.SpaceBetween` on wide displays while preventing right-edge clipping of `HudLaunchWaveButton` and `HudBatteryWidget` on `360.dp` portrait phones (`214/214` unit tests passing).

---

### Version 0.0.1.033

### 🛠️ HUD Hotfixes, Unified Telemetry Popups, QOL Auto-Close & CI Recovery (`versionCode = 54`)
- **Clamped Top HUD & Safe Cutout Insets (`RogueTDScreen.kt`):**
  - Strictly clamped upward drag offset to `>= 0f` and applied combined `displayCutout` + `statusBars` top safe padding before drag translation so `hudOffsetY = 0f` always sits cleanly below camera punch-holes/notches, with consistent portrait `UpdateBanner` spacing.
- **Lowered Round-1 Tile Callout & 180° Banner Rotation (`RogueTDScreen.kt`, `Round1Banners.kt`):**
  - Reworded the Round-1 hint to `"SELECT ANY TILE TO BUILD (Tap boosted pad)"`, relocated it to ~3/4 screen height (`BiasAlignment(0f, 0.5f)`) clear of center lanes and the bottom dock, removed unnecessary `hasRound1TopBanner` portrait padding, and applied smooth `edgeCounterRotation` (including `180°`) to Round-1 callouts and `UpdateBanner`.
- **Battery Pause Shortcut & Unified Telemetry Popups (`HudStatusWidgets.kt`, `HudTopBar.kt`, `HudTelemetryPopup.kt`, `HudTeleportPadsCapsule.kt`, `HudSynergyCapsule.kt`):**
  - Wired `HudBatteryWidget` taps directly to `onOpenPauseMenu` across both portrait and landscape HUD layouts alongside the dedicated Pause button.
  - Unified Teleport Pads and Active Synergies popups onto dark-glass full-screen `Dialog` overlays with smooth `AnimatedVisibility`, edge-anchored alignment across all four orientations (`0°` Top, `90°` Left, `180°` Bottom, `270°` Right), and counter-rotated contents.
- **Pulsing HUD Edge Tabs & Untruncated Squircle Labels (`EdgeBulletTipTab.kt`, `TargetPriorityMenu.kt`, `TowerUpgradeBar.kt`):**
  - Restyled `EdgeBulletTipTab` to match the dark-glass HUD (`0xFF161F30` @ `0.86f`, `1.2.dp` border, `48.dp` target) with an optional `isPulsing` alpha/scale beacon for active teleport and manual-rotate tabs.
  - Removed the `.take(4)` truncation on `TargetPriorityMenu` (so `"FIRST"` and `"STRONGEST"` render cleanly inside widened `56.dp–72.dp` stacked pills) and expanded `"Singular."` to full `"Singularity"` in `getArchetypeCardLabels`.
- **Smart Squircle Auto-Close & Persistent Tier-3 Synergy Lines (`GameUIState.kt`, `RogueTDViewModel.kt`, `TowerPlacementCoordinator.kt`, `TerrainRenderer.kt`):**
  - Added `lastSelectedTowerId` (preserved across close/deselect, cleared on sell or `startNewRun`) so Tier-3 global synergy lines remain visible after squircles close while range circles only render for actively selected towers.
  - Auto-closes build/upgrade squircles after Tier 0 `buildTower` and Tier 1 `upgradeTowerToVector` when remaining scrap cannot afford the cheapest next upgrade, while keeping Tier 2+ upgrades and manual `selectTile` open.
- **GitHub Actions Node 24 & Clean-Checkout CI Recovery (`.github/workflows/release.yml`, `ExampleRobolectricTest.kt`, `BulletHudModernization033Test.kt`, `RedTeamRemediation026Test.kt`, `LeaderboardCoreTest.kt`):**
  - Upgraded GitHub Actions (`checkout@v5`, `setup-gradle@v5`, `upload-artifact@v6` alongside `setup-java@v5`) for Node 24 compatibility and refactored Firestore credential tests to use an isolated `FirebaseApp` so unit tests pass on clean CI checkouts without `app/google-services.json`.
  - Added 34 new hotfix regression tests in `BulletHudModernization033Test.kt` and hardened deterministic test isolation (`206/206` unit tests passing).

---

## 🕹️ Previous Releases

### Version 0.0.1.032

### 🎯 Minimalist "Bullet HUD" & Control Modernization
- **Standalone Canvas Battery Cell (`HudStatusWidgets.kt`):**
  - Replaced emoji prefixes with a custom Canvas-drawn battery cell outline + terminal nub, centered tabular charge percentage (`tnum`), and a conditional green charging sweep/pulse animation when plugged in.
- **Unified Top HUD Capsules & `BEGIN` Glow Fix (`HudTopBar.kt`, `HudStatusWidgets.kt`, `HudSynergyCapsule.kt`, `HudTeleportPadsCapsule.kt`):**
  - Standardized all 9 top HUD capsules to uniform `28.dp` portrait height (`36.dp` rotated) with consistent `8.dp` squircle corners, wrapped the Wave badge in a matching capsule, and added layout-bound swapping (`hudCapsuleContainer`) so `90°`/`270°` counter-rotated capsules never overlap or clip.
  - Streamlined `HudAutoWaveToggle` to a compact `"AUTO"` pill with glowing-dot vs hollow-ring state feedback, and replaced the Material3 `Button` in `HudLaunchWaveButton` with a clipped `Surface` to eliminate the oversized `48.dp` glow artifact.
- **Edge "Bullet Tip" Tabs & Non-Blocking Callouts (`EdgeBulletTipTab.kt`, `RogueTDScreen.kt`):**
  - Extracted reusable edge-bleeding `EdgeBulletTipTab` composables with `48.dp` touch targets, counter-rotated icons, and smooth horizontal `AnimatedVisibility` slide/fade transitions for manual rotation hints.
  - Compacted and repositioned the Round-1 Ready callout dynamically right beneath the trimmed top HUD so upper buildable tiles remain unobstructed.
- **Dark-Glass Singularity Modal & Upgrader Polish (`SingularityWarningDialog.kt`, `TowerUpgradeBar.kt`, `TargetPriorityMenu.kt`):**
  - Redesigned `SingularityWarningDialog` with translucent dark-glass styling, tabular telemetry rows, squircle buttons, and full `deviceRotationAngle` counter-rotation support.
  - Upgraded `SquircleActionButton`, `SingularityGateSquircle`, and `TargetPriorityMenu` with crisp vector iconography and `tnum` pricing, and removed ~465 lines of dead legacy drawer code from `TargetPriorityMenu.kt`.
- **Verification Suite (`BulletHudModernization032Test.kt`, `ExampleRobolectricTest.kt`):**
  - Added 5 Robolectric Compose UI regression tests covering portrait and `90°`/`270°` rotated HUD rendering, Singularity modal confirmation, `EdgeBulletTipTab`, and `FloatingSquircleUpgradeBar` controls (`172/172` tests passing).

---

## 🕹️ Previous Releases

### Version 0.0.1.031

### 🔧 Kotlin 2.4.21 Upgrade & Strict Leaderboard Credential Isolation
- **Kotlin 2.4.21 & Plugin Catalog Alignment:**
  - Upgraded Kotlin to `2.4.21` (`gradle/libs.versions.toml`), unified `com.google.gms.google-services` (`4.6.0`) via version catalog aliases across root and app Gradle scripts, and removed the legacy `-Xskip-metadata-version-check` compiler flag.
- **Strict Placeholder & Fallback Isolation (`FirestoreLeaderboardApi.kt`, `app/build.gradle.kts`):**
  - Removed hardcoded project ID fallbacks in `BuildConfig` and `FirestoreLeaderboardApi.resolveProjectId()`, relying cleanly on environment/local overrides or `google-services.json` (`FirebaseApp.options`, `FirebaseOptions.fromResource`, and `@string/project_id` / `@string/google_api_key`).
  - Added dedicated `isPlaceholderApiKey()` and `isPlaceholderProjectId()` guards so placeholder tokens (`API_KEY`, `your_firestore_api_key`, `your_firestore_project_id`) never shadow real credentials or falsely mark an unconfigured client as ready.
- **Expanded Verification Suite:**
  - Added unit tests in `ExampleRobolectricTest.kt` verifying placeholder rejection, unconfigured `isConfigured` states, and `google-services.json` context fallback resolution (`167/167` tests passing).

---

## 🕹️ Previous Releases

### Version 0.0.1.030

### 🏆 The Leaderboard Fix & Firebase Infrastructure Upgrade
- **Google Services & Firebase BoM Modernization:**
  - Integrated the Google Services Gradle plugin `4.6.0` (`com.google.gms.google-services`) and upgraded to Firebase Android BoM `35.2.0` (`com.google.firebase:firebase-bom`).
  - Seamlessly reads and passes `google-services.json` configuration values to Firebase SDKs across Authentication and Analytics.
- **Dynamic Leaderboard Configuration Resolution:**
  - Hardened `FirestoreLeaderboardApi` and `AuthRepository` with dynamic project ID and API key discovery, prioritizing valid configurations from `google-services.json` (`FirebaseApp.options` and `@string/google_api_key`) and gracefully falling back to build-time or local parameters.
  - Added robust placeholder isolation for `key=API_KEY` to guarantee offline and team-environment compatibility without shadowing authentic credentials.
  - Configured Kotlin compiler compatibility flags (`-Xskip-metadata-version-check`) for seamless interoperability with modern Android dependencies.

---

## 🕹️ Previous Releases

### Version 0.0.1.029

### 🛡️ Full Codebase Audit, Red-Team Security Hardening & Modularity Restoration
- **Modularity Law Compliance (`<= 750` Lines/File):**
  - Modularized `HudTopBar.kt` (extracting `HudSynergyCapsule.kt`, `HudTeleportPadsCapsule.kt`, and `HudStatusWidgets.kt`) and `DevMenuDrawer.kt` (extracting `DevEconomyTab.kt`, `DevProbesAndCinemaTabs.kt`, and `DevExportBalanceDialog.kt`) so 100% of production Kotlin files under `app/src/main/java/com/example/` stay at or under 750 lines.
  - Restored horizontal scroll, 90°/270° rotated capsule layouts, centered rotated dialogs, and accurate `activeEnemyCount` telemetry on the top HUD.
- **Engine, Simulation & Targeting Fixes (`CombatSimulationEngine.kt`, `SimulationCoordinator.kt`, `WaveGenerator.kt`, `TowerPlacementCoordinator.kt`):**
  - Added strict `.isDead` filtering in `SpatialHashGrid` queries and `findEnemyById`, eliminated self-aliased projectile scratch list mutations on zero-delta steps, and added finite/zero-division guards across `DamageCalculator.kt`, `EntityModels.kt`, and `EntityRenderer.kt`.
  - Gated Cryo Shatterburst strictly on Cryo Apex Form `B.a` in a dedicated pre-bounty pass, fixed Stellar weapon splash attributes, corrected Wave 10/20 Boss modifier precedence & scripted wave speed scaling, guarded post-victory `deployWave()` & ticks, deferred victory run recording until final state commit, and used immutable copy updates for tower target priority and booster swaps.
- **UI, Canvas, Audio & Lifecycle Polish (`GameCanvas.kt`, `TerrainRenderer.kt`, `SoundManager.kt`, `RogueTDViewModel.kt`, `MainActivity.kt`, `RunSummaryDialogs.kt`, `TelemetryLeaderboardDialog.kt`):**
  - Eliminated Float32 epoch-millisecond trig quantization freezes in screen shake and terrain/tower animations, hoisted fixed Canvas `Stroke` instances, fixed `OutlinedTextField` vertical clipping and banner overlap, wired retry click handlers on post-run banners, and serialized post-run callsign updates with async score submission.
  - Recovered background music when unmuting from a muted start, applied volume/mute changes immediately even while paused, unloaded stale `SoundPool` generic SFX on profile switches, ran `soundManager.updateTick` unconditionally at unscaled wall-clock deltas, and cached auto-rotate settings via a `ContentObserver`.
- **Red-Team Network, Auth, Update & Storage Hardening (`AuthRepository.kt`, `ScoreSubmissionWorker.kt`, `LeaderboardStore.kt`, `LeaderboardRepository.kt`, `TelemetryManager.kt`, `AppUpdateManager.kt`):**
  - Added process-wide `LeaderboardStore` locking, atomic `(uid, idToken)` session snapshots via `AuthRepository.getAuthenticatedSession()`, 5xx/malformed refresh preservation, non-blocking worker queues, transient refresh failure protection, Double-bit damage persistence, isolated `TelemetryManager` synchronization & seed fallback, HTTPS enforcement, filename sanitization, and APK package/signing-certificate SHA-256 verification before install, plus backup exclusions for auth/identity preferences.

---

## 🕹️ Previous Releases

### Version 0.0.1.028

### 🔑 Anonymous Auth Recovery, Cross-Instance Synchronization & Clear Error Diagnostics
- **Process-Wide Auth Synchronization (`AuthRepository.kt`):**
  - Replaced the per-instance `authMutex` with a shared process-wide `sharedAuthMutex` and synchronous `SharedPreferences` commits on `Dispatchers.IO`, preventing races between `RogueTDViewModel` and `ScoreSubmissionWorker` when creating or refreshing anonymous sessions.
- **Stale Native Session Recovery & REST Fallback (`AuthRepository.kt`):**
  - Verified ID token acquisition inside `ensureAuthenticated()` and detected stale, expired, or non-transiently failing native `FirebaseAuth` users: signs out the broken native session, clears matching cached UIDs, retries native anonymous sign-in, and falls back to REST Identity Toolkit so `currentUserId` and the Bearer `idToken` never mismatch.
  - Preserved active native sessions on transient `NETWORK_UNAVAILABLE`, `TIMEOUT`, and `RATE_LIMITED` failures without minting conflicting REST accounts.
- **Actionable Auth Error Classification (`AuthRepository.kt`, `ScoreSubmissionWorker.kt`):**
  - Added structured `AuthFailure` parsing across both native `FirebaseAuthException` / `ExecutionException` causes and REST Identity Toolkit / Secure Token error responses (`ADMIN_ONLY_OPERATION`, `OPERATION_NOT_ALLOWED`, `API_KEY_ANDROID_APP_BLOCKED`, `API_KEY_SERVICE_BLOCKED`, `API_KEY_INVALID`, `CONFIGURATION_NOT_FOUND`, and network/timeout failures).
  - Updated `ScoreSubmissionWorker` to propagate exact `AuthFailure` diagnostics across all pending runs (failing fast on `UNCONFIGURED` errors instead of masking every auth issue behind `"Anonymous authentication unavailable (no Firebase ID token)"`).

### Version 0.0.1.027
- **Leaderboard Auth & Offline Queue Hardening (`AuthRepository.kt`, `FirestoreLeaderboardApi.kt`, `LeaderboardRepository.kt`, `ScoreSubmissionWorker.kt`):**
  - Tightened pending run queue state transitions (`PENDING`, `SUBMITTED`, `LOCAL_ONLY`), callsign updates (`updatePendingCallsign`), WorkManager retry backoff/scheduling, and post-run status banner diagnostics (`RunSummaryDialogTest.kt`, `LeaderboardCoreTest.kt`).

### Version 0.0.1.026

### 🛡️ Combat Attribution, Tier-3 Aura Sync, 52-Tower Balance & HUD/DevMenu Fixes
- **Projectile Damage & Kill Attribution (`CombatSimulationEngine.kt`, `SimulationCoordinator.kt`):**
  - Fixed projectile towers (`GATLING`, `MORTAR`, `STELLAR`) so hit and AoE splash damage and kills properly credit `totalDamage` and `kills` on the originating `TowerInstance` for MVP tracking and telemetry.
  - Preserved wave-completion floating text notifications and skipped post-mortem tower fire on core-breach ticks, ensuring `recordRunCompletion` fires once after final state is committed.
- **Same-Tick Tier-3 Auras & Singularity Vault Parity (`TowerUpgradeManager.kt`, `TowerPlacementCoordinator.kt`):**
  - Applied accumulated Tier-3 global slow (`globalSlowBonus`) and armor-ignore (`globalArmorIgnore`) modifiers on the same simulation tick, and separated live `FROST_SPIRE` slow state from permanent `ascensionFrostSlowBonus`.
  - Fixed `confirmSingularityCollapse()` so anchor towers and multi-generational `synergyVault` entries are never double-counted across Conduit generations.
- **52-Tower Balance & Range/Fire-Rate Isolation (`TowerBalanceConfig.kt`, `GameModels.kt`):**
  - Aligned `DEFAULT_TOWER_BALANCES` with `ALL_52_DESCRIPTORS`, isolated tier-specific `findCustomTowerBalance()` overrides, prevented double-applied `towerRngMul` in `getEffectiveCombatRange()`, and stopped legacy `finalFormCode` towers from double-stacking global fire-rate buffs.
- **HUD, Upgrade Bar & Dev Menu Fixes (`HudTopBar.kt`, `TowerUpgradeBar.kt`, `DevMenuDrawer.kt`):**
  - Wired the `MinimalTopHud` pause button and deduplicated absorbed synergy counts, prevented double-firing build callbacks in `FloatingSquircleUpgradeBar`, synchronized fractional upgrade cost rounding (`roundToInt`) between UI and coordinator, and preserved Dev Menu `PROBES` state across tab switches.

---

### Version 0.0.1.025

### 🌐 Global Leaderboard Restoration & Auth Fallback
- **Dynamic Runtime API Key Resolution (`FirestoreLeaderboardApi.kt`):**
  - Restored live leaderboard connectivity by resolving the Firebase API key at runtime from `BuildConfig.LEADERBOARD_API_KEY`, `FirebaseApp.getInstance().options.apiKey`, or `FirebaseOptions.fromResource(context)`.
- **REST Identity Toolkit Auth Fallback & Token Refresh (`AuthRepository.kt`, `ScoreSubmissionWorker.kt`):**
  - Added automated anonymous sign-up and token refresh against Google Identity Toolkit / Secure Token REST endpoints when native `FirebaseAuth` is unavailable, plus forced ID token refresh retries on HTTP `403`.
- **Post-Upload Throttle Bypass (`LeaderboardRepository.kt`, `RogueTDViewModel.kt`):**
  - Added `bypassThrottle = true` on `fetchLeaderboard()` so completed background `ScoreSubmissionWorker` uploads immediately refresh the Global Ranks board.

---

### Version 0.0.1.024

### 🔄 Dual-Thumb Rotation Tabs, Strict Tier-3 Conduit Gating & Absorbed Vault HUD
- **Dual-Thumb Manual Rotation Suggestion Tabs (`RogueTDScreen.kt`):**
  - Added matching left- and right-edge manual rotation suggestion tabs whenever system auto-rotate is locked and hardware tilt differs from the HUD orientation.
- **Strict Tier-3 Conduit Ascension Gating & Escalating Cost (`TowerPlacementCoordinator.kt`, `TowerUpgradeBar.kt`, `SingularityWarningDialog.kt`):**
  - Enforced 100% Tier-3 / Final Form readiness across all buildable tiles (`isSingularityGridReady()`) and escalating Conduit ascension scrap pricing (`750 * (currentMaxGen + 1)`).
- **Absorbed Conduit Synergies HUD (`HudTopBar.kt`):**
  - Displayed banked multi-generational Conduit vaults in the Top HUD Synergies dropdown/dialog.

---

### Version 0.0.1.023

### 🖥️ Immersive Fullscreen, Draggable Bullet HUD, Frosted Edge Tabs & Updater Hygiene
- **Immersive Fullscreen & Draggable Top HUD (`MainActivity.kt`, `HudTopBar.kt`, `RogueTDScreen.kt`):**
  - Enabled transient swipe immersive fullscreen (`WindowInsetsControllerCompat`) and made `MinimalTopHud` vertically draggable with a live clock & battery indicator handle.
- **Frosted Semicircle Edge Tabs & Landscape Ergonomics:**
  - Converted left/right teleportation swap controls into compact frosted semicircle edge tabs with 48dp touch targets and tuned landscape button heights and `TowerUpgradeBar` spacing.
- **Separated Booster Pads & Updater Storage Hygiene (`MapGenerator.kt`, `AppUpdateManager.kt`):**
  - Enforced minimum Manhattan separation (`>= 4`) between same-color booster pad pairs and added `performStartupApkHygiene()` to delete corrupt/obsolete cached APKs on startup and surface already-downloaded updates.

---

### Version 0.0.1.022

### 🗺️ Every Non-Lane Tile Buildable, Dense Winding Lane Expansion, Geometry Dev Sliders, Square Grid Auto-Zoom & Affordability Dots
- **100% Buildable Board (Zero Inoperable Clearance Lockout):**
  - Removed artificial clearance locks on empty tiles: every grid coordinate without a lane, spawner, or core is `TileType.EMPTY` with `isClearance = false` and fully buildable (`isBuildable == true`).
  - Preserved all 6 Booster Pads (2 Green `AMPLIFIER`, 2 Teal `HIGH_GROUND`, 2 Orange `CONDUIT`) adjacent to lanes, counting towards total buildable spots.
- **Dense Winding Lane Expansion & Target Open Spots:**
  - Instead of locking blank tiles, procedural lanes wind densely across the grid using 2-tile U-bends (`A -> B` to `A -> A' -> B' -> B`) until the board reaches the desired `targetOpenSlots` (e.g. 15 available tower slots).
  - Parity-matching 1-tile detour across adjacent opposing lane tiles guarantees exact parity alignment without getting stuck.
  - All invariants strictly verified: Manhattan distance 1 continuity, no single-lane loops, all 4 corners touched, and central spine overlap clamped within `minLaneOverlap..maxLaneOverlap`.
- **Per-Difficulty Open Slots, Overlap & Map Size Dev Sliders:**
  - Added new **"MAP GEOMETRY & LANE COVERAGE"** section to Tab 0 (`ECONOMY`) of the Dev Menu Drawer with interactive sliders for:
    - Easy Mode Available Tower Spots (`6..40`, default 15)
    - Medium Mode Available Tower Spots (`6..40`, default 15)
    - Hard Mode Available Tower Spots (`6..40`, default 15)
    - Min Lane Overlap (`2..20`, default 4) & Max Lane Overlap (`4..30`, default 12)
    - Map Grid Columns / Width (`6..16`, default 10) & Map Grid Rows / Height (`10..28`, default 20)
  - Full persistence via `SharedPreferences` and automatic diff export under `"deltas"` in `DevBalanceConfig.exportDiffJson()`.
- **Live Map Regeneration Before Wave 1:**
  - When map geometry sliders or game difficulty change while Wave 1 has not started (`!isWaveActive && waveNumber == 1 && towers.isEmpty()`), the map automatically regenerates with full track particles for immediate visual preview.
- **Strictly Square Grid Cells & Dynamic Viewport Auto-Zoom:**
  - Replaced aspect-ratio distortion with uniform 1:1 square cell calculations across both render and touch hit-testing: `cellSize = minOf(width / gridWidth, height / gridHeight)`, dynamically zooming and centering the board as rows or columns change.
- **Affordability Indicators on Buildable Tiles:**
  - Buildable empty tiles now display a subtle, faint pulsing white center dot whenever the player has accumulated enough Scrap to afford the lowest-cost unlocked tower class.

---

## 🕹️ Previous Releases

### Version 0.0.1.021

### 🛡️ Architecture Hardening, Synergy Singularity Economy Calibration & Combat Cadence Overhaul
- **Synergy Singularity Guaranteed Across EASY, MEDIUM & HARD:**
  - Curated procedural map generation (`MapGenerator.kt`) to a focused **15 tactical buildable pads** (6 boosted Harmonic pads + 9 lane-adjacent tactical pads) while converting surrounding non-lane terrain to scenic sector bedrock (`TileType.OBSTACLE`).
  - Added difficulty-aware economy scaling (`scrapIncomeMultiplier`: EASY `1.45x`, MEDIUM `1.20x`, HARD `1.05x`, INFINITE `1.00x`) across wave completion bonuses (`WaveGenerator.kt`) and enemy kill bounties (`SimulationCoordinator.kt`), guaranteeing players can fill 100% of buildable pads to Tier 2+ and forge the **Synergy Conduit** well before the final wave in every difficulty mode.
- **Deliberate Combat Cadence & Archetype Fire-Rate Rebalance:**
  - Lowered all 7 base `TowerType` fire rates by ~30–35% (`GATLING = 0.80/s`, `CRYO = 1.05/s`, `TESLA = 0.48/s`, `MORTAR = 0.28/s`, `LASER = 1.15/s`, `VOID = 0.40/s`, `STELLAR = 0.60/s`) and all 36 `TowerArchetype` fire rates by ~40%, with proportional per-shot damage compensation.
  - Eliminated fire-rate multiplier double-dipping in `SimulationCoordinator.kt`.
- **Zero-Allocation Hot-Loop Balance & Upgrade Resolution (Flaw 1 Fix):**
  - Pre-allocated static $O(1)$ lookup maps in `TowerUpgradeManager.kt` and `DevBalanceConfig` (`DEFAULT_TOWER_BALANCES`, `STATIC_ENEMY_BALANCES`) and added `cachedBalance` on `TowerInstance`, eliminating 480+ per-second heap allocations during 60–120 FPS combat ticks.
- **Double-Buffered Entity Lists & HUD Recomposition Isolation (Flaw 2 Fix):**
  - Implemented A/B double-buffered entity lists (`scratchSurvivingEnemiesA/B`, `scratchSurvivingProjectilesA/B`) in `CombatSimulationEngine.kt` to prevent concurrent UI list mutation, and isolated discrete HUD state (`toHudState()`) from high-frequency 60–120 FPS render state (`RenderState`) in `RogueTDScreen.kt`.
- **Exact Dynamic Scrap Investment & 75% Sell-Back Math (Flaw 3 Fix):**
  - Added `investedScrap: Int` tracking directly on `TowerInstance` across placement and all upgrade paths, ensuring `TowerUpgradeManager.getSellValue()` always refunds an exact 75% of actual scrap spent (including dynamic Final Form scaling).
- **Unified Combat Range Progression (Flaw 4 Fix):**
  - Fixed shadowed `finalFormCode` vs. `tier` range conditions in `TowerInstance.getEffectiveCombatRange()` so Tier 2 (`1.20x`) and Tier 3 (`1.35x`) range bonuses apply cleanly without shadowing or double-multiplying.
- **Comprehensive NaN & Zero-Division Physics Guards (Flaw 5 Fix):**
  - Added strict `.isFinite()` and zero-denominator guards across projectile steps, enemy path segments, AoE splash radii, and explosion lifetimes in `CombatSimulationEngine.kt`.
- **Fail-Fast Firestore Security Rule Handling (Flaw 6 Fix):**
  - Removed redundant cross-collection failover on HTTP `403 PERMISSION_DENIED` (`FailureKind.SECURITY_REJECTED`) in `FirestoreLeaderboardApi.kt`, cutting rejected request overhead in half.

---

## 🕹️ Previous Releases

### Version 0.0.1.020

### 🔒 Accessibility Font-Scale Lock, Standardized Dev JSON Metadata & Two-Stage Soft Back Navigation
- **Universal UI Font-Scale Lock (`fontScale = 1.0f`):** Locked `LocalDensity` in `MyApplicationTheme` (`Theme.kt`) and `Configuration.fontScale` in `MainActivity.attachBaseContext()` / `onConfigurationChanged()` to `1.0f`. All tactical HUDs, floating squircles, and popup `Dialog(...)` sub-windows now maintain exact layout proportions regardless of Android system accessibility font-size settings.
- **Standardized Dev Menu & Library JSON Exports:** Updated `DevBalanceConfig.exportDiffJson()` and `SoundManager.exportLibraryJson()` to always include top-level `"appName": "Dual Front TD"`, `"versionName"`, and `"versionCode"` metadata along with a structured `"deltas"` block containing all modified balance, audio, visual, and favorited parameters.
- **Two-Stage Android Soft Back Arrow Navigation:** Added a context-aware `BackHandler` in `RogueTDScreen.kt`:
  - **Stage 1 (Squircles Present):** Pressing Android's soft back arrow while a tower or buildable empty tile is selected cleanly closes the floating squircle bar.
  - **Stage 2 (No Squircles Present):** Pressing Android's soft back arrow when no squircles are open opens the Pause Menu instead of exiting the match.

---

## 🕹️ Previous Releases

### Version 0.0.1.019
- **750-Line Architectural Modularity Law (100% Compliance):** Restructured all 54 production Kotlin source files under `app/src/main/java/com/example/` so every single file strictly adheres to the `<= 750`-line architectural ceiling with high domain cohesion.
- **God-ViewModel Decomposition & Domain Coordinators:** Reduced `RogueTDViewModel.kt` to 596 lines by extracting focused domain coordinators into `engine/coordinator/`:
  - `SimulationCoordinator.kt`: Virtual time scaling, combat simulation ticking, track particle stepping, status effect decay, and sound tick dispatch.
  - `TowerPlacementCoordinator.kt`: Coordinate grid snapping, path clearance verification, scrap transactions, turret construction, and singularity collapse orchestration.
  - `WaveProgressionCoordinator.kt`: Dual-lane spawn timers, wave queue popping, boss multiplier scaling, and wave completion payouts.
- **UI & Canvas Sub-Renderer Decomposition:**
  - Split `GameCanvas.kt` into specialized sub-renderers under `ui/canvas/`: `TerrainRenderer.kt`, `TrackRenderer.kt`, and `EntityRenderer.kt`, eliminating all per-frame `listOf(...)` allocations inside draw loops.
  - Extracted HUD and dialog surfaces into `ui/hud/components/` (`TargetPriorityMenu.kt`, `SingularityWarningDialog.kt`) and `ui/dialog/` (`RunSummaryDialogs.kt`, `TelemetryLeaderboardDialog.kt`, `AudioVisualCustomizationPanel.kt`, `DevMenuComponents.kt`), and cached vector `Path` instances in `TowerUpgradeBar.kt`.
- **Model, Config & Audio Partitioning:** Partitioned core models into `EntityModels.kt`, `MapModels.kt`, `ThemeModels.kt`, and `TowerSynergyConfig.kt`, and split procedural audio synthesis into `ProceduralAudioSynthesizer.kt` and `AudioSynthesizer.kt`.
- **Zero-Allocation Hot-Loop & Spatial Grid Hardening:** Eliminated per-tick collection copies (`toMutableList()`, `.filter`, `.map`, `.associateBy`) across `SimulationCoordinator.kt` and `CombatSimulationEngine.kt`, added reusable `DamageResult` containers in `DamageCalculator.kt`, hardened `SpatialHashGrid.queryNearby` with caller-supplied buffers and in-place predicates, and added strict zero-distance/NaN guards to projectile step math.

---

## 🕹️ Previous Releases

### Version 0.0.1.018
- **Global Leaderboard Restoration & UX Navigation:** Fixed navigation routing from the Pause Menu and Post-Run Summary Dialog to route directly to Tab 2 (Global Ranks). Added automatic score refreshing on tab switch via reactive Compose effects, and synchronized background WorkManager score upload completion directly with `LeaderboardRepository` for instant rank updating.
- **REST Identity Toolkit Auth Hardening:** Hardened `AuthRepository` with graceful fallback to Firebase REST Identity Toolkit anonymous authentication when native Firebase Auth initialization is unavailable, preserving strict unauthenticated device UUID fallback contracts. Added real-time score upload status feedback to post-run dialogs.
- **Simulation Decoupling via `CombatSimulationEngine`:** Extracted pure simulation math, ballistics calculation, enemy path progression, Cryo shatterburst effects, and visual particle simulation from `RogueTDViewModel.kt` into `CombatSimulationEngine.kt`, reducing ViewModel complexity and enforcing clean separation of concerns.
- **Spatial Hash AoE Acceleration:** Upgraded explosive munitions sweeps (Mortar artillery, Hellfire blasts, Cluster munitions, and Void singularity splash) from full linear scans to $O(K)$ localized spatial neighborhood queries via `SpatialHashGrid.getPotentialCollisions(x, y, radius)`.
- **Domain Model Partitioning:** Partitioned monolithic `GameModels.kt` by separating tower archetypes, classes, and balances into `TowerBalanceConfig.kt` and enemy roles, shapes, and archetypes into `EnemyBalanceConfig.kt`, maintaining high cohesion and strict file size boundaries.
- **Zero-Allocation Hot-Loop Scratch Buffering:** Implemented pre-allocated double-buffered scratch lists for surviving entities and AoE collision queries in `CombatSimulationEngine`, eliminating per-tick garbage collection pauses during high-density 100+ swarm waves.

---

## 🕹️ Previous Releases

### Version 0.0.1.017
- **Combat Cadence & Level 1–3 Progression Salience:** Rebalanced baseline starter weapon fire rates by ~40–50% (Gatling 1.2/s, Cryo 1.6/s, Tesla 0.7/s, Mortar 0.40/s, Laser 1.8/s, Void 0.6/s, Stellar 0.9/s) and scaled per-shot base damage up (+80% to +130%) so early-game towers deliver punchy, measured strikes rather than hose-like sprays. Scaled upgrade progression through Tier 1 (+20% SPD, +15% DMG), Tier 2 (+40% SPD, +35% DMG), and Tier 3 (+70% SPD, +60% DMG) so evolutions feel truly transformative.
- **Physical Ballistics & Projectile Travel Time:** Tuned ballistic velocities (Gatling 8.5, Mortar 4.2, Stellar 6.0) so munitions travel across the grid with realistic flight physics and artillery arcs, allowing players to track ordnance flight time.
- **Player-Chosen Synergy Conduit & Permanent Teaser Squircle:**
  - On Tier 3 maxed towers, the 4th upgrade slot now houses an interactive Conduit dock.
  - When the grid is incomplete, it acts as a progress teaser showing ready counts (`X/Y Ready`), with tapping triggering floating HUD guidance.
  - When the grid is 100% full with Tier 2/3 towers, it illuminates in electric gold (`[★ FORGE ★ CONDUIT]`), allowing players to choose *which specific tower* collapses the board into the Synergy Conduit monument.
  - **Strictly ONE Synergy Conduit Invariant:** Sacrificing an older Conduit into a newly chosen tower absorbs all past banked vaults and generation ranks, keeping exactly one monument on the battlefield.
- **Tier 3 Squircle Dynamic Cost Synchronization:** Fixed cost desynchronization where the upgrade UI displayed a flat 280 scrap while the engine charged dynamic multi-upgrade costs.
- **Expanded Map Grid & Tactical Lanes:** Expanded default map height from 18 to 20 (+2 rows, 200 tiles, 14–18 buildable pads) and widened procedural lane paths (`minLaneLength = 16, maxLaneLength = 32`) with corner legs touching outer edges before converging at the nexus spine.

---

## 🕹️ Previous Releases

### Version 0.0.1.016
- **Zero-Allocation Compose Rendering Engine:** Refactored `GameCanvas.kt` rendering pipeline with singleton path reuse (`scratchPath1`, `scratchPath2`, `lanePathA`, `lanePathB`, `mergedPath`), eliminating all per-frame `Path()` heap allocations across tower skins, badges, enemy vectors, and tactical pointers.
- **Hot-Loop Heap Elimination:** Replaced all high-frequency `UUID.randomUUID().toString()` string instantiations in weapon fire, enemy spawning, and projectiles with an atomic, zero-allocation primitive sequence generator (`"p" + nextEntityId++`).
- **Dirty-Flag Synergy Caching:** Implemented dirty-flag caching (`isSynergyCacheDirty`) for `computeActiveSynergies()`. Active synergy set calculations are cached across ticks and recomputed only on structural state changes (tower build, upgrade, sell, or singularity collapse).
- **$O(1)$ Projectile-Enemy Spatial Indexing & Squared Distance Comparisons:** Pre-indexed surviving enemies by ID (`enemyMap`) in projectile update loops to replace $O(P \cdot E)$ double scans with $O(1)$ hash lookups, and replaced per-tick Euclidean `Math.hypot` calls with squared distance comparisons (`dx * dx + dy * dy <= r * r`).
- **Asymptotic Infinite Wave Ceiling:** Hardened `WaveGenerator.kt` late-game exponential scaling with an asymptotic double-clamp (`minOf(post50, 300.0)` and `minOf(1.0e12f, w50Base * expCurve)`), preventing `Float.POSITIVE_INFINITY` or `NaN` overflow while maintaining exact escalation curves.
- **Global Movement Floor Safeguard:** Clamped hostile slow multipliers (`maxOf(0.15f, 1f - state.globalFrostSlowBonus)`) to prevent reverse-walking enemies under stacked global frost buffs.
- **Modular Simulation Decoupling:** Decoupled `CinematicController.kt` from `RogueTDViewModel.kt`, removing over 500 lines of showcase logic and enforcing single-responsibility architecture across the simulation layer.
- **Architecture & Modularity Standards:** Enforced zero-allocation 60/120 FPS loop invariants, asymptotic math soft-caps, and modular file-size boundaries across game simulation and rendering layers.

---

## 🕹️ Previous Releases

### Version 0.0.1.015
- **The Synergy Singularity (Board-Merge Capstone Loop):** Fill and max 100% of the buildable map grid (Tier ≥ 2 Archetype or Apex) to unlock the high-stakes `[★ SINGULARITY CAPSTONE]` squircle on your oldest anchor tower. Activating it permanently banks all active global synergies and auras into a multi-generational `BankedSynergyVault`, clearing the remaining tiles for drafting fresh generations of towers.
- **Mid-Wave Tactical High-Alert:** Singularity activation is supported unrestricted—even during intense mid-wave engagements—via a tactical confirmation modal displaying sacrificed tower counts and exact scrap refund.
- **20% Singularity Scrap Bounty:** Sacrificing your board refunds 20% of all invested scrap across all sacrificed towers directly into the treasury (minimum 250c), enabling instant drafting of 2–3 starter towers for Generation N+1.
- **Persistent Multi-Generational Monument:** Banked synergies stack cumulatively generation after generation inside the single Capstone monument, creating massive compounding aura curves.
- **4-Tier Difficulty Modes:** Choose your challenge level in the Pause Menu—`EASY` (Wave 30 milestone), `MEDIUM` (Wave 50 milestone), `HARD` (Wave 100 milestone), and `INFINITE` (endless escalation).
- **True Exponential Escalation Math:** Hard mode scales exponentially past Wave 30 ($1.35 \times 1.045^{\text{wave}-30}$) and Infinite mode scales with an aggressive $1.08^{\text{wave}-50}$ curve past Wave 50.
- **Asymptotic Engine Safeguards:** Clamped enemy slow factors to a minimum of 15% speed (preventing 0% freeze locks) and enforced a 50ms turret firing cooldown floor (20 shots/sec cap) to safeguard CPU performance under multi-generational synergy overclocking.

---

## 🕹️ Previous Releases

### Version 0.0.1.014
- **Standardized Rotated Bullet HUD:** All rotated indicators (Wave, HP, Scrap, Synergies, Teleportation Pads, Auto-Wave, and Begin) standardized to a sleek, uniform `28.dp x 38.dp` layout, providing generous vertical headroom that eliminates text clipping in landscape orientation.
- **Streamlined Launch & Rotation Parity:** Streamlined Begin button with a single play arrow and vertical icon/label stacking under rotation; decoupled the Settings gear from rotation; added automatic system rotation lock (`ACCELEROMETER_ROTATION`) compliance with an on-tilt suggestion trigger.
- **Full-Width Edge-Anchored Landscape Dropdowns:** Overhauled Synergies and Teleportation Pads menus in landscape to use full-width unconstrained dialogs (`usePlatformDefaultWidth = false`) anchored directly adjacent to the Top HUD on the active edge, providing 320–360dp width so all mechanics and 6/6 Harmonic Grid details display clearly without clipping.
- **100% Unique 52-Tower Palette & Squircles:** Every single tower across all 4 classes, 12 Tier 1 vectors, and 36 Tier 2 archetypes now has a completely unique, non-overlapping hex color. Upgraded squircles feature centered title/subtitle typography matching the tower's unique hex.
- **In-Place Target Priority & Sell Ergonomics:** Restored muscle-memory ergonomics by pinning Target Priority and Sell together on the right flank adjacent to upgrade squircles, counter-rotating in place without jumping across the screen.
- **Deselect-on-Reselect Toggle:** Tapping an already selected tower or tile immediately dismisses the bottom upgrade dock without requiring players to tap a neutral track tile.

---

## 🕹️ Previous Releases

### Version 0.0.1.013
- **Dev-Tuning Disqualification Resolution:** Completely disentangled audio volumes, mute toggles, music track selections, turret skins, and visual themes from dev-tuning detection via `isGameplayModified()`. Legitimate player runs are never disqualified from global rankings by player preferences or cosmetic customizations.
- **Cold-Start Network Resiliency:** Opening the telemetry modal or in-game pause leaderboard directly bypasses query throttles (`forceRefresh = true`) and auto-retries initial network socket handshakes during cold boot.
- **Verified Uplink Empty-State UX:** Cleanly differentiated between connected empty scoreboards (`GLOBAL UPLINK VERIFIED • CONNECTED`) and actual network drops, providing an instant `"Refresh Rankings"` affordance.
- **Google Identity Toolkit REST Fallback:** Hardened `AuthRepository` with an automated REST fallback for Anonymous Authentication, ensuring valid JWT tokens on all Android devices and emulators.
- **Canonical Collection Normalization & Fail-Safe Fallback:** All client requests are automatically sanitized and normalized to canonical `/leaderboards`, with seamless cross-path failover to `/leaderboard` to ensure 100% submission and query reliability.
- **Dual-Path Security Rules Alignment:** Cloud security rules strictly authorize and validate both `/leaderboards/{runId}` and `/leaderboard/{scoreId}` with identical field schemas, eliminating HTTP 403 `PERMISSION_DENIED` errors.
- **Structured Logcat Telemetry:** Integrated verbose, structured diagnostic logging across `[FirestoreLeaderboardApi]`, `[LeaderboardRepository]`, and `[ScoreSubmissionWorker]` for full real-time device observability.
- **Graceful UI State Protection:** Round-end score submission cleanly displays `QUEUED FOR UPLOAD (OFFLINE)` whenever scores are safely staged in the local queue, preventing false alarm failure banners.

---

## 🕹️ Previous Releases

### Version 0.0.1.011
- **Production Index Alignment:** Fully aligned Firestore composite indexing sorting primarily by highest wave reached (`maxWave` DESC), with tie-breaking by high score (`highScore` DESC) and submission timestamp.
- **Dynamic Secure Key Resolution:** Build configuration dynamically resolves Firebase credentials via environment variables, local configuration, or `google-services.json`, keeping repositories completely clean of hardcoded keys.
- **Resilient Fallback Query Engine:** Self-healing query pipeline with client-side dual-sort ensures scoreboard availability across all network conditions and provisioning states.
- **Strict Identity & Offline Zero-Loss Queue:** Submissions are cryptographically bound to authenticated Firebase Anonymous player tokens with WorkManager offline retry queueing.

### 👤 Native Anonymous Authentication & Zero-Friction Identity (v0.0.1.009)
- **Silent Player Identity:** Integrated native Firebase Anonymous Authentication for seamless player identity. Every commander receives a persistent, secure cryptographic identity without sign-up forms, passwords, or personal data collection.
- **Score Authorship Protection:** Security rules strictly verify that leaderboard submissions match the authenticated user identity while keeping scores tamper-proof and immutable.

### 🏆 Dual-Metric Composite Global Leaderboards
- **Composite Ranking Query:** Implemented dual-criteria composite ranking sorted primarily by `maxWave` descending, using `highScore` as an instant tie-breaker, backed by dedicated cloud composite indexing for minimal read overhead.
- **Local Commander Highlight:** Dynamically flags and highlights the active commander's rank entry on the global scoreboard.

### 🌐 Real-Time Remote Balancing (Remote Config)
- **Dynamic Wave & Economy Multipliers:** Bound game engine balance curves to cloud configuration parameters, enabling on-the-fly adjustments to enemy health scaling, scrap bounties, boss health multipliers, and harmonic grid damage boosts.

### 📊 Churn Telemetry & Difficulty Spike Pinpointing
- **Failure State Analytics:** Instrumented granular failure event logging (`level_fail`) capturing the exact failure wave, tower count, remaining scrap currency, and MVP tower class to identify overtuned difficulty cliffs.
- **FTUE Funnel Metrics:** Added telemetry tracking across initial hint engagement and wave progression milestones.

---

## 🕹️ Previous Updates (Version 0.0.1.008)

### 🔐 Persistent Signing Certificate Parity & Conflict Resolution
- **Canonical Keystore Parity:** Aligned local and release APK signing with the same canonical debug keystore, preventing `"App not installed as package conflicts with an existing package"` signature mismatches on in-app updates.
- **Self-Healing ADB Companion:** Enhanced `Dual-Front-TD-ADB.exe` with automatic signature conflict detection; cleanly uninstalls mismatched legacy certificates and installs new packages in a single seamless step.

### 🎮 Tactical Ergonomics & UI Rotation Fluidity (v0.0.1.007 / v0.0.1.006 Highlights)
- **Booster Pad Dropdown Capsule:** Converted the wide booster pad sub-strip into an interactive dropdown capsule (`⚡ X/6`) alongside aura synergies detailing scrap boost bonuses, the 6/6 Harmonic Grid buff (+20% Tower Damage / +25% Scrap across all lanes), and dual-thumb repositioning controls.
- **Dynamic Orientation Stacking:** In portrait mode, Core HP and Scrap render inline horizontally to minimize top HUD thickness. In landscape rotation, Core HP, Scrap, and Auto-Wave dynamically stack vertically into thin pills to preserve horizontal track clearance.
- **Side-by-Side Target Priority & Sell Squircles:** In portrait, Target Priority and Sell buttons stack vertically in a 44dp column to match squircle height (reducing dock width to 296dp). In landscape rotation, they deploy side-by-side.
- **Tier 0 Compound Upgrade Naming:** Vector upgrade buttons now use unified compound naming and archetype theme colors (`Kinetic Offense`, `Void Defense`, `Siege Support`, `Stellar Offense`).
- **Clean Settings Gear:** Removed the opaque container box behind the settings gear cog for a clean borderless floating icon.
- **Tactical Map Expansion (+2 Rows: 10x18):** Expanded battlefield map height by 2 rows with synchronized spatial hash indexing for deeper choke-point positioning.

### 🎬 In-Game Feature Showcase Polish & Touch Lock
- **Automated Feature Demonstrations:** In-game cinematics now execute real-time gameplay demonstrations (targeting reticles, booster pad teleportation maneuvers, and squircle upgrade bar inspections).
- **Playback Input Protection:** Complete screen touch locking during cinematic playback ensures zero accidental interruptions.
- **Direct Clipboard Integration:** Launching any showcase copies its standard clip reference (`v001004_CinematicX.mp4`) to the device clipboard.

---

## 📜 Prior Patch Highlights

### Version 0.0.1.003
- **True Screen Orientation & HUD Counter-Rotation:** Coordinate decoupling keeps tactical game canvas fixed, while top Bullet HUD (`HudTopBar.kt`), floating squircle action bar (`TowerUpgradeBar.kt`), and combat damage numbers smoothly counter-rotate (`Modifier.rotate(-angle)` / `graphicsLayer`) so all text and metrics stay readable in 0°, 90°, 180°, and 270°.
- **Floating Squircle Upgrade Action Bar:** Replaced heavy bottom drawer sheet with sleek 56dp x 56dp squircle buttons floating 32dp above gesture bar. Features glowing neon borders, minimal cost badges (e.g. `80c`, `140c`, `180c`, `210c`), spring scale tap feedback, and zero multi-paragraph clutter.
- **6 In-Game Feature Cinematics:** Built-in deterministic simulation engine running at locked 60 FPS (1,800 frames per scenario) accessible via the DEV menu showcasing peak combat and tactical mechanics.
- **Firebase Leaderboard Roundtrip Hardened:** Validated REST submission with SHA-1 cert fingerprint `174775252333dc2e8159f5ac6de1019661c3e897`, RPC diagnostics, and offline retry queue.

### Version 0.0.1.002
- **52-Tower Evolution Matrix:** Introduced Class 4: Stellar Cosmic (`STELLAR_COSMIC`) with 9 final form archetypes (Supernova Pulsar, Singularity Bastion, Graviton Lance) and cosmic beam piercing.
- **Booster Pad Teleportation:** 6 color-paired booster pads with instant tactical swaps, 750ms cooldown, and +3% scrap bounty per occupied pad.
- **Audio Polish & Low-Pass Filtering:** Smooth 600–1000ms gradual fades and mellow ambient synthesizer filtering (<650Hz).
- **Rebalanced Economy:** Tuned early-game scrap bounties across waves 1–100.

### Version 0.0.1.001
- **Spatial Hash Grid Collision Engine (`SpatialHashGrid.kt`):** Replaced brute-force projectile checks with a uniform spatial partitioning grid, locking smooth 60/120 FPS performance during massive swarm waves.
- **Modular UI Refactor:** Separated HUD, upgrade bar, and menus into single-responsibility components.
- **39-Tower Hierarchy:** Completed initial 3 -> 9 -> 27 branching upgrade progression across Kinetic, Elemental, and Siege families.
- **Procedural Level 1–4 Visual Upgrade Cues:** Procedural chassis, stabilizer wings, focus barrels, and Apex radiant coronas.

### Version 0.0.0.013
- **Pause Menu Audio Attenuation:** Music smoothly drops -6dB when paused and resumes instantly when returning to the fight.
- **Interactive Sound Auditioning:** Preview gunfire sounds and background music tracks directly in the settings menu.
- **Extended Procedural Music:** 45-second looping synthesizer compositions with smooth tapered transitions.
- **Leaderboard Resilience:** Resolved API token authentication on score uploads and added offline retry handling.

### Version 0.0.0.012
- **Screen Rotation & Adaptive Layout:** Locked the underlying canvas coordinates to prevent hitbox drift while dynamically rotating HUD text chips and menus across portrait and landscape.
- **Focus Loss Audio Termination:** Sound immediately cuts when switching apps or locking the screen, eliminating ghost audio loops.

### Version 0.0.0.010
- **Official Rebrand to Dual Front TD:** Updated application identity, standalone Windows launcher (`Dual Front TD.exe`), and improved board verticality (16-row layout).

---

## 🎮 Core Gameplay Systems

- **Dual-Front Creep Assault:** Creeps spawn simultaneously along dual conduits (`Lane Alpha` and `Lane Beta`), winding through all 4 corners, merging at a central nexus, and converging on your Core.
- **Tactical Evolution Tree:** 4 base towers branching into 12 Level 2 specializations and 36 Level 3 advanced forms (52 total units).
- **Compound Synergy Matrix:** Stacking tower families and classes activates board-wide passive buffs.
- **Detailed Run Telemetry:** Per-run breakdowns tracking enemies neutralized, total DPS output, MVP defense towers, scrap economy, and worldwide leaderboard standings.

---

## 🛠️ Tech Stack

- **Language:** Kotlin 2.4.21
- **UI Framework:** Jetpack Compose & Material 3
- **Architecture:** MVI / Unidirectional Data Flow with Android Architecture Components (`ViewModel`, `StateFlow`)
- **Graphics:** Hardware-accelerated Compose Canvas with custom coordinate transform matrices
- **Testing:** Robolectric, JUnit 4, AndroidX Test Runner (167/167 passing)
- **Target Platform:** Android (Target SDK 36, Android 16 ready, 16 KB page-size compatible) + Windows Companion Runner

---

## 🚀 Building from Source

```bash
# Build debug APK
./gradlew assembleDebug

# Compile portable Windows companion launcher (.exe)
./gradlew buildWindowsLauncher

# Run full automated test suite
./gradlew testDebugUnitTest
```

Build outputs:
- Android APK: `app/build/outputs/apk/debug/app-debug.apk` (and staged under `apk/Dual-Front-TD-v0.0.1.031-debug.apk`)
- Local ADB Runner: `apk/Dual-Front-TD-ADB.exe` (general ADB companion shell)
