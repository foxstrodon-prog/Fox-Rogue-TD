package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.*
import com.example.engine.network.AuthRepository
import com.example.engine.repository.LeaderboardRepository
import com.example.engine.storage.LeaderboardStore
import com.example.engine.worker.LeaderboardWorkScheduler
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

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
    val scrap: Int = 420, // Ample starting credits for dual-lane defense setup
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
    val updateUIState: AppUpdateState = AppUpdateState(),
    val showUpdateModal: Boolean = false,
    val soundProfile: com.example.audio.SoundProfile = com.example.audio.SoundProfile.CYBER_SYNTH,
    val musicTrack: com.example.audio.MusicTrack = com.example.audio.MusicTrack.ROLLING_PLAYLIST,
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
    val isSingularityWarningOpen: Boolean = false
)

class RogueTDViewModel @JvmOverloads constructor(
    application: Application,
    val authRepository: AuthRepository = AuthRepository(application),
    val remoteConfigManager: RemoteConfigManager = RemoteConfigManager(),
    val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(
        store = LeaderboardStore(application),
        scheduler = LeaderboardWorkScheduler(application),
        authRepo = authRepository
    )
) : AndroidViewModel(application) {

    private val telemetryManager = TelemetryManager(application)
    val appUpdateManager = AppUpdateManager(application, viewModelScope)
    val soundManager = com.example.audio.SoundManager(application)
    val cinematicController = CinematicController(this)

    private var nextEntityId: Long = 0L
    private fun nextId(prefix: String = "ent_"): String = "$prefix${nextEntityId++}"

    private var isSynergyCacheDirty: Boolean = true
    private var cachedSynergies: List<GlobalAuraSynergy> = emptyList()

    internal fun markSynergyDirty() {
        isSynergyCacheDirty = true
    }

    internal fun clearPendingSpawns() {
        pendingSpawns.clear()
    }

    private val initialConfig = telemetryManager.getBalanceConfig()
    internal val _uiState = MutableStateFlow(
        GameUIState(
            balanceConfig = initialConfig,
            masterVolume = initialConfig.masterVolume,
            musicVolume = initialConfig.musicVolume,
            sfxVolume = initialConfig.sfxVolume,
            upgradeVolume = initialConfig.upgradeVolume,
            isAudioMuted = initialConfig.isMuted,
            musicTrack = runCatching { com.example.audio.MusicTrack.valueOf(initialConfig.musicTrackName) }.getOrDefault(com.example.audio.MusicTrack.ROLLING_PLAYLIST),
            kineticSfxSlot = initialConfig.kineticSfxSlot,
            elementalSfxSlot = initialConfig.elementalSfxSlot,
            siegeSfxSlot = initialConfig.siegeSfxSlot,
            turretSkinSlot = initialConfig.turretSkinSlot,
            mapThemeSlot = initialConfig.mapThemeSlot
        )
    )
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    private var devTunedThisRun: Boolean = false
    private var gameLoopJob: Job? = null
    private var pendingSpawns: MutableList<EnemySpawnEntry> = mutableListOf()
    private var nextSpawnTimeMs: Long = 0L
    private var waveHasStartedSpawning: Boolean = false
    private var updateDismissedForSession: Boolean = false

    // Zero-allocation Uniform Grid Spatial Partitioning (Leo & Dr. Alan)
    val spatialGrid = com.example.engine.SpatialHashGrid(cellSize = 2.0f, gridWidth = 16, gridHeight = 18)

    // Real-Time Telemetry Probes (Tessa)
    var currentFps: Int = 60
        private set
    var frameJitterMs: Float = 0.5f
        private set

    private val frameTimes = LongArray(30)
    private var frameCount = 0

    init {
        remoteConfigManager.initialize()
        telemetryManager.logTutorialStart()
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.ensureAuthenticated()
        }

        val config = _uiState.value.balanceConfig
        soundManager.setMasterVolumeLevel(config.masterVolume)
        soundManager.setMusicVolumeLevel(config.musicVolume)
        soundManager.setSfxVolumeLevel(config.sfxVolume)
        soundManager.setUpgradeVolumeLevel(config.upgradeVolume)
        soundManager.setMutedState(config.isMuted)
        soundManager.activeMusicTrack = _uiState.value.musicTrack

        refreshTelemetry()
        startNewRun()
        checkForAppUpdates()

        viewModelScope.launch {
            leaderboardRepository.initialize()
            leaderboardRepository.fetchLeaderboard(forceRefresh = false)
        }

        viewModelScope.launch {
            leaderboardRepository.boardState.collect { boardState ->
                _uiState.update { prev ->
                    prev.copy(
                        onlineLeaderboard = boardState.entries,
                        leaderboardStatus = boardState.status,
                        leaderboardPendingCount = boardState.pendingCount,
                        leaderboardErrorMessage = when (boardState.status) {
                            GlobalBoardStatus.ERROR -> {
                                if (boardState.pendingCount > 0) {
                                    "Offline Queue: ${boardState.pendingCount} score(s) pending sync"
                                } else {
                                    boardState.failureKind?.name ?: "Connection error"
                                }
                            }
                            GlobalBoardStatus.OFFLINE_CACHED -> if (boardState.entries.isEmpty()) "Connection failed. Scores cached locally." else null
                            GlobalBoardStatus.NOT_CONFIGURED -> "Database Pending: Enable (default) Firestore in Firebase Console"
                            else -> null
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            appUpdateManager.updateState.collect { updateState ->
                _uiState.update { prev ->
                    val shouldOpenModal = if (updateState.status == UpdateDownloadStatus.AVAILABLE && !updateDismissedForSession) true else prev.showUpdateModal
                    prev.copy(
                        updateUIState = updateState,
                        showUpdateModal = shouldOpenModal
                    )
                }
            }
        }
    }

    fun updateDeviceRotation(targetAngle: Float) {
        val normalized = (targetAngle % 360f + 360f) % 360f
        if (_uiState.value.deviceRotationAngle != normalized || _uiState.value.hardwareTiltAngle != normalized || _uiState.value.isSystemAutoRotateLocked) {
            _uiState.update {
                it.copy(
                    deviceRotationAngle = normalized,
                    hardwareTiltAngle = normalized,
                    isSystemAutoRotateLocked = false
                )
            }
        }
    }

    fun updateHardwareTiltAngle(targetAngle: Float) {
        val normalized = (targetAngle % 360f + 360f) % 360f
        if (_uiState.value.hardwareTiltAngle != normalized || !_uiState.value.isSystemAutoRotateLocked) {
            _uiState.update {
                it.copy(
                    hardwareTiltAngle = normalized,
                    isSystemAutoRotateLocked = true
                )
            }
        }
    }

    fun confirmManualRotation() {
        val target = _uiState.value.hardwareTiltAngle
        _uiState.update { it.copy(deviceRotationAngle = target) }
    }

    fun fetchOnlineLeaderboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            leaderboardRepository.fetchLeaderboard(forceRefresh = forceRefresh)
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates()
        }
    }

    fun downloadAndInstallUpdate() {
        val state = _uiState.value.updateUIState
        if (state.status == UpdateDownloadStatus.DOWNLOADING || state.status == UpdateDownloadStatus.CHECKING) {
            return
        }
        if (state.status == UpdateDownloadStatus.READY_TO_INSTALL && state.downloadedApkFile != null) {
            appUpdateManager.installApk(state.downloadedApkFile)
        } else {
            val info = state.updateInfo ?: return
            appUpdateManager.startDownload(info)
        }
    }

    fun dismissUpdateModal() {
        updateDismissedForSession = true
        _uiState.update { it.copy(showUpdateModal = false) }
    }

    fun openUpdateModal() {
        _uiState.update { it.copy(showUpdateModal = true) }
    }

    override fun onCleared() {
        super.onCleared()
        appUpdateManager.unregisterReceiver()
        soundManager.release()
    }

    fun refreshTelemetry() {
        val activeName = telemetryManager.getActivePlayerName()
        _uiState.update {
            it.copy(
                activePlayerName = activeName,
                lastRunRecord = telemetryManager.getLastRun(),
                playerLifetimeTelemetry = telemetryManager.getLifetimeTelemetryForPlayer(activeName),
                leaderboard = telemetryManager.getLeaderboard(),
                lifetimeTelemetry = telemetryManager.getLifetimeTelemetry(),
                towerPerformance = telemetryManager.getTowerPerformance()
            )
        }
    }

    fun toggleTelemetryView(open: Boolean) {
        if (open) {
            refreshTelemetry()
            fetchOnlineLeaderboard(forceRefresh = true)
        }
        _uiState.update { it.copy(isTelemetryOpen = open) }
    }

    fun toggleSettingsView(open: Boolean) {
        if (open) refreshTelemetry()
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun openTelemetryFromPauseMenu() {
        refreshTelemetry()
        fetchOnlineLeaderboard(forceRefresh = true)
        _uiState.update { it.copy(isPauseMenuOpen = false, isTelemetryOpen = true) }
    }

    fun openSettingsFromPauseMenu() {
        refreshTelemetry()
        _uiState.update { it.copy(isPauseMenuOpen = false, isSettingsOpen = true) }
    }

    fun isInternalDialogActive(): Boolean {
        val s = _uiState.value
        return s.isPauseMenuOpen || s.isSettingsOpen || s.isTelemetryOpen || s.showEndRunConfirmDialog || s.showUpdateModal
    }

    fun openPauseMenu() {
        soundManager.isPaused = true
        val currentSpeed = _uiState.value.gameSpeed
        val savedSpeed = if (currentSpeed != GameSpeed.PAUSED) currentSpeed else _uiState.value.prePauseSpeed
        _uiState.update {
            it.copy(
                isPauseMenuOpen = true,
                prePauseSpeed = savedSpeed,
                gameSpeed = GameSpeed.PAUSED
            )
        }
    }

    fun closePauseMenu() {
        soundManager.isPaused = false
        val resumeSpeed = if (_uiState.value.prePauseSpeed != GameSpeed.PAUSED) _uiState.value.prePauseSpeed else GameSpeed.NORMAL
        _uiState.update {
            it.copy(
                isPauseMenuOpen = false,
                gameSpeed = resumeSpeed
            )
        }
    }

    fun requestEndRun() {
        soundManager.isPaused = true
        val currentSpeed = _uiState.value.gameSpeed
        val savedSpeed = if (currentSpeed != GameSpeed.PAUSED) currentSpeed else _uiState.value.prePauseSpeed
        _uiState.update {
            it.copy(
                showEndRunConfirmDialog = true,
                prePauseSpeed = savedSpeed,
                gameSpeed = GameSpeed.PAUSED
            )
        }
    }

    fun dismissEndRunConfirm() {
        if (!_uiState.value.isPauseMenuOpen) {
            soundManager.isPaused = false
        }
        val resumeSpeed = if (!_uiState.value.isPauseMenuOpen && _uiState.value.prePauseSpeed != GameSpeed.PAUSED) _uiState.value.prePauseSpeed else GameSpeed.PAUSED
        _uiState.update {
            it.copy(
                showEndRunConfirmDialog = false,
                gameSpeed = resumeSpeed
            )
        }
    }

    fun confirmEndRun() {
        soundManager.isPaused = false
        val state = _uiState.value
        val wavesCompleted = if (state.isWaveActive) maxOf(0, state.waveNumber - 1) else state.waveNumber
        recordRunCompletion(isVictory = false, waves = wavesCompleted)
        _uiState.update { current ->
            current.copy(
                showEndRunConfirmDialog = false,
                isPauseMenuOpen = false,
                isGameOver = true,
                isVictory = false,
                isWaveActive = false,
                gameSpeed = GameSpeed.PAUSED
            )
        }
    }

    fun dismissFirstTileHint() {
        telemetryManager.logTutorialComplete()
        _uiState.update { it.copy(hasClickedFirstTile = true) }
    }

    fun updateBalanceConfig(newConfig: DevBalanceConfig) {
        if (newConfig.isGameplayModified()) {
            devTunedThisRun = true
        }
        telemetryManager.saveBalanceConfig(newConfig)
        soundManager.setMasterVolumeLevel(newConfig.masterVolume)
        soundManager.setMusicVolumeLevel(newConfig.musicVolume)
        soundManager.setSfxVolumeLevel(newConfig.sfxVolume)
        soundManager.setUpgradeVolumeLevel(newConfig.upgradeVolume)
        soundManager.setMutedState(newConfig.isMuted)
        _uiState.update { it.copy(balanceConfig = newConfig) }
    }

    fun resetBalanceConfigToDefaults() {
        val defaultConfig = DevBalanceConfig()
        telemetryManager.saveBalanceConfig(defaultConfig)
        soundManager.setMasterVolumeLevel(defaultConfig.masterVolume)
        soundManager.setMusicVolumeLevel(defaultConfig.musicVolume)
        soundManager.setSfxVolumeLevel(defaultConfig.sfxVolume)
        soundManager.setUpgradeVolumeLevel(defaultConfig.upgradeVolume)
        soundManager.setMutedState(defaultConfig.isMuted)
        _uiState.update {
            it.copy(
                balanceConfig = defaultConfig,
                masterVolume = defaultConfig.masterVolume,
                musicVolume = defaultConfig.musicVolume,
                sfxVolume = defaultConfig.sfxVolume,
                upgradeVolume = defaultConfig.upgradeVolume,
                isAudioMuted = defaultConfig.isMuted,
                kineticSfxSlot = defaultConfig.kineticSfxSlot,
                elementalSfxSlot = defaultConfig.elementalSfxSlot,
                siegeSfxSlot = defaultConfig.siegeSfxSlot,
                turretSkinSlot = defaultConfig.turretSkinSlot,
                mapThemeSlot = defaultConfig.mapThemeSlot
            )
        }
    }

    fun resetTowerPerformance() {
        telemetryManager.resetTowerPerformance()
        _uiState.update { it.copy(towerPerformance = telemetryManager.getTowerPerformance()) }
    }

    /**
     * Spawns a custom test formation into the active map lanes (Tessa's Sandbox).
     */
    fun spawnSandboxWave(enemyType: EnemyType, count: Int) {
        val level = _uiState.value.level ?: return
        val baseBal = _uiState.value.balanceConfig.getEnemyBalance(enemyType)
        val newEnemies = mutableListOf<EnemyInstance>()
        for (i in 0 until count) {
            val laneIndex = i % 2
            val path = if (laneIndex == 0) level.laneAlpha else level.laneBeta
            val start = path.first()
            val newEnemy = EnemyInstance(
                id = nextId("sb_"),
                type = enemyType,
                laneIndex = laneIndex,
                currentPathSegmentIndex = 0,
                segmentProgress = -(i * 0.35f),
                maxHp = baseBal.hp,
                currentHp = baseBal.hp,
                speed = baseBal.speed,
                worldX = start.x.toFloat(),
                worldY = start.y.toFloat(),
                isBoss = (enemyType == EnemyType.BOSS)
            )
            newEnemies.add(newEnemy)
        }
        _uiState.update { current ->
            current.copy(
                enemies = current.enemies + newEnemies,
                isWaveActive = true
            )
        }
    }

    fun toggleSynergyPanel() {
        _uiState.update { it.copy(isSynergyPanelExpanded = !it.isSynergyPanelExpanded) }
    }

    fun setSoundProfile(profile: com.example.audio.SoundProfile) {
        soundManager.activeProfile = profile
        _uiState.update { it.copy(soundProfile = profile) }
    }

    fun setMusicTrack(track: com.example.audio.MusicTrack) {
        soundManager.activeMusicTrack = track
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(musicTrackName = track.name)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(musicTrack = track, balanceConfig = updated)
        }
    }

    fun setMasterVolume(level: Float) {
        soundManager.setMasterVolumeLevel(level)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(masterVolume = level)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(masterVolume = level, balanceConfig = updated)
        }
    }

    fun setSfxVolume(level: Float) {
        soundManager.setSfxVolumeLevel(level)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(sfxVolume = level)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(sfxVolume = level, balanceConfig = updated)
        }
    }

    fun setUpgradeVolume(level: Float) {
        soundManager.setUpgradeVolumeLevel(level)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(upgradeVolume = level)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(upgradeVolume = level, balanceConfig = updated)
        }
    }

    fun setMusicVolume(level: Float) {
        soundManager.setMusicVolumeLevel(level)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(musicVolume = level)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(musicVolume = level, balanceConfig = updated)
        }
    }

    fun toggleAudioMute(): Boolean {
        val newMuted = soundManager.toggleMute()
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(isMuted = newMuted)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(isAudioMuted = newMuted, balanceConfig = updated)
        }
        return newMuted
    }

    fun setAudioMuted(muted: Boolean) {
        soundManager.setMutedState(muted)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(isMuted = muted)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(isAudioMuted = muted, balanceConfig = updated)
        }
    }

    fun setKineticSfxSlot(slot: Int) {
        val clamped = slot.coerceIn(1, 5)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(kineticSfxSlot = clamped)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(kineticSfxSlot = clamped, balanceConfig = updated)
        }
    }

    fun setElementalSfxSlot(slot: Int) {
        val clamped = slot.coerceIn(1, 5)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(elementalSfxSlot = clamped)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(elementalSfxSlot = clamped, balanceConfig = updated)
        }
    }

    fun setSiegeSfxSlot(slot: Int) {
        val clamped = slot.coerceIn(1, 5)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(siegeSfxSlot = clamped)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(siegeSfxSlot = clamped, balanceConfig = updated)
        }
    }

    fun setTurretSkinSlot(slot: Int) {
        val clamped = slot.coerceIn(1, 5)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(turretSkinSlot = clamped)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(turretSkinSlot = clamped, balanceConfig = updated)
        }
    }

    fun setMapThemeSlot(slot: Int) {
        val clamped = slot.coerceIn(1, 5)
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(mapThemeSlot = clamped)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(mapThemeSlot = clamped, balanceConfig = updated)
        }
    }

    fun playTurretTestSound(towerClass: TowerClass, slot: Int) {
        soundManager.playTurretTestSound(towerClass, slot)
    }

    fun playTowerUpgradeTestSound(towerClass: TowerClass, slot: Int) {
        soundManager.playTowerUpgradeTestSound(towerClass, slot)
    }

    fun setTowerVisualSkin(skin: String) {
        _uiState.update { it.copy(towerVisualSkin = skin) }
    }

    fun playTestSound(fx: com.example.audio.SoundFx) {
        soundManager.play(fx)
    }

    fun startNewRun() {
        virtualSimTimeMs = 0L
        devTunedThisRun = false
        isSynergyCacheDirty = true
        cachedSynergies = emptyList()
        gameLoopJob?.cancel()

        val seed = System.currentTimeMillis()
        val balance = _uiState.value.balanceConfig
        val level = MapGenerator.generate(
            seed = seed,
            minLaneLength = balance.minLaneLength,
            maxLaneLength = balance.maxLaneLength
        )
        val firstWave = WaveGenerator.generateWave(1, balance.enemiesPerRoundMultiplier, _uiState.value.difficulty)

        val initialParticles = mutableListOf<TrackParticle>()
        for (i in 0 until 12) {
            val lane = i % 2
            val path = if (lane == 0) level.laneAlpha else level.laneBeta
            val maxSeg = (path.size - 2).coerceAtLeast(0)
            initialParticles.add(
                TrackParticle(
                    id = "tp_$i",
                    laneIndex = lane,
                    segmentIndex = (i * 2) % (maxSeg + 1),
                    progress = (i * 0.15f) % 1.0f,
                    speed = 0.8f + (i % 3) * 0.2f,
                    color = if (lane == 0) 0xFF00F0FF else 0xFFFFB800,
                    size = 2.5f + (i % 2) * 1.5f
                )
            )
        }

        _uiState.update { prev ->
            prev.copy(
                seed = seed,
                level = level,
                towers = emptyList(),
                enemies = emptyList(),
                projectiles = emptyList(),
                laserBeams = emptyList(),
                lightningArcs = emptyList(),
                explosions = emptyList(),
                floatingTexts = emptyList(),
                trackParticles = initialParticles,
                activeGlobalSynergies = emptyList(),
                isSynergyPanelExpanded = false,
                animTimeMs = 0L,
                globalKineticSpeedBonus = 0f,
                globalFrostSlowBonus = 0f,
                globalTeslaArcsEnabled = false,
                globalMortarSplashBonus = 0f,
                globalSolarCritsEnabled = false,
                globalVoidShredEnabled = false,
                globalDamageBonus = 1.0f,
                scrapBountyBonus = 1.0f,
                coreHp = 30,
                maxCoreHp = 30,
                scrap = prev.balanceConfig.startingScrap,
                waveNumber = 1,
                currentWaveData = firstWave,
                isWaveActive = false,
                gameSpeed = GameSpeed.NORMAL,
                selectedTile = null,
                selectedTower = null,
                isGameOver = false,
                isVictory = false,
                totalKills = 0,
                totalScrapEarned = prev.balanceConfig.startingScrap,
                totalDamageDealt = 0f,
                screenShakeIntensity = 0f,
                autoCallNextWave = false,
                overchargeRemainingMs = 0L,
                isTelemetryOpen = false,
                isSettingsOpen = false,
                isPauseMenuOpen = false,
                hasClickedFirstTile = false
            )
        }

        pendingSpawns.clear()
        waveHasStartedSpawning = false
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val deltaMs = (now - lastTime).coerceAtMost(50L)
                lastTime = now

                // Telemetry tracking: FPS & Frame-time Jitter (Tessa)
                val idx = frameCount % 30
                frameTimes[idx] = deltaMs
                frameCount++
                if (frameCount % 15 == 0) {
                    val count = minOf(frameCount, 30)
                    var sum = 0L
                    for (i in 0 until count) sum += frameTimes[i]
                    val avgDelta = sum.toDouble() / count
                    currentFps = if (avgDelta > 0) (1000.0 / avgDelta).toInt().coerceIn(1, 120) else 60
                    var sumSq = 0.0
                    for (i in 0 until count) {
                        val diff = frameTimes[i] - avgDelta
                        sumSq += diff * diff
                    }
                    frameJitterMs = Math.sqrt(sumSq / count).toFloat()
                }

                val speed = _uiState.value.gameSpeed.multiplier
                val simDeltaSec = (deltaMs / 1000f) * (if (speed > 0f) speed else 1.0f)
                if (speed > 0f && !_uiState.value.isGameOver && !isInternalDialogActive()) {
                    tickSimulation(simDeltaSec, (deltaMs * speed).toLong(), now)
                } else {
                    _uiState.update { it.copy(animTimeMs = now) }
                }

                delay(16) // ~60 FPS
            }
        }
    }

    private var virtualSimTimeMs: Long = 0L

    private fun tickSimulation(deltaSec: Float, deltaMs: Long, nowMs: Long = System.currentTimeMillis()) {
        virtualSimTimeMs += deltaMs
        soundManager.updateTick(nowMs, deltaMs)
        val state = _uiState.value
        val level = state.level ?: return

        // Deterministic Scripted Cinematic Controller (Track 4)
        if (state.activeCinematic != null) {
            if (!cinematicController.processFrame(state.activeCinematic, state)) {
                return
            }
        }

        val newOvercharge = maxOf(0L, state.overchargeRemainingMs - deltaMs)
        val currentEnemies = state.enemies.toMutableList()

        // 0. Advance Track Energy Particles along the vector curves
        val updatedParticles = state.trackParticles.map { p ->
            val path = if (p.laneIndex == 0) level.laneAlpha else level.laneBeta
            var seg = p.segmentIndex
            var prog = p.progress + p.speed * deltaSec
            while (prog >= 1.0f && seg < path.size - 2) {
                prog -= 1.0f
                seg++
            }
            if (seg >= path.size - 2 && prog >= 1.0f) {
                seg = 0
                prog = 0f
            }
            p.copy(segmentIndex = seg, progress = prog)
        }

        // 1. Dual-Lane Wave Spawning
        if (state.isWaveActive && pendingSpawns.isNotEmpty()) {
            nextSpawnTimeMs -= deltaMs
            if (nextSpawnTimeMs <= 0) {
                val spawn = pendingSpawns.removeAt(0)
                waveHasStartedSpawning = true

                val path = if (spawn.laneIndex == 0) level.laneAlpha else level.laneBeta
                val start = path.first()
                val customEnemyBal = state.balanceConfig.getEnemyBalance(spawn.type)
                val bossMul = if (spawn.isBoss) remoteConfigManager.bossHealthMultiplier else 1.0f
                val hpMultiplier = spawn.hpScale * state.balanceConfig.enemyHpMultiplier * remoteConfigManager.waveHpMultiplier * bossMul
                val effectiveMaxHp = customEnemyBal.hp * hpMultiplier
                val newEnemy = EnemyInstance(
                    id = nextId("en_"),
                    type = spawn.type,
                    laneIndex = spawn.laneIndex,
                    currentPathSegmentIndex = 0,
                    segmentProgress = 0f,
                    maxHp = effectiveMaxHp,
                    currentHp = effectiveMaxHp,
                    speed = customEnemyBal.speed * spawn.speedScale,
                    worldX = start.x.toFloat(),
                    worldY = start.y.toFloat(),
                    isBoss = spawn.isBoss
                )
                currentEnemies.add(newEnemy)

                nextSpawnTimeMs = if (pendingSpawns.isNotEmpty()) pendingSpawns.first().delayMs else 0L
            }
        }

        // 2. Dual-Lane Movement & Status Effects
        val survivingEnemies = mutableListOf<EnemyInstance>()
        var coreDamageTaken = 0

        for (enemy in currentEnemies) {
            if (enemy.slowTimerMs > 0) enemy.slowTimerMs = maxOf(0L, enemy.slowTimerMs - deltaMs)
            if (enemy.shockTimerMs > 0) enemy.shockTimerMs = maxOf(0L, enemy.shockTimerMs - deltaMs)

            val path = if (enemy.laneIndex == 0) level.laneAlpha else level.laneBeta
            val globalSlow = if (state.globalFrostSlowBonus > 0f) maxOf(0.15f, 1f - state.globalFrostSlowBonus) else 1.0f
            val moveDistance = enemy.effectiveSpeed * state.balanceConfig.enemySpeedMultiplier * globalSlow * deltaSec
            enemy.segmentProgress += moveDistance

            while (enemy.segmentProgress >= 1f && enemy.currentPathSegmentIndex < path.size - 1) {
                enemy.segmentProgress -= 1f
                enemy.currentPathSegmentIndex++
            }

            if (enemy.currentPathSegmentIndex >= path.size - 1) {
                coreDamageTaken += if (enemy.isBoss) 6 else 1
            } else {
                val p0 = path[enemy.currentPathSegmentIndex]
                val p1 = path[enemy.currentPathSegmentIndex + 1]
                val t = enemy.segmentProgress
                enemy.worldX = p0.x + (p1.x - p0.x) * t
                enemy.worldY = p0.y + (p1.y - p0.y) * t
                enemy.headingRad = atan2((p1.y - p0.y).toFloat(), (p1.x - p0.x).toFloat())
                survivingEnemies.add(enemy)
            }
        }

        // Rebuild Spatial Hash Grid for O(1) bucket lookups across all towers & projectiles (Leo & Dr. Alan)
        spatialGrid.rebuild(survivingEnemies)

        // Core Damage & Screen Shake
        var newCoreHp = state.coreHp
        var gameOver = state.isGameOver
        var shake = maxOf(0f, state.screenShakeIntensity - deltaSec * 3f)

        if (coreDamageTaken > 0) {
            newCoreHp = maxOf(0, state.coreHp - coreDamageTaken)
            shake = 1.0f
            val corePos = level.tiles.firstOrNull { it.type == TileType.CORE }?.pos ?: GridPos(level.width / 2, level.height - 1)
            addFloatingText(corePos.x.toFloat(), corePos.y.toFloat(), "-$coreDamageTaken BREACH!", 0xFFFF2A55)
            if (newCoreHp <= 0) {
                gameOver = true
                recordRunCompletion(isVictory = false, waves = state.waveNumber - 1)
            }
        }

        // 3. Dynamic Global Buffs from Active Final Forms
        val towers = state.towers
        val finalFormDefs = towers.mapNotNull {
            if (it.isFinalForm) TowerUpgradeManager.getFinalForm(it.type, it.finalFormCode!!) else null
        }

        var globalFireRateBonus = state.globalKineticSpeedBonus
        var globalRangeBonus = 0f
        var globalDamageBonus = (state.globalDamageBonus - 1.0f)
        var globalSlowBonus = state.globalFrostSlowBonus
        var globalCritRate = if (state.globalSolarCritsEnabled) 0.28f else 0.08f
        var globalCritMul = 2.5f
        var globalArmorIgnore = if (state.globalVoidShredEnabled) 1.0f else 0f
        var globalScrapBonus = state.scrapBountyBonus

        for (ff in finalFormDefs) {
            when {
                ff.code == "A.a" && ff.subName.contains("Hyper-Gatling") -> globalFireRateBonus += 0.18f
                ff.code == "C.a" && ff.subName.contains("Sniper") -> globalRangeBonus += 0.25f
                ff.code == "B.a" && ff.subName.contains("Sabot") -> globalArmorIgnore = maxOf(globalArmorIgnore, 0.25f)
                ff.code == "A.a" && ff.subName.contains("Absolute Zero") -> globalSlowBonus = maxOf(globalSlowBonus, 0.15f)
                ff.code == "C.a" && ff.subName.contains("Blizzard") -> globalSlowBonus = maxOf(globalSlowBonus, 0.25f)
                ff.code == "B.a" && ff.subName.contains("Gigawatt") -> globalDamageBonus += 0.25f
                ff.code == "B.c" && ff.subName.contains("Hailstorm") -> globalCritRate += 0.10f
                ff.code == "B.b" && ff.subName.contains("Aurora") -> globalCritMul += 0.50f
                ff.code == "C.c" && ff.subName.contains("Cryo Bounty") -> globalScrapBonus += 0.30f
                ff.code == "C.c" && ff.subName.contains("Dynamo") -> globalScrapBonus += 0.35f
                ff.code == "C.c" && ff.subName.contains("Void Scrap") -> globalScrapBonus += 0.45f
                ff.code == "B.a" && ff.subName.contains("Sub-Space") -> globalArmorIgnore = 1.0f
                ff.code == "A.a" && ff.subName.contains("Black Hole") -> globalSlowBonus = maxOf(globalSlowBonus, 0.25f)
                ff.code == "C.a" && ff.subName.contains("Solar Flare") -> { globalCritRate += 0.20f; globalCritMul = maxOf(globalCritMul, 2.5f) }
                ff.code == "C.b" && ff.subName.contains("Tachyon Flash") -> globalFireRateBonus += 0.25f
                ff.code == "B.b" && ff.subName.contains("Kaleidoscope") -> globalRangeBonus += 0.20f
                ff.code == "A.c" && ff.subName.contains("Carpet Bomber") -> globalFireRateBonus += 0.20f
                ff.code == "C.a" && ff.subName.contains("Orbital Kinetic") -> globalRangeBonus += 0.35f
            }
        }

        val newProjectiles = state.projectiles.toMutableList()
        val newBeams = state.laserBeams.toMutableList()
        val newArcs = state.lightningArcs.toMutableList()
        val newExplosions = state.explosions.toMutableList()
        val simNowMs = virtualSimTimeMs

        val boostedTiles = level.tiles.filter { it.buff != TileBuff.NONE }
        val harmonicCount = towers.count { t -> boostedTiles.any { it.pos == t.gridPos } }
        val isHarmonicActive = harmonicCount >= 6 && boostedTiles.isNotEmpty()
        val harmonicDmgBonus = if (isHarmonicActive) remoteConfigManager.harmonicGridDamageBoost else 0.0f
        val harmonicIncomeMul = if (isHarmonicActive) 1.25f else 1.0f

        for (tower in towers) {
            val tile = level.tiles.firstOrNull { it.pos == tower.gridPos }
            val tileBuff = tile?.buff ?: TileBuff.NONE
            val p0 = tower.pathTiers[0]
            val p1 = tower.pathTiers[1]
            val p2 = tower.pathTiers[2]

            val t1Def = if (tower.branchTier1 != null) TowerUpgradeManager.getTier1Options(tower.type).firstOrNull { it.branchKey == tower.branchTier1 } else null
            val ffDef = if (tower.finalFormCode != null) TowerUpgradeManager.getFinalForm(tower.type, tower.finalFormCode!!) else null

            val legacyDmgMul = ffDef?.damageMultiplier ?: t1Def?.damageMultiplier ?: 1.0f
            val legacySpdMul = ffDef?.fireRateMultiplier ?: t1Def?.fireRateMultiplier ?: 1.0f

            val tierSpdMultiplier = maxOf(legacySpdMul, when (tower.tier) {
                3 -> 1.70f // +70% attack speed at Apex
                2 -> 1.40f // +40% attack speed at Archetype
                1 -> 1.20f // +20% attack speed at Vector
                else -> 1.0f
            })
            val tierDmgMultiplier = maxOf(legacyDmgMul, when (tower.tier) {
                3 -> 1.60f // +60% damage at Apex
                2 -> 1.35f // +35% damage at Archetype
                1 -> 1.15f // +15% damage at Vector
                else -> 1.0f
            })

            val isVectorSynergy = (tower.vector != null && tower.vector == tileBuff.vectorCategory)
            val synergyDmgBonus = if (isVectorSynergy) 0.15f else 0.0f
            val synergySpdBonus = if (isVectorSynergy) 0.15f else 0.0f

            val customTConfig = state.balanceConfig.getEffectiveTowerBalance(tower.type, tower.branchTier1, tower.finalFormCode)
            val effectiveRange = tower.getEffectiveCombatRange(
                tileBuff = tileBuff,
                globalRangeBonus = globalRangeBonus,
                balanceRange = customTConfig.range
            )

            val tileSpdBonus = if (tileBuff == TileBuff.CONDUIT) 0.35f else 0.0f
            val overchargeSpd = if (newOvercharge > 0) 1.0f else 0.0f
            val fireRate = customTConfig.fireRate * state.balanceConfig.globalTowerFireRateMultiplier * tierSpdMultiplier * (1.0f + tileSpdBonus + synergySpdBonus + globalFireRateBonus + overchargeSpd)
            val calculatedCooldown = if (fireRate > 0f) (1000f / fireRate).toLong() else 1000L
            val cooldownMs = maxOf(50L, calculatedCooldown)

            val towerX = tower.gridPos.x.toFloat()
            val towerY = tower.gridPos.y.toFloat()

            // Targets in range across BOTH lanes! (Optimized via SpatialHashGrid)
            // Euclidean distance check matching exact indicated circular perimeter: dist <= effectiveRange + enemy.type.hitRadius
            val inRangeEnemies = spatialGrid.queryNearby(towerX, towerY, effectiveRange + 0.6f).filter { enemy ->
                val dist = hypot(enemy.worldX - tower.gridPos.x, enemy.worldY - tower.gridPos.y)
                dist <= (effectiveRange + enemy.type.hitRadius)
            }

            if (inRangeEnemies.isEmpty()) {
                tower.targetEnemyId = null
                continue
            }

            val target = when (tower.targetPriority) {
                TargetPriority.FIRST -> inRangeEnemies.maxByOrNull { it.currentPathSegmentIndex + it.segmentProgress }
                TargetPriority.CLOSEST -> inRangeEnemies.minByOrNull { hypot(it.worldX - towerX, it.worldY - towerY) }
                TargetPriority.STRONGEST -> inRangeEnemies.maxByOrNull { it.currentHp }
                TargetPriority.WEAKEST -> inRangeEnemies.minByOrNull { it.currentHp }
            } ?: inRangeEnemies.first()

            tower.targetEnemyId = target.id
            tower.rotationAngleDeg = (atan2(target.worldY - towerY, target.worldX - towerX) * (180f / PI.toFloat()))

            if (simNowMs - tower.lastShotTimeMs >= cooldownMs) {
                tower.lastShotTimeMs = simNowMs

                // Trigger Turret Gunfire Audio (TASK 05 & TASK 06)
                val sfxSlot = when (tower.towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> state.kineticSfxSlot
                    TowerClass.ELEMENTAL_VOID -> state.elementalSfxSlot
                    TowerClass.HEAVY_SIEGE -> state.siegeSfxSlot
                    TowerClass.STELLAR_COSMIC -> 1
                }
                soundManager.playTurretFire(tower.towerClass, sfxSlot)

                val tileDmgBonus = if (tileBuff == TileBuff.AMPLIFIER) 0.30f else 0.0f
                val apexBonus = if (tower.isFinalForm) 0.30f else 0.0f
                val classDmgMul = when (tower.towerClass) {
                    TowerClass.KINETIC_BALLISTIC -> state.balanceConfig.kineticDamageMultiplier
                    TowerClass.ELEMENTAL_VOID -> state.balanceConfig.elementalDamageMultiplier
                    TowerClass.HEAVY_SIEGE -> state.balanceConfig.heavyDamageMultiplier
                    TowerClass.STELLAR_COSMIC -> 1.15f
                }
                val baseDmg = customTConfig.damage * tierDmgMultiplier * classDmgMul * (1.0f + tileDmgBonus + synergyDmgBonus + harmonicDmgBonus + globalDamageBonus + apexBonus)
                val isCrit = Random.nextFloat() < globalCritRate
                val finalDmg = if (isCrit) baseDmg * globalCritMul else baseDmg

                when (tower.type) {
                    TowerType.GATLING -> {
                        newProjectiles.add(
                            Projectile(
                                id = nextId("p_"),
                                startX = towerX,
                                startY = towerY,
                                currentX = towerX,
                                currentY = towerY,
                                targetEnemyId = target.id,
                                targetX = target.worldX,
                                targetY = target.worldY,
                                speed = 8.5f,
                                damage = finalDmg,
                                color = tower.type.color,
                                isCrit = isCrit,
                                hasClusterSplit = (p2 >= 2), // Ricochet
                                sourceShape = tower.shape,
                                sourceAffinity = tower.affinity
                            )
                        )
                        // Twin minigun tracer if Path 0 Tier 2
                        if (p0 >= 2) {
                            newProjectiles.add(
                                Projectile(
                                    id = nextId("p_"),
                                    startX = towerX + 0.1f,
                                    startY = towerY + 0.1f,
                                    currentX = towerX + 0.1f,
                                    currentY = towerY + 0.1f,
                                    targetEnemyId = target.id,
                                    targetX = target.worldX,
                                    targetY = target.worldY,
                                    speed = 8.5f,
                                    damage = finalDmg * 0.75f,
                                    color = 0xFFFFFFFF,
                                    isCrit = isCrit,
                                    sourceShape = tower.shape,
                                    sourceAffinity = tower.affinity
                                )
                            )
                        }
                    }
                    TowerType.CRYO -> {
                        newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, target.worldX, target.worldY, tower.type.color))
                        val slowRate = if (p0 >= 1) 0.65f else 0.50f
                        target.slowTimerMs = if (p0 >= 2) 2200L else 1400L
                        val rawSlow = if (p0 >= 2) 0.85f else slowRate
                        target.slowFactor = maxOf(0.15f, 1f - rawSlow)
                        applyDamageToEnemy(target, finalDmg, tower, isCrit, armorPierce = false)
                    }
                    TowerType.TESLA -> {
                        val maxChains = 3 + (if (p0 == 1) 2 else if (p0 >= 2) 5 else 0)
                        val chainTargets = mutableListOf(target)
                        val remaining = survivingEnemies.filter { it.id != target.id }.toMutableList()

                        while (chainTargets.size < maxChains && remaining.isNotEmpty()) {
                            val last = chainTargets.last()
                            val next = remaining.minByOrNull { hypot(it.worldX - last.worldX, it.worldY - last.worldY) }
                            if (next != null && hypot(next.worldX - last.worldX, next.worldY - last.worldY) < 3.4f) {
                                chainTargets.add(next)
                                remaining.remove(next)
                            } else {
                                break
                            }
                        }

                        val arcPoints = mutableListOf<Pair<Float, Float>>()
                        arcPoints.add(towerX to towerY)
                        for (ct in chainTargets) {
                            arcPoints.add(ct.worldX to ct.worldY)
                            ct.shockTimerMs = 2500L
                            applyDamageToEnemy(ct, finalDmg, tower, isCrit, armorPierce = false)
                        }
                        newArcs.add(LightningArc(nextId("arc_"), arcPoints, tower.type.color))
                    }
                    TowerType.MORTAR -> {
                        val dist = hypot(target.worldX - towerX, target.worldY - towerY)
                        val splashRad = tower.type.splashRadius * (if (p1 >= 2) 1.6f else 1.0f)
                        newProjectiles.add(
                            Projectile(
                                id = nextId("p_"),
                                startX = towerX,
                                startY = towerY,
                                currentX = towerX,
                                currentY = towerY,
                                targetEnemyId = null,
                                targetX = target.worldX,
                                targetY = target.worldY,
                                speed = 4.2f,
                                damage = finalDmg,
                                color = tower.type.color,
                                isSplash = true,
                                splashRadius = splashRad,
                                isMortarArc = true,
                                totalDistance = maxOf(0.1f, dist),
                                isCrit = isCrit,
                                hasClusterSplit = (p0 >= 1),
                                sourceShape = tower.shape,
                                sourceAffinity = tower.affinity
                            )
                        )
                    }
                    TowerType.LASER -> {
                        newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, target.worldX, target.worldY, tower.type.color))
                        if (p1 >= 2 && inRangeEnemies.size >= 2) {
                            // Split dual beam
                            val secondTarget = inRangeEnemies.first { it.id != target.id }
                            newBeams.add(LaserBeam(nextId("beam_"), towerX, towerY, secondTarget.worldX, secondTarget.worldY, 0xFFFF7A00))
                            applyDamageToEnemy(secondTarget, finalDmg * 0.75f, tower, isCrit, armorPierce = true)
                        }
                        applyDamageToEnemy(target, finalDmg, tower, isCrit, armorPierce = true)
                    }
                    TowerType.VOID -> {
                        newExplosions.add(ExplosionEffect(nextId("exp_"), towerX, towerY, tower.type.splashRadius, color = tower.type.color))
                        for (enemy in inRangeEnemies) {
                            applyDamageToEnemy(enemy, finalDmg, tower, isCrit, armorPierce = true)
                            if (p0 >= 1) {
                                enemy.segmentProgress = maxOf(0f, enemy.segmentProgress - (if (p0 >= 2) 0.16f else 0.08f))
                            }
                        }
                    }
                    TowerType.STELLAR -> {
                        newProjectiles.add(
                            Projectile(
                                id = nextId("p_"),
                                startX = towerX,
                                startY = towerY,
                                currentX = towerX,
                                currentY = towerY,
                                targetEnemyId = target.id,
                                targetX = target.worldX,
                                targetY = target.worldY,
                                speed = 6.0f,
                                damage = finalDmg,
                                isCrit = isCrit,
                                color = 0xFFFFD700,
                                sourceShape = EnemyShape.STAR,
                                sourceAffinity = ElementAffinity.YELLOW
                            )
                        )
                    }
                }
            }
        }

        // 4. Update Projectiles & Splitting
        val survivingProjectiles = mutableListOf<Projectile>()
        val enemyMap = survivingEnemies.associateBy { it.id }

        for (proj in newProjectiles) {
            val targetEnemy = if (proj.targetEnemyId != null) enemyMap[proj.targetEnemyId] else null
            val destX = targetEnemy?.worldX ?: proj.targetX
            val destY = targetEnemy?.worldY ?: proj.targetY

            val dx = destX - proj.currentX
            val dy = destY - proj.currentY
            val dist = hypot(dx, dy)
            val step = proj.speed * deltaSec

            if (dist <= step || dist < 0.25f) {
                if (proj.isSplash) {
                    newExplosions.add(ExplosionEffect(nextId("exp_"), destX, destY, proj.splashRadius, color = proj.color))
                    val splashRadSq = proj.splashRadius * proj.splashRadius
                    val splashTargets = survivingEnemies.filter {
                        val ex = it.worldX - destX
                        val ey = it.worldY - destY
                        (ex * ex + ey * ey) <= splashRadSq
                    }
                    for (st in splashTargets) {
                        applyDamageToEnemy(st, proj.damage, null, proj.isCrit, armorPierce = false, sourceShape = proj.sourceShape, sourceAffinity = proj.sourceAffinity)
                    }

                    // Mortar Cluster Munitions
                    if (proj.hasClusterSplit) {
                        for (i in 0 until 3) {
                            val angle = (i * 2.094f) + Random.nextFloat()
                            val offsetX = cos(angle) * 0.85f
                            val offsetY = sin(angle) * 0.85f
                            newExplosions.add(ExplosionEffect(nextId("exp_"), destX + offsetX, destY + offsetY, 0.9f, color = 0xFFFFB800, lifeMs = 200L))
                            val clusterRadSq = 0.9f * 0.9f
                            val clusterTargets = survivingEnemies.filter {
                                val ex = it.worldX - (destX + offsetX)
                                val ey = it.worldY - (destY + offsetY)
                                (ex * ex + ey * ey) <= clusterRadSq
                            }
                            for (ct in clusterTargets) {
                                applyDamageToEnemy(ct, proj.damage * 0.45f, null, false, armorPierce = false, sourceShape = proj.sourceShape, sourceAffinity = proj.sourceAffinity)
                            }
                        }
                    }
                } else {
                    val hitEnemy = if (proj.targetEnemyId != null) enemyMap[proj.targetEnemyId] else null
                    if (hitEnemy != null) {
                        applyDamageToEnemy(hitEnemy, proj.damage, null, proj.isCrit, armorPierce = false, sourceShape = proj.sourceShape, sourceAffinity = proj.sourceAffinity)
                        // Gatling Ricochet
                        if (proj.hasClusterSplit) {
                            val bounceTarget = survivingEnemies.firstOrNull {
                                val bx = it.worldX - hitEnemy.worldX
                                val by = it.worldY - hitEnemy.worldY
                                it.id != hitEnemy.id && (bx * bx + by * by) < 6.25f
                            }
                            if (bounceTarget != null) {
                                applyDamageToEnemy(bounceTarget, proj.damage * 0.50f, null, false, armorPierce = false, sourceShape = proj.sourceShape, sourceAffinity = proj.sourceAffinity)
                            }
                        }
                    }
                }
            } else {
                proj.currentX += (dx / dist) * step
                proj.currentY += (dy / dist) * step
                proj.traveledDistance += step
                survivingProjectiles.add(proj)
            }
        }

        // 5. Harvest dead hostiles
        var killsGained = 0
        var scrapGained = 0
        val remainingAlive = mutableListOf<EnemyInstance>()

        val conduitTilesCount = towers.count { t ->
            val tile = level.tiles.firstOrNull { it.pos == t.gridPos }
            tile?.buff == TileBuff.CONDUIT
        }
        val conduitIncomeBonus = 1.0f + (conduitTilesCount * 0.15f)
        val padEconomyBonus = 1.0f + (harmonicCount * 0.03f)

        for (enemy in survivingEnemies) {
            if (enemy.isDead) {
                killsGained++
                val bounty = (enemy.type.scrapValue * state.scrapBountyBonus * state.balanceConfig.globalIncomeMultiplier * remoteConfigManager.scrapBountyMultiplier * conduitIncomeBonus * harmonicIncomeMul * padEconomyBonus).roundToInt().coerceAtLeast(1)
                scrapGained += bounty

                addFloatingText(enemy.worldX, enemy.worldY, "+$bounty", 0xFFFFD700)

                // Cryo Shatterburst
                if (enemy.slowTimerMs > 0 && state.globalFrostSlowBonus > 0f) {
                    newExplosions.add(ExplosionEffect(nextId("exp_"), enemy.worldX, enemy.worldY, 1.4f, color = 0xFF38BDF8))
                    val shatterRadSq = 1.4f * 1.4f
                    val shatterTargets = survivingEnemies.filter {
                        val sx = it.worldX - enemy.worldX
                        val sy = it.worldY - enemy.worldY
                        it != enemy && (sx * sx + sy * sy) <= shatterRadSq
                    }
                    for (st in shatterTargets) {
                        applyDamageToEnemy(st, enemy.maxHp * 0.25f, null, false, armorPierce = true)
                    }
                }
            } else {
                remainingAlive.add(enemy)
            }
        }

        // 6. Update Visual Effects
        newBeams.removeAll {
            it.lifeMs -= deltaMs
            it.lifeMs <= 0
        }
        newArcs.removeAll {
            it.lifeMs -= deltaMs
            it.lifeMs <= 0
        }
        newExplosions.removeAll {
            it.lifeMs -= deltaMs
            it.currentRadius = it.maxRadius * (1f - (it.lifeMs.toFloat() / it.maxLifeMs))
            it.lifeMs <= 0
        }

        val updatedFloatingTexts = _uiState.value.floatingTexts.mapNotNull { ft ->
            val remLife = ft.lifeMs - deltaMs
            if (remLife <= 0) null
            else ft.copy(lifeMs = remLife, y = ft.y - (deltaSec * 0.7f))
        }

        // 7. Check Wave Completion
        var waveActive = state.isWaveActive
        var currentWave = state.waveNumber
        var isVictory = state.isVictory

        if (waveActive && waveHasStartedSpawning && pendingSpawns.isEmpty() && remainingAlive.isEmpty()) {
            waveActive = false
            waveHasStartedSpawning = false

            var completionReward = ((state.currentWaveData?.completionBonusScrap ?: 100) * state.balanceConfig.waveBonusMultiplier * state.balanceConfig.globalIncomeMultiplier).roundToInt().coerceAtLeast(10)

            // Mortar C.c: Atomic Shell Battery yields +40% scrap credits at end of wave
            if (towers.any { it.isFinalForm && it.type == TowerType.MORTAR && it.finalFormCode == "C.c" }) {
                completionReward = (completionReward * 1.4f).toInt()
            }
            // Laser C.c: Solar Battery Collector subsidy +50 Credits per wave
            if (towers.any { it.isFinalForm && it.type == TowerType.LASER && it.finalFormCode == "C.c" }) {
                completionReward += 50
            }
            // Gatling C.c: Automated Phalanx regenerates +1 Core HP per wave
            if (towers.any { it.isFinalForm && it.type == TowerType.GATLING && it.finalFormCode == "C.c" }) {
                newCoreHp = minOf(state.maxCoreHp, newCoreHp + 1)
            }

            scrapGained += completionReward
            telemetryManager.logWaveComplete(currentWave, completionReward)

            val corePos = level.tiles.firstOrNull { it.type == TileType.CORE }?.pos ?: GridPos(level.width / 2, level.height - 1)
            val clearMessage = if (currentWave % 5 == 0) "★ Milestone Wave $currentWave Cleared! +$completionReward ★" else "Wave $currentWave Cleared! +$completionReward"
            addFloatingText(corePos.x.toFloat(), corePos.y.toFloat() - 1f, clearMessage, if (currentWave % 5 == 0) 0xFFFFD700 else 0xFF00F0FF)

            if (currentWave >= state.difficulty.maxWave) {
                isVictory = true
                recordRunCompletion(isVictory = true, waves = currentWave)
                addFloatingText(corePos.x.toFloat(), corePos.y.toFloat() - 2f, "👑 ${state.difficulty.label} VICTORY ACHIEVED! 👑", 0xFFFFD700)
            } else {
                currentWave++
            }
        }

        val synergies = if (isSynergyCacheDirty) {
            val s = computeActiveSynergies(towers)
            cachedSynergies = s
            isSynergyCacheDirty = false
            s
        } else {
            cachedSynergies
        }

        _uiState.update { prev ->
            prev.copy(
                enemies = remainingAlive,
                projectiles = survivingProjectiles,
                laserBeams = newBeams,
                lightningArcs = newArcs,
                explosions = newExplosions,
                floatingTexts = updatedFloatingTexts,
                trackParticles = updatedParticles,
                activeGlobalSynergies = synergies,
                harmonicGridActive = isHarmonicActive,
                harmonicPadsCount = harmonicCount,
                globalRangeBonus = globalRangeBonus,
                animTimeMs = nowMs,
                coreHp = newCoreHp,
                scrap = prev.scrap + scrapGained,
                totalScrapEarned = prev.totalScrapEarned + scrapGained,
                totalKills = prev.totalKills + killsGained,
                isGameOver = gameOver,
                isVictory = isVictory,
                isWaveActive = waveActive,
                waveNumber = currentWave,
                screenShakeIntensity = shake,
                overchargeRemainingMs = newOvercharge,
                bestWaveRecord = maxOf(prev.bestWaveRecord, currentWave)
            )
        }

        if (!waveActive && state.autoCallNextWave && !gameOver && !isVictory) {
            deployWave()
        }
    }

    private fun applyDamageToEnemy(
        enemy: EnemyInstance,
        rawDamage: Float,
        sourceTower: TowerInstance?,
        isCrit: Boolean,
        armorPierce: Boolean,
        sourceShape: EnemyShape? = null,
        sourceAffinity: ElementAffinity? = null
    ) {
        val state = _uiState.value
        val armor = if (armorPierce || state.globalVoidShredEnabled) 0f else enemy.type.baseArmor
        val towerShape = sourceTower?.shape ?: sourceShape ?: EnemyShape.CIRCLE
        val towerAffinity = sourceTower?.affinity ?: sourceAffinity ?: ElementAffinity.RED

        val combatResult = DamageCalculator.calculateDamage(
            baseDamage = rawDamage,
            towerShape = towerShape,
            towerAffinity = towerAffinity,
            enemyShape = enemy.type.shape,
            enemyAffinity = enemy.type.elementAffinity,
            enemyArmor = armor
        )
        val finalDamage = combatResult.finalDamage

        enemy.currentHp -= finalDamage
        sourceTower?.let {
            it.totalDamage += finalDamage
            if (enemy.isDead) it.kills++
        }

        _uiState.update { it.copy(totalDamageDealt = it.totalDamageDealt + finalDamage) }

        if (isCrit || (combatResult.isShapeMatch && combatResult.isColorAdvantage) || combatResult.shapeMultiplier < 1.0f || finalDamage > 60f) {
            val tag = when {
                isCrit -> "${finalDamage.toInt()} CRIT!"
                combatResult.isShapeMatch && combatResult.isColorAdvantage -> "${finalDamage.toInt()} WEAK!"
                combatResult.shapeMultiplier < 1.0f -> "${finalDamage.toInt()} RESIST"
                else -> "${finalDamage.toInt()}"
            }
            val textColor = when {
                isCrit -> 0xFFFFB800
                combatResult.isShapeMatch && combatResult.isColorAdvantage -> 0xFF00FF9D
                combatResult.shapeMultiplier < 1.0f -> 0xFF94A3B8
                else -> 0xFFFFFFFF
            }
            addFloatingText(
                x = enemy.worldX,
                y = enemy.worldY - 0.2f,
                text = tag,
                color = textColor,
                isCrit = isCrit
            )
        }
    }

    fun deployWave() {
        val state = _uiState.value
        if (state.isWaveActive || state.isGameOver) return

        telemetryManager.logWaveStart(state.waveNumber)

        val waveData = WaveGenerator.generateWave(
            state.waveNumber,
            state.balanceConfig.enemiesPerRoundMultiplier,
            state.difficulty
        )
        pendingSpawns = waveData.spawnList.toMutableList()
        nextSpawnTimeMs = 300L
        waveHasStartedSpawning = false

        _uiState.update {
            it.copy(
                isWaveActive = true,
                currentWaveData = waveData
            )
        }
    }

    fun selectTile(tile: MapTile?) {
        if (!_uiState.value.hasClickedFirstTile && tile != null) {
            telemetryManager.logTutorialComplete()
        }
        // Reselecting currently active tile or tower toggles the selection off (dismisses upgrade/build dock)
        if (tile != null && tile.pos == _uiState.value.selectedTile?.pos) {
            _uiState.update {
                it.copy(
                    selectedTile = null,
                    selectedTower = null
                )
            }
            return
        }
        val tower = _uiState.value.towers.firstOrNull { it.gridPos == tile?.pos }
        _uiState.update {
            it.copy(
                selectedTile = tile,
                selectedTower = tower,
                hasClickedFirstTile = if (tile != null) true else it.hasClickedFirstTile
            )
        }
    }

    // === TIER 0: Base Placement (Basic, Elemental, Heavy) ===
    fun buildTower(tile: MapTile, towerClass: TowerClass = TowerClass.KINETIC_BALLISTIC) {
        val state = _uiState.value
        val cost = (towerClass.baseCost * state.balanceConfig.buildCostMultiplier).roundToInt()

        if (state.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }
        if (state.towers.any { it.gridPos == tile.pos }) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }
        if (!TowerUpgradeManager.isClassUnlocked(towerClass, state.towers)) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        val newTower = TowerInstance(
            id = nextId("tow_"),
            gridPos = tile.pos,
            towerClass = towerClass,
            tier = 0
        )

        soundManager.play(com.example.audio.SoundFx.PLACE)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers + newTower,
                selectedTower = newTower
            )
        }

        addFloatingText(tile.pos.x.toFloat(), tile.pos.y.toFloat(), "-$cost", 0xFFFFD700)
    }

    // Compatibility method
    fun buildTower(tile: MapTile, type: TowerType) {
        buildTower(tile, TowerClass.KINETIC_BALLISTIC)
    }

    // === TIER 1: Vector Specialization (Offensive / Defensive / Support) ===
    fun upgradeTowerToVector(tower: TowerInstance, vector: TowerVector) {
        if (tower.tier != 0 && tower.branchTier1 == null) return
        val cost = TowerUpgradeManager.getTier1VectorCost(tower.towerClass)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.tier = 1
        tower.vector = vector
        tower.branchTier1 = when (vector) {
            TowerVector.OFFENSIVE -> "A"
            TowerVector.DEFENSIVE -> "B"
            TowerVector.SUPPORT -> "C"
        }

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList(),
                selectedTower = tower
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+${vector.displayName} ${tower.towerClass.displayName}!", 0xFF00FF9D)
    }

    // === TIER 2: Identity Lock (Archetype Selection from 9 options per class) ===
    fun upgradeTowerToArchetype(tower: TowerInstance, archetype: TowerArchetype) {
        if (tower.tier != 1 && !tower.isTier1) return
        val cost = TowerUpgradeManager.getTier2Cost(archetype)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.tier = 2
        tower.archetype = archetype
        tower.vector = archetype.vector
        tower.towerClass = archetype.towerClass
        tower.finalFormCode = "${tower.branchTier1 ?: "A"}.a"

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList(),
                selectedTower = tower,
                screenShakeIntensity = 0.4f
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ ${archetype.displayName}! ★", 0xFFFFB800)
    }

    // === TIER 3: Global Aura Projection (Choose between 2 unique auras!) ===
    fun upgradeTowerToTier3Global(tower: TowerInstance, auraIndex: Int = 0) {
        if (tower.tier != 2 && !tower.isTier2) return
        val cost = TowerUpgradeManager.getTier3GlobalCost(tower, _uiState.value.towers)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.tier = 3
        tower.isTier3GlobalActive = true
        tower.selectedAuraIndex = auraIndex
        val arch = tower.archetype ?: mapTypeToArchetype(tower.type)
        val chosenAura = if (auraIndex == 1) arch.secondaryAura else arch.primaryAura

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList(),
                selectedTower = tower,
                screenShakeIntensity = 0.8f
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "🌟 ${chosenAura.name} ONLINE! 🌟", 0xFFFFD700)
    }

    private fun computeActiveSynergies(towers: List<TowerInstance>): List<GlobalAuraSynergy> {
        val tier3Towers = towers.filter { it.isTier3 }
        val counts = mutableMapOf<Pair<TowerArchetype, Int>, Int>()
        for (t in tier3Towers) {
            val arch = t.archetype ?: mapTypeToArchetype(t.type)
            val key = arch to t.selectedAuraIndex
            counts[key] = (counts[key] ?: 0) + 1
        }

        val activeList = counts.map { (key, count) ->
            val (arch, auraIdx) = key
            val aura = if (auraIdx == 1) arch.secondaryAura else arch.primaryAura
            val emoji = when (arch.towerClass) {
                TowerClass.KINETIC_BALLISTIC -> "⚔️"
                TowerClass.ELEMENTAL_VOID -> "✨"
                TowerClass.HEAVY_SIEGE -> "💥"
                TowerClass.STELLAR_COSMIC -> "★"
            }
            GlobalAuraSynergy(
                auraName = aura.name,
                effectDescription = aura.description,
                sourceCount = count,
                iconEmoji = emoji,
                color = arch.color
            )
        }.toMutableList()

        val capstone = towers.firstOrNull { it.isCapstone }
        if (capstone != null && capstone.synergyVault.isNotEmpty()) {
            val bankedList = capstone.synergyVault.flatMap { it.synergies }
            for (bs in bankedList) {
                val existingIndex = activeList.indexOfFirst { it.auraName == bs.auraName }
                if (existingIndex >= 0) {
                    val existing = activeList[existingIndex]
                    activeList[existingIndex] = existing.copy(sourceCount = existing.sourceCount + bs.sourceCount)
                } else {
                    activeList.add(bs)
                }
            }
        }

        return activeList
    }

    fun canInitiateSingularity(): Boolean {
        val state = _uiState.value
        val level = state.level ?: return false
        val buildableTiles = level.tiles.filter { it.isBuildable }
        if (buildableTiles.isEmpty()) return false
        if (state.towers.size < buildableTiles.size) return false
        return state.towers.all { it.tier >= 2 || it.finalFormCode != null }
    }

    fun getSingularityAnchorTower(): TowerInstance? {
        val state = _uiState.value
        val selected = state.selectedTower
        if (selected != null && (selected.tier >= 2 || selected.finalFormCode != null)) {
            // Player-chosen tactical agency over Conduit monument placement
            return selected
        }
        val capstone = state.towers.firstOrNull { it.isCapstone }
        if (capstone != null) return capstone
        return state.towers.minByOrNull { it.id }
    }

    fun requestSingularityCollapse() {
        val state = _uiState.value
        if (!canInitiateSingularity()) {
            val level = state.level
            val buildableTiles = level?.tiles?.filter { it.isBuildable } ?: emptyList()
            val readyCount = state.towers.count { it.tier >= 2 || it.finalFormCode != null }
            val total = buildableTiles.size
            soundManager.play(com.example.audio.SoundFx.INVALID)
            val anchor = state.selectedTower ?: state.towers.firstOrNull()
            if (anchor != null) {
                addFloatingText(
                    anchor.gridPos.x.toFloat(),
                    anchor.gridPos.y.toFloat(),
                    "Grid Incomplete! Fill all tiles ($readyCount/$total) to Tier 2+!",
                    0xFFFF3B30
                )
            }
            return
        }
        _uiState.update { it.copy(isSingularityWarningOpen = true) }
    }

    fun dismissSingularityWarning() {
        _uiState.update { it.copy(isSingularityWarningOpen = false) }
    }

    fun confirmSingularityCollapse() {
        val state = _uiState.value
        if (!canInitiateSingularity()) {
            _uiState.update { it.copy(isSingularityWarningOpen = false) }
            return
        }

        val anchor = getSingularityAnchorTower() ?: return
        val sacrificed = state.towers.filter { it.id != anchor.id }

        // Strictly ONE Synergy Conduit rule: If a prior Conduit exists among sacrificed towers,
        // absorb all of its historic banked vaults and generation into the newly chosen anchor
        val previousCapstone = sacrificed.firstOrNull { it.isCapstone }
        if (previousCapstone != null) {
            anchor.synergyVault.addAll(previousCapstone.synergyVault)
            anchor.generation = maxOf(anchor.generation, previousCapstone.generation)
        }

        // Calculate 20% scrap refund bounty
        val totalInvested = sacrificed.sumOf { it.getInvestedScrapValue() }
        val scrapBounty = maxOf(250, (totalInvested * 0.20f).toInt())

        // Capture current generation active synergies to bank into anchor
        val currentSynergies = computeActiveSynergies(state.towers)
        anchor.synergyVault.add(
            BankedSynergyVault(
                generation = anchor.generation + 1,
                synergies = currentSynergies
            )
        )
        anchor.isCapstone = true
        anchor.generation += 1

        val newExplosions = state.explosions.toMutableList()
        sacrificed.forEach { st ->
            newExplosions.add(
                ExplosionEffect(
                    id = nextId("exp_"),
                    x = st.gridPos.x.toFloat(),
                    y = st.gridPos.y.toFloat(),
                    maxRadius = 1.6f,
                    color = 0xFFFFD700
                )
            )
        }

        soundManager.play(com.example.audio.SoundFx.UPGRADE)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                towers = listOf(anchor),
                selectedTower = anchor,
                scrap = it.scrap + scrapBounty,
                totalScrapEarned = it.totalScrapEarned + scrapBounty,
                explosions = newExplosions,
                screenShakeIntensity = 1.0f,
                isSingularityWarningOpen = false
            )
        }

        addFloatingText(
            anchor.gridPos.x.toFloat(),
            anchor.gridPos.y.toFloat(),
            "+$scrapBounty SCRAP BOUNTY!",
            0xFFFFD700
        )
        addFloatingText(
            anchor.gridPos.x.toFloat(),
            anchor.gridPos.y.toFloat() - 0.7f,
            "★ GEN ${anchor.generation} CONDUIT ONLINE ★",
            0xFF00F0FF
        )
    }

    fun setGameDifficulty(difficulty: GameDifficulty) {
        if (_uiState.value.isWaveActive) return
        _uiState.update { it.copy(difficulty = difficulty) }
    }

    fun upgradeTowerToTier1(tower: TowerInstance, branchKey: String) {
        if (!tower.isBase) return
        val cost = TowerUpgradeManager.getTier1Cost(tower, branchKey)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.branchTier1 = branchKey
        val option = TowerUpgradeManager.getTier1Options(tower.type).firstOrNull { it.branchKey == branchKey }
        val name = option?.subName ?: "Branch $branchKey"

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList(),
                selectedTower = tower
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$name!", 0xFF00FF9D)
    }

    fun upgradeTowerToFinalForm(tower: TowerInstance, finalFormCode: String) {
        if (!tower.isTier1) return
        val cost = TowerUpgradeManager.getFinalFormCost(tower, finalFormCode, _uiState.value.towers)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.finalFormCode = finalFormCode
        val finalFormDef = TowerUpgradeManager.getFinalForm(tower.type, finalFormCode)
        val name = finalFormDef?.subName ?: "Apex Form"

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList(),
                selectedTower = tower,
                screenShakeIntensity = 0.5f
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ $name! ★", 0xFFFFB800)
    }

    fun upgradeTowerPath(tower: TowerInstance, pathIndex: Int) {
        val currentTier = tower.pathTiers[pathIndex]
        if (currentTier >= 2) return // Max 2 upgrades per path!

        val nextTier = currentTier + 1
        val cost = TowerUpgradeManager.getUpgradeCost(tower, pathIndex, nextTier, _uiState.value.towers)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.pathTiers[pathIndex] = nextTier
        val upgradeName = TowerUpgradeManager.getPathUpgrade(tower.type, pathIndex, nextTier).name

        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap - cost,
                towers = it.towers.toList() // trigger recomposition
            )
        }

        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$upgradeName!", 0xFF00FF9D)
    }

    fun ascendTower(tower: TowerInstance) {
        if (!tower.isMaxed || tower.isAscended) return

        val cost = TowerUpgradeManager.getAscensionCost(tower)
        if (_uiState.value.scrap < cost) {
            soundManager.play(com.example.audio.SoundFx.INVALID)
            return
        }

        tower.finalFormCode = "${tower.branchTier1 ?: "A"}.a"
        soundManager.playTowerUpgrade(tower.towerClass, _uiState.value.turretSkinSlot)
        isSynergyCacheDirty = true

        // Apply tower's distinctive global boost
        _uiState.update { state ->
            val updated = state.copy(
                scrap = state.scrap - cost,
                towers = state.towers.toList(),
                screenShakeIntensity = 0.8f
            )

            when (tower.type) {
                TowerType.GATLING -> updated.copy(globalKineticSpeedBonus = updated.globalKineticSpeedBonus + 0.22f)
                TowerType.CRYO -> updated.copy(globalFrostSlowBonus = 0.15f, globalDamageBonus = updated.globalDamageBonus + 0.20f)
                TowerType.TESLA -> updated.copy(globalTeslaArcsEnabled = true)
                TowerType.MORTAR -> updated.copy(globalMortarSplashBonus = updated.globalMortarSplashBonus + 0.25f)
                TowerType.LASER -> updated.copy(globalSolarCritsEnabled = true)
                TowerType.VOID -> updated.copy(globalVoidShredEnabled = true, scrapBountyBonus = updated.scrapBountyBonus + 0.35f)
                TowerType.STELLAR -> updated.copy(globalDamageBonus = updated.globalDamageBonus + 0.20f)
            }
        }

        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "★ APEX ASCENDED! ★", 0xFFFFB800)
    }

    private var lastBoosterSwapTimeMs: Long = 0L

    fun swapBoosterPads() {
        val now = System.currentTimeMillis()
        if (now - lastBoosterSwapTimeMs < 750L) return
        val level = _uiState.value.level ?: return
        val currentTowers = _uiState.value.towers.toMutableList()
        if (currentTowers.isEmpty()) return

        val padPairs = listOf(TileBuff.AMPLIFIER, TileBuff.HIGH_GROUND, TileBuff.CONDUIT)
        var swappedAny = false

        for (buffType in padPairs) {
            val pads = level.tiles.filter { it.buff == buffType }
            if (pads.size == 2) {
                val pad1 = pads[0].pos
                val pad2 = pads[1].pos
                val tower1 = currentTowers.firstOrNull { it.gridPos == pad1 }
                val tower2 = currentTowers.firstOrNull { it.gridPos == pad2 }

                if (tower1 != null && tower2 != null) {
                    val idx1 = currentTowers.indexOf(tower1)
                    val idx2 = currentTowers.indexOf(tower2)
                    currentTowers[idx1] = tower1.copy(gridPos = pad2)
                    currentTowers[idx2] = tower2.copy(gridPos = pad1)
                    swappedAny = true
                    addFloatingText(pad1.x.toFloat(), pad1.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF)
                    addFloatingText(pad2.x.toFloat(), pad2.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF)
                } else if (tower1 != null && tower2 == null) {
                    val idx1 = currentTowers.indexOf(tower1)
                    currentTowers[idx1] = tower1.copy(gridPos = pad2)
                    swappedAny = true
                    addFloatingText(pad2.x.toFloat(), pad2.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF)
                } else if (tower1 == null && tower2 != null) {
                    val idx2 = currentTowers.indexOf(tower2)
                    currentTowers[idx2] = tower2.copy(gridPos = pad1)
                    swappedAny = true
                    addFloatingText(pad1.x.toFloat(), pad1.y.toFloat(), "⇄ TELEPORT", 0xFF00F0FF)
                }
            }
        }

        if (swappedAny) {
            lastBoosterSwapTimeMs = now
            soundManager.playPlacement()
            isSynergyCacheDirty = true
            _uiState.update { it.copy(towers = currentTowers) }
        }
    }

    fun sellSelectedTower() {
        val tower = _uiState.value.selectedTower ?: return
        val sellVal = tower.getSellValue()
        soundManager.play(com.example.audio.SoundFx.SELL)
        isSynergyCacheDirty = true

        _uiState.update {
            it.copy(
                scrap = it.scrap + sellVal,
                towers = it.towers.filter { t -> t.id != tower.id },
                selectedTower = null,
                selectedTile = null
            )
        }
        addFloatingText(tower.gridPos.x.toFloat(), tower.gridPos.y.toFloat(), "+$sellVal", 0xFFFFD700)
    }

    fun changeSelectedTowerPriority(priority: TargetPriority) {
        val tower = _uiState.value.selectedTower ?: return
        tower.targetPriority = priority
        _uiState.update { it.copy(towers = it.towers.toList()) }
    }

    private fun recordRunCompletion(isVictory: Boolean, waves: Int) {
        val state = _uiState.value
        // If no wave was started or zero combat score, do not save!
        val wasCombatStarted = waves > 0 || state.totalKills > 0 || state.totalDamageDealt > 0f
        if (!wasCombatStarted) {
            return
        }

        val mvp = state.towers.maxByOrNull { it.totalDamage }?.let { it.archetype?.displayName ?: it.type.displayName } ?: "Vulcan Minigun"
        val playerName = telemetryManager.getActivePlayerName()
        val isDev = devTunedThisRun || state.balanceConfig.isGameplayModified()
        val record = RunRecord(
            id = UUID.randomUUID().toString(),
            seed = state.seed,
            sectorName = state.level?.biome?.name ?: "Sector Zero",
            playerName = playerName,
            wavesCleared = waves,
            enemiesKilled = state.totalKills,
            totalDamage = state.totalDamageDealt,
            scrapEarned = state.totalScrapEarned,
            ascendedTowersCount = state.towers.count { it.isAscended },
            mvpTower = mvp,
            isVictory = isVictory,
            timestamp = System.currentTimeMillis(),
            isDevTuned = isDev
        )

        if (!isVictory) {
            val mvpClass = state.towers.maxByOrNull { it.totalDamage }?.towerClass?.name ?: "KINETIC_BALLISTIC"
            telemetryManager.logLevelFail(
                waveFailed = waves + 1,
                towersBuilt = state.towers.size,
                currencyRemaining = state.scrap,
                mvpTowerClass = mvpClass
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            telemetryManager.recordRun(record)
            telemetryManager.recordTowerSession(state.towers)
            refreshTelemetry()

            leaderboardRepository.enqueueRun(
                run = record,
                callsign = playerName,
                isDevTuned = isDev,
                userId = authRepository.currentUserId
            )
            leaderboardRepository.fetchLeaderboard(forceRefresh = true)
        }
    }

    fun setPlayerName(name: String) {
        telemetryManager.updateLastRunPlayerName(name)
        refreshTelemetry()
    }

    fun toggleAutoWave() {
        _uiState.update { it.copy(autoCallNextWave = !it.autoCallNextWave) }
    }

    fun setGameSpeed(speed: GameSpeed) {
        _uiState.update { current ->
            if (current.isPauseMenuOpen || current.showEndRunConfirmDialog) {
                current.copy(prePauseSpeed = speed, gameSpeed = GameSpeed.PAUSED)
            } else {
                current.copy(gameSpeed = speed, prePauseSpeed = speed)
            }
        }
    }

    internal fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean = false) {
        val ft = FloatingText(
            id = nextId("ft_"),
            x = x,
            y = y,
            text = text,
            color = color,
            isCrit = isCrit
        )
        _uiState.update { it.copy(floatingTexts = it.floatingTexts + ft) }
    }

    // === IN-APP 30-SECOND SCRIPTED CINEMATIC ENGINE (TRACK 4) ===
    fun launchCinematic(scenario: CinematicScenario) = cinematicController.launchCinematic(scenario)
    fun stopCinematic() = cinematicController.stopCinematic()
    fun jumpToSignatureWave(scenario: CinematicScenario) = cinematicController.jumpToSignatureWave(scenario)
}

