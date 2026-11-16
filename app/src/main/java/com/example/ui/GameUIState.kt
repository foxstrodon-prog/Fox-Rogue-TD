package com.example.ui

import com.example.audio.MusicTrack
import com.example.audio.SoundProfile
import com.example.engine.AppUpdateState
import com.example.engine.WaveData
import com.example.model.*

data class GameUIState(
    val seed: Long = 0L,
    val level: GeneratedLevel? = null,
    val towers: List<TowerInstance> = emptyList(),
    val enemies: List<EnemyInstance> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val laserBeams: List<LaserBeam> = emptyList(),
    val lightningArcs: List<LightningArc> = emptyList(),
    val explosions: List<ExplosionEffect> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),

    // Global Ascension Boosts
    val globalKineticSpeedBonus: Float = 0f,
    val ascensionFrostSlowBonus: Float = 0f,
    val globalFrostSlowBonus: Float = 0f,
    val globalTeslaArcsEnabled: Boolean = false,
    val globalMortarSplashBonus: Float = 0f,
    val globalSolarCritsEnabled: Boolean = false,
    val globalVoidShredEnabled: Boolean = false,
    val globalDamageBonus: Float = 1.0f,
    val globalRangeBonus: Float = 0f,
    val scrapBountyBonus: Float = 1.0f,

    val coreHp: Int = 30,
    val maxCoreHp: Int = 30,
    val scrap: Int = 420,
    val waveNumber: Int = 1,
    val currentWaveData: WaveData? = null,
    val isWaveActive: Boolean = false,
    val gameSpeed: GameSpeed = GameSpeed.NORMAL,
    val selectedTile: MapTile? = null,
    val selectedTower: TowerInstance? = null,
    val isGameOver: Boolean = false,
    val isVictory: Boolean = false,
    val totalKills: Int = 0,
    val totalScrapEarned: Int = 420,
    val totalDamageDealt: Float = 0f,
    val screenShakeIntensity: Float = 0f,
    val autoCallNextWave: Boolean = false,
    val overchargeRemainingMs: Long = 0L,
    val leaderboard: List<RunRecord> = emptyList(),
    val lifetimeTelemetry: LifetimeTelemetry = LifetimeTelemetry(),
    val isTelemetryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isPauseMenuOpen: Boolean = false,
    val prePauseSpeed: GameSpeed = GameSpeed.NORMAL,
    val hasClickedFirstTile: Boolean = false,
    val balanceConfig: DevBalanceConfig = DevBalanceConfig(),
    val towerPerformance: List<TowerPerformanceRecord> = emptyList(),
    val bestWaveRecord: Int = 1,
    val trackParticles: List<TrackParticle> = emptyList(),
    val activeGlobalSynergies: List<GlobalAuraSynergy> = emptyList(),
    val isSynergyPanelExpanded: Boolean = false,
    val showEndRunConfirmDialog: Boolean = false,
    val harmonicGridActive: Boolean = false,
    val harmonicPadsCount: Int = 0,
    val harmonicGridDamageBoost: Float = 0.20f,
    val activePlayerName: String = "Operative",
    val lastRunRecord: RunRecord? = null,
    val playerLifetimeTelemetry: LifetimeTelemetry = LifetimeTelemetry(),
    val animTimeMs: Long = 0L,
    val deviceRotationAngle: Float = 0f,
    val hardwareTiltAngle: Float = 0f,
    val isSystemAutoRotateLocked: Boolean = false,
    val onlineLeaderboard: List<OnlineScoreEntry> = emptyList(),
    val leaderboardStatus: GlobalBoardStatus = GlobalBoardStatus.IDLE,
    val leaderboardPendingCount: Int = 0,
    val leaderboardErrorMessage: String? = null,
    val runSubmissionState: RunSubmissionState = RunSubmissionState.NONE,
    val leaderboardSubmissionError: String? = null,
    val updateUIState: AppUpdateState = AppUpdateState(),
    val showUpdateModal: Boolean = false,
    val soundProfile: SoundProfile = SoundProfile.CYBER_SYNTH,
    val musicTrack: MusicTrack = MusicTrack.ROLLING_PLAYLIST,
    val masterVolume: Float = 0.7f,
    val sfxVolume: Float = 0.8f,
    val upgradeVolume: Float = 0.85f,
    val musicVolume: Float = 0.45f,
    val isAudioMuted: Boolean = false,
    val kineticSfxSlot: Int = 1,
    val elementalSfxSlot: Int = 1,
    val siegeSfxSlot: Int = 1,
    val turretSkinSlot: Int = 1,
    val mapThemeSlot: Int = 1,
    val towerVisualSkin: String = "Vector Blueprint",
    val activeCinematic: ActiveCinematicState? = null,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM,
    val isSingularityWarningOpen: Boolean = false,
    val telemetryInitialTab: Int = 0
) {
    /**
     * Extracts the high-frequency 60-120 FPS render state for [GameCanvas].
     */
    fun toRenderState(): RenderState = RenderState(
        enemies = enemies,
        projectiles = projectiles,
        laserBeams = laserBeams,
        lightningArcs = lightningArcs,
        explosions = explosions,
        floatingTexts = floatingTexts,
        trackParticles = trackParticles,
        screenShakeIntensity = screenShakeIntensity,
        animTimeMs = animTimeMs
    )

    /**
     * Returns a normalized discrete HUD state with high-frequency tick fields zeroed out,
     * preventing Jetpack Compose from recomposing HUD bars on pure physics/movement frames.
     */
    fun toHudState(): GameUIState = copy(
        enemies = emptyList(),
        projectiles = emptyList(),
        laserBeams = emptyList(),
        lightningArcs = emptyList(),
        explosions = emptyList(),
        floatingTexts = emptyList(),
        trackParticles = emptyList(),
        screenShakeIntensity = 0f,
        animTimeMs = 0L,
        overchargeRemainingMs = if (overchargeRemainingMs > 0L) 1L else 0L,
        totalDamageDealt = 0f
    )
}

/**
 * Dedicated high-frequency render state stream read inside the [GameCanvas] draw phase.
 */
data class RenderState(
    val enemies: List<EnemyInstance> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    val laserBeams: List<LaserBeam> = emptyList(),
    val lightningArcs: List<LightningArc> = emptyList(),
    val explosions: List<ExplosionEffect> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val trackParticles: List<TrackParticle> = emptyList(),
    val screenShakeIntensity: Float = 0f,
    val animTimeMs: Long = 0L
)
