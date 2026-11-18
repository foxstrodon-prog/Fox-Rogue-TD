package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicTrack
import com.example.audio.SoundFx
import com.example.audio.SoundManager
import com.example.audio.SoundProfile
import com.example.engine.*
import com.example.engine.coordinator.AssetVaultCoordinator
import com.example.engine.coordinator.SimulationCoordinator
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
import com.example.engine.network.AssetVoteSyncManager
import com.example.engine.network.AuthRepository
import com.example.engine.network.FirestoreLeaderboardApi
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

class RogueTDViewModel @JvmOverloads constructor(
    application: Application,
    val authRepository: AuthRepository = AuthRepository(
        context = application,
        apiKey = FirestoreLeaderboardApi.resolveDefaultApiKey(application)
    ),
    val remoteConfigManager: RemoteConfigManager = RemoteConfigManager(),
    val leaderboardRepository: LeaderboardRepository = LeaderboardRepository(
        api = FirestoreLeaderboardApi(context = application),
        store = LeaderboardStore(application),
        scheduler = LeaderboardWorkScheduler(application),
        authRepo = authRepository
    )
) : AndroidViewModel(application) {

    private val telemetryManager = TelemetryManager(application)
    val appUpdateManager = AppUpdateManager(application, viewModelScope)
    val soundManager = SoundManager(application)
    private val cinematicHost = object : CinematicHost {
        override val uiStateFlow: MutableStateFlow<GameUIState> get() = _uiState
        override val soundManager: SoundManager get() = this@RogueTDViewModel.soundManager
        override fun clearPendingSpawns() = this@RogueTDViewModel.clearPendingSpawns()
        override fun markSynergyDirty() = this@RogueTDViewModel.markSynergyDirty()
        override fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean) =
            this@RogueTDViewModel.addFloatingText(x, y, text, color, isCrit)
        override fun swapBoosterPads() = this@RogueTDViewModel.swapBoosterPads()
        override fun deployWave() = this@RogueTDViewModel.deployWave()
    }
    val cinematicController = CinematicController(cinematicHost)

    private var nextEntityId: Long = 0L
    private fun nextId(prefix: String = "ent_"): String = "$prefix${nextEntityId++}"

    private val initialConfig = telemetryManager.getBalanceConfig()
    internal val _uiState = MutableStateFlow(
        GameUIState(
            balanceConfig = initialConfig,
            masterVolume = initialConfig.masterVolume,
            musicVolume = initialConfig.musicVolume,
            sfxVolume = initialConfig.sfxVolume,
            upgradeVolume = initialConfig.upgradeVolume,
            isAudioMuted = initialConfig.isMuted,
            musicTrack = runCatching { MusicTrack.valueOf(initialConfig.musicTrackName) }.getOrDefault(MusicTrack.ROLLING_PLAYLIST),
            kineticSfxSlot = initialConfig.kineticSfxSlot, elementalSfxSlot = initialConfig.elementalSfxSlot,
            siegeSfxSlot = initialConfig.siegeSfxSlot, stellarSfxSlot = initialConfig.stellarSfxSlot,
            turretSkinSlot = initialConfig.turretSkinSlot, mapThemeSlot = initialConfig.mapThemeSlot,
            kineticFireId = initialConfig.kineticFireId, elementalFireId = initialConfig.elementalFireId,
            siegeFireId = initialConfig.siegeFireId, stellarFireId = initialConfig.stellarFireId,
            turretSkinId = initialConfig.turretSkinId, mapThemeId = initialConfig.mapThemeId,
            bgmTrackId = initialConfig.bgmTrackId, sfxPackId = initialConfig.sfxPackId
        )
    )
    val uiState: StateFlow<GameUIState> = _uiState.asStateFlow()

    private var devTunedThisRun: Boolean = false
    private var runSessionId: Long = 0L
    private var gameLoopJob: Job? = null
    private var lastRecordRunJob: Job? = null
    private var lastSetPlayerNameJob: Job? = null
    private var updateDismissedForSession: Boolean = false

    // Domain Coordinators (Task D-01)
    val combatEngine = CombatSimulationEngine()
    val spatialGrid: SpatialHashGrid get() = combatEngine.spatialGrid

    val waveCoordinator = WaveProgressionCoordinator(
        uiStateFlow = _uiState, remoteConfigManager = remoteConfigManager,
        telemetryManager = telemetryManager, nextId = ::nextId,
        addFloatingText = ::addFloatingText, onRecordRunCompletion = ::recordRunCompletion
    )

    val simulationCoordinator = SimulationCoordinator(
        uiStateFlow = _uiState, combatEngine = combatEngine, waveCoordinator = waveCoordinator,
        soundManager = soundManager, remoteConfigManager = remoteConfigManager,
        cinematicController = cinematicController, nextId = ::nextId,
        onRecordRunCompletion = ::recordRunCompletion
    )

    companion object {
        /** Global simulation baseline pace boost so 1X plays at ~1.85x prior baseline speed. */
        const val BASE_SIM_SPEED_MULTIPLIER = 1.85f
        const val FIXED_SIM_STEP_SEC = 0.020f
        const val FIXED_SIM_STEP_MS = 20L
        private const val MAX_PHYSICS_SUBSTEPS = 48
    }

    private var simAccumulatorSec: Float = 0f

    val placementCoordinator = TowerPlacementCoordinator(
        uiStateFlow = _uiState, soundManager = soundManager, telemetryManager = telemetryManager,
        nextId = ::nextId, addFloatingText = ::addFloatingText, onSynergyDirty = ::markSynergyDirty,
        onRedispatchEnemies = waveCoordinator::requeueActiveEnemies,
        onResetTrackParticles = { lvl -> createInitialTrackParticles(lvl.laneAlpha, lvl.laneBeta) }
    )

    val assetVaultCoordinator = AssetVaultCoordinator(
        uiStateFlow = _uiState, soundManager = soundManager, telemetryManager = telemetryManager,
        voteSyncManager = AssetVoteSyncManager(application, authRepository), scope = viewModelScope
    )

    fun markSynergyDirty() { simulationCoordinator.isSynergyCacheDirty = true }
    fun clearPendingSpawns() { waveCoordinator.clearPendingSpawns() }
    var currentFps: Int = 60; private set
    var frameJitterMs: Float = 0.5f; private set
    private val frameTimes = LongArray(30)
    private var frameCount = 0

    init {
        remoteConfigManager.initialize()
        telemetryManager.logTutorialStart()
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.ensureAuthenticated()
            assetVaultCoordinator.syncPendingVotesIfNeeded()
        }

        val config = _uiState.value.balanceConfig
        soundManager.setMasterVolumeLevel(config.masterVolume)
        soundManager.setMusicVolumeLevel(config.musicVolume)
        soundManager.setSfxVolumeLevel(config.sfxVolume)
        soundManager.setUpgradeVolumeLevel(config.upgradeVolume)
        soundManager.setMutedState(config.isMuted)
        if (soundManager.activeMusicTrack != _uiState.value.musicTrack) {
            soundManager.activeMusicTrack = _uiState.value.musicTrack
        }
        assetVaultCoordinator.syncEquippedFromState()

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
                        runSubmissionState = boardState.lastSubmissionState,
                        leaderboardSubmissionError = boardState.lastSubmissionError,
                        leaderboardErrorMessage = when (boardState.status) {
                            GlobalBoardStatus.ERROR -> {
                                boardState.errorMessage
                                    ?: if (boardState.pendingCount > 0) "Offline Queue: ${boardState.pendingCount} score(s) pending sync"
                                    else boardState.failureKind?.name ?: "Connection error"
                            }
                            GlobalBoardStatus.OFFLINE_CACHED -> {
                                if (boardState.entries.isEmpty()) boardState.errorMessage ?: "Connection failed. Scores cached locally."
                                else null
                            }
                            GlobalBoardStatus.NOT_CONFIGURED -> {
                                boardState.errorMessage ?: "Database Pending: Enable (default) Firestore in Firebase Console"
                            }
                            else -> null
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            appUpdateManager.updateState.collect { updateState ->
                _uiState.update { prev ->
                    val shouldOpenModal = if ((updateState.status == UpdateDownloadStatus.AVAILABLE || updateState.status == UpdateDownloadStatus.READY_TO_INSTALL) && !updateDismissedForSession) true else prev.showUpdateModal
                    prev.copy(updateUIState = updateState, showUpdateModal = shouldOpenModal)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            appUpdateManager.performStartupApkHygiene()
        }
    }

    fun updateDeviceRotation(targetAngle: Float) {
        val normalized = (targetAngle % 360f + 360f) % 360f
        if (_uiState.value.deviceRotationAngle != normalized || _uiState.value.hardwareTiltAngle != normalized || _uiState.value.isSystemAutoRotateLocked) {
            _uiState.update { it.copy(deviceRotationAngle = normalized, hardwareTiltAngle = normalized, isSystemAutoRotateLocked = false) }
        }
    }

    fun updateHardwareTiltAngle(targetAngle: Float) {
        val normalized = (targetAngle % 360f + 360f) % 360f
        if (_uiState.value.hardwareTiltAngle != normalized || !_uiState.value.isSystemAutoRotateLocked) {
            _uiState.update { it.copy(hardwareTiltAngle = normalized, isSystemAutoRotateLocked = true) }
        }
    }

    fun confirmManualRotation() {
        val target = _uiState.value.hardwareTiltAngle
        _uiState.update { it.copy(deviceRotationAngle = target) }
    }

    fun fetchOnlineLeaderboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (forceRefresh && leaderboardRepository.boardState.value.pendingCount > 0) {
                leaderboardRepository.retryPendingSubmissions(replaceExisting = true)
            }
            leaderboardRepository.fetchLeaderboard(forceRefresh = forceRefresh)
        }
    }

    fun clearPendingLeaderboardQueue() {
        viewModelScope.launch {
            leaderboardRepository.clearPendingQueue()
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch { appUpdateManager.checkForUpdates() }
    }

    fun downloadAndInstallUpdate() {
        val state = _uiState.value.updateUIState
        if (state.status == UpdateDownloadStatus.DOWNLOADING || state.status == UpdateDownloadStatus.CHECKING) return
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

    fun openUpdateModal() = _uiState.update { it.copy(showUpdateModal = true) }

    override fun onCleared() {
        super.onCleared()
        appUpdateManager.unregisterReceiver()
        leaderboardRepository.close()
        soundManager.release()
    }

    fun refreshTelemetry() {
        val activeName = telemetryManager.getActivePlayerName()
        val persistedLastRun = telemetryManager.getLastRun()
        _uiState.update {
            it.copy(
                activePlayerName = activeName,
                lastRunRecord = persistedLastRun ?: it.lastRunRecord,
                playerLifetimeTelemetry = telemetryManager.getLifetimeTelemetryForPlayer(activeName),
                leaderboard = telemetryManager.getLeaderboard(),
                lifetimeTelemetry = telemetryManager.getLifetimeTelemetry(),
                towerPerformance = telemetryManager.getTowerPerformance()
            )
        }
    }

    fun toggleTelemetryView(open: Boolean, initialTab: Int = 0) {
        if (open) {
            refreshTelemetry()
            if (initialTab != 2) {
                fetchOnlineLeaderboard(forceRefresh = false)
            }
        }
        _uiState.update { it.copy(isTelemetryOpen = open, telemetryInitialTab = initialTab) }
    }

    fun toggleSettingsView(open: Boolean) {
        if (open) refreshTelemetry()
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun openTelemetryFromPauseMenu() {
        refreshTelemetry()
        _uiState.update { it.copy(isPauseMenuOpen = false, isTelemetryOpen = true, telemetryInitialTab = 2) }
    }

    fun openSettingsFromPauseMenu() {
        refreshTelemetry()
        _uiState.update { it.copy(isPauseMenuOpen = false, isSettingsOpen = true) }
    }

    fun openAssetVaultFromPauseMenu() {
        assetVaultCoordinator.onVaultOpened()
        _uiState.update { it.copy(isPauseMenuOpen = false, isAssetVaultOpen = true) }
    }

    fun closeAssetVault() {
        assetVaultCoordinator.stopPreview()
        assetVaultCoordinator.syncPendingVotesIfNeeded()
        _uiState.update { it.copy(isAssetVaultOpen = false, isPauseMenuOpen = true) }
    }

    fun isInternalDialogActive(): Boolean {
        val s = _uiState.value
        return s.isPauseMenuOpen || s.isSettingsOpen || s.isTelemetryOpen || s.isAssetVaultOpen || s.showEndRunConfirmDialog || s.showUpdateModal || s.isGameOver || s.isVictory || s.isSingularityWarningOpen
    }

    fun openPauseMenu() {
        val s = _uiState.value
        if (s.isGameOver || s.isVictory || s.isPauseMenuOpen || s.isSettingsOpen || s.isTelemetryOpen || s.isAssetVaultOpen) return
        soundManager.isPaused = true
        val currentSpeed = s.gameSpeed
        val savedSpeed = if (currentSpeed != GameSpeed.PAUSED) currentSpeed else s.prePauseSpeed
        _uiState.update { it.copy(isPauseMenuOpen = true, prePauseSpeed = savedSpeed, gameSpeed = GameSpeed.PAUSED) }
    }

    fun closePauseMenu() {
        soundManager.isPaused = false
        val resumeSpeed = if (_uiState.value.prePauseSpeed != GameSpeed.PAUSED) _uiState.value.prePauseSpeed else GameSpeed.NORMAL
        _uiState.update { it.copy(isPauseMenuOpen = false, gameSpeed = resumeSpeed) }
    }

    fun requestEndRun() {
        soundManager.isPaused = true
        val currentSpeed = _uiState.value.gameSpeed
        val savedSpeed = if (currentSpeed != GameSpeed.PAUSED) currentSpeed else _uiState.value.prePauseSpeed
        _uiState.update { it.copy(showEndRunConfirmDialog = true, prePauseSpeed = savedSpeed, gameSpeed = GameSpeed.PAUSED) }
    }

    fun dismissEndRunConfirm() {
        if (!_uiState.value.isPauseMenuOpen) soundManager.isPaused = false
        val resumeSpeed = if (!_uiState.value.isPauseMenuOpen && _uiState.value.prePauseSpeed != GameSpeed.PAUSED) _uiState.value.prePauseSpeed else GameSpeed.PAUSED
        _uiState.update { it.copy(showEndRunConfirmDialog = false, gameSpeed = resumeSpeed) }
    }

    fun confirmEndRun() {
        soundManager.isPaused = false
        val state = _uiState.value
        val wavesCompleted = maxOf(0, state.waveNumber - 1)
        recordRunCompletion(isVictory = false, waves = wavesCompleted)
        _uiState.update { current ->
            current.copy(
                showEndRunConfirmDialog = false, isPauseMenuOpen = false,
                isGameOver = true, isVictory = false, isWaveActive = false, gameSpeed = GameSpeed.PAUSED
            )
        }
    }

    fun dismissFirstTileHint() {
        telemetryManager.logTutorialComplete()
        _uiState.update { it.copy(hasClickedFirstTile = true) }
    }

    fun updateBalanceConfig(newConfig: DevBalanceConfig) {
        val oldConfig = _uiState.value.balanceConfig
        if (newConfig.isGameplayModified()) devTunedThisRun = true
        telemetryManager.saveBalanceConfig(newConfig)
        soundManager.setMasterVolumeLevel(newConfig.masterVolume)
        soundManager.setMusicVolumeLevel(newConfig.musicVolume)
        soundManager.setSfxVolumeLevel(newConfig.sfxVolume)
        soundManager.setUpgradeVolumeLevel(newConfig.upgradeVolume)
        soundManager.setMutedState(newConfig.isMuted)
        _uiState.update { it.copy(balanceConfig = newConfig) }
        regenerateMapIfPreWave1(oldConfig, newConfig, _uiState.value.difficulty)
    }

    fun resetBalanceConfigToDefaults() {
        val oldConfig = _uiState.value.balanceConfig
        val defaultConfig = DevBalanceConfig()
        telemetryManager.saveBalanceConfig(defaultConfig)
        soundManager.setMasterVolumeLevel(defaultConfig.masterVolume)
        soundManager.setMusicVolumeLevel(defaultConfig.musicVolume)
        soundManager.setSfxVolumeLevel(defaultConfig.sfxVolume)
        soundManager.setUpgradeVolumeLevel(defaultConfig.upgradeVolume)
        soundManager.setMutedState(defaultConfig.isMuted)
        _uiState.update {
            it.copy(
                balanceConfig = defaultConfig, masterVolume = defaultConfig.masterVolume,
                musicVolume = defaultConfig.musicVolume, sfxVolume = defaultConfig.sfxVolume,
                upgradeVolume = defaultConfig.upgradeVolume, isAudioMuted = defaultConfig.isMuted,
                kineticSfxSlot = defaultConfig.kineticSfxSlot, elementalSfxSlot = defaultConfig.elementalSfxSlot,
                siegeSfxSlot = defaultConfig.siegeSfxSlot, stellarSfxSlot = defaultConfig.stellarSfxSlot,
                turretSkinSlot = defaultConfig.turretSkinSlot, mapThemeSlot = defaultConfig.mapThemeSlot,
                kineticFireId = defaultConfig.kineticFireId, elementalFireId = defaultConfig.elementalFireId,
                siegeFireId = defaultConfig.siegeFireId, stellarFireId = defaultConfig.stellarFireId,
                turretSkinId = defaultConfig.turretSkinId, mapThemeId = defaultConfig.mapThemeId,
                bgmTrackId = defaultConfig.bgmTrackId, sfxPackId = defaultConfig.sfxPackId
            )
        }
        assetVaultCoordinator.syncEquippedFromState()
        regenerateMapIfPreWave1(oldConfig, defaultConfig, _uiState.value.difficulty)
    }

    private fun regenerateMapIfPreWave1(oldConfig: DevBalanceConfig, newConfig: DevBalanceConfig, difficulty: GameDifficulty) {
        val state = _uiState.value
        if (!state.isWaveActive && state.waveNumber == 1 && state.towers.isEmpty()) {
            val geometryChanged = oldConfig.easyTargetOpenSlots != newConfig.easyTargetOpenSlots ||
                oldConfig.mediumTargetOpenSlots != newConfig.mediumTargetOpenSlots ||
                oldConfig.hardTargetOpenSlots != newConfig.hardTargetOpenSlots ||
                oldConfig.minLaneOverlap != newConfig.minLaneOverlap ||
                oldConfig.maxLaneOverlap != newConfig.maxLaneOverlap ||
                oldConfig.mapColumns != newConfig.mapColumns ||
                oldConfig.mapRows != newConfig.mapRows
            if (geometryChanged) {
                regeneratePreWave1Level(newConfig, difficulty)
            }
        }
    }

    private fun regeneratePreWave1Level(balance: DevBalanceConfig, difficulty: GameDifficulty) {
        val seed = _uiState.value.seed
        val level = MapGenerator.generate(
            seed = seed,
            width = balance.mapColumns,
            height = balance.mapRows,
            minLaneLength = balance.minLaneLength,
            maxLaneLength = balance.maxLaneLength,
            targetOpenSlots = balance.getTargetOpenSlotsForDifficulty(difficulty),
            minLaneOverlap = balance.minLaneOverlap,
            maxLaneOverlap = balance.maxLaneOverlap
        )
        val initialParticles = createInitialTrackParticles(level.laneAlpha, level.laneBeta)
        _uiState.update { it.copy(level = level, trackParticles = initialParticles, selectedTile = null) }
    }

    private fun createInitialTrackParticles(laneAlpha: List<GridPos>, laneBeta: List<GridPos>): List<TrackParticle> {
        val totalCount = 24
        val particles = ArrayList<TrackParticle>(totalCount)
        for (i in 0 until totalCount) {
            val lane = i % 2
            val laneIdx = i / 2
            val path = if (lane == 0) laneAlpha else laneBeta
            val segCount = (path.size - 1).coerceAtLeast(1)
            val segIdx = ((laneIdx * segCount) / 12).coerceIn(0, (segCount - 1).coerceAtLeast(0))
            particles.add(
                TrackParticle(
                    id = "tp_$i",
                    laneIndex = lane,
                    segmentIndex = segIdx,
                    progress = ((laneIdx * 0.23f) + lane * 0.11f) % 1.0f,
                    speed = 0.85f + (i % 3) * 0.20f,
                    color = if (lane == 0) 0xFF00F0FF else 0xFFFFB800,
                    size = 2.5f + (i % 2) * 1.5f
                )
            )
        }
        return particles
    }

    fun resetTowerPerformance() {
        telemetryManager.resetTowerPerformance()
        _uiState.update { it.copy(towerPerformance = telemetryManager.getTowerPerformance()) }
    }

    fun toggleSynergyPanel() = _uiState.update { it.copy(isSynergyPanelExpanded = !it.isSynergyPanelExpanded) }

    fun setSoundProfile(profile: SoundProfile) {
        soundManager.activeProfile = profile
        _uiState.update { it.copy(soundProfile = profile) }
    }

    fun setMusicTrack(track: MusicTrack) {
        val bgmId = com.example.audio.BuildArchiveCatalog.resolveBgmTrack(
            when (track) {
                MusicTrack.NONE -> ""
                MusicTrack.ROLLING_PLAYLIST -> "default_bgm_rolling_playlist"
                MusicTrack.CYBER_AMBIENT -> "default_bgm_cyber_ambient"
                else -> "b60_bgm_${track.name.lowercase()}"
            }
        ).id
        val resolvedId = if (track == MusicTrack.NONE) "" else bgmId
        _uiState.update { prev ->
            val updated = prev.balanceConfig.copy(musicTrackName = track.name, bgmTrackId = resolvedId)
            telemetryManager.saveBalanceConfig(updated)
            prev.copy(musicTrack = track, bgmTrackId = resolvedId, balanceConfig = updated)
        }
        if (track == MusicTrack.NONE) soundManager.activeMusicTrack = track else soundManager.playBgmByAssetId(resolvedId)
        assetVaultCoordinator.syncEquippedFromState()
    }

    fun setMasterVolume(level: Float) = updateSlotSetting { cfg, st -> st.copy(masterVolume = level, balanceConfig = cfg.copy(masterVolume = level)) }.also { soundManager.setMasterVolumeLevel(level) }
    fun setSfxVolume(level: Float) = updateSlotSetting { cfg, st -> st.copy(sfxVolume = level, balanceConfig = cfg.copy(sfxVolume = level)) }.also { soundManager.setSfxVolumeLevel(level) }
    fun setUpgradeVolume(level: Float) = updateSlotSetting { cfg, st -> st.copy(upgradeVolume = level, balanceConfig = cfg.copy(upgradeVolume = level)) }.also { soundManager.setUpgradeVolumeLevel(level) }
    fun setMusicVolume(level: Float) = updateSlotSetting { cfg, st -> st.copy(musicVolume = level, balanceConfig = cfg.copy(musicVolume = level)) }.also { soundManager.setMusicVolumeLevel(level) }

    fun toggleAudioMute(): Boolean {
        val newMuted = soundManager.toggleMute()
        updateSlotSetting { cfg, st -> st.copy(isAudioMuted = newMuted, balanceConfig = cfg.copy(isMuted = newMuted)) }
        return newMuted
    }

    fun setAudioMuted(muted: Boolean) {
        soundManager.setMutedState(muted)
        updateSlotSetting { cfg, st -> st.copy(isAudioMuted = muted, balanceConfig = cfg.copy(isMuted = muted)) }
    }

    private inline fun updateSlotSetting(crossinline transform: (DevBalanceConfig, GameUIState) -> GameUIState) {
        _uiState.update { prev -> transform(prev.balanceConfig, prev).also { telemetryManager.saveBalanceConfig(it.balanceConfig) } }
        assetVaultCoordinator.syncEquippedFromState()
    }

    fun setKineticSfxSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveFireId(TowerClass.KINETIC_BALLISTIC, c)
        updateSlotSetting { cfg, st -> st.copy(kineticSfxSlot = c, kineticFireId = id, balanceConfig = cfg.copy(kineticSfxSlot = c, kineticFireId = id)) }
    }
    fun setElementalSfxSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveFireId(TowerClass.ELEMENTAL_VOID, c)
        updateSlotSetting { cfg, st -> st.copy(elementalSfxSlot = c, elementalFireId = id, balanceConfig = cfg.copy(elementalSfxSlot = c, elementalFireId = id)) }
    }
    fun setSiegeSfxSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveFireId(TowerClass.HEAVY_SIEGE, c)
        updateSlotSetting { cfg, st -> st.copy(siegeSfxSlot = c, siegeFireId = id, balanceConfig = cfg.copy(siegeSfxSlot = c, siegeFireId = id)) }
    }
    fun setStellarSfxSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveFireId(TowerClass.STELLAR_COSMIC, c)
        updateSlotSetting { cfg, st -> st.copy(stellarSfxSlot = c, stellarFireId = id, balanceConfig = cfg.copy(stellarSfxSlot = c, stellarFireId = id)) }
    }
    fun setTurretSkinSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveSkinId(c)
        updateSlotSetting { cfg, st -> st.copy(turretSkinSlot = c, turretSkinId = id, balanceConfig = cfg.copy(turretSkinSlot = c, turretSkinId = id)) }
    }
    fun setMapThemeSlot(slot: Int) = slot.coerceIn(1, 5).let { c ->
        val id = com.example.audio.BuildArchiveCatalog.buildActiveThemeId(c)
        updateSlotSetting { cfg, st -> st.copy(mapThemeSlot = c, mapThemeId = id, balanceConfig = cfg.copy(mapThemeSlot = c, mapThemeId = id)) }
    }

    fun playTurretTestSound(towerClass: TowerClass, slot: Int) = soundManager.playTurretTestSound(towerClass, slot)
    fun playTowerUpgradeTestSound(towerClass: TowerClass, slot: Int) = soundManager.playTowerUpgradeTestSound(towerClass, slot)
    fun playTestSound(fx: SoundFx) = soundManager.play(fx)

    fun startNewRun() {
        runSessionId++
        leaderboardRepository.resetRunSubmissionState()
        simulationCoordinator.resetVirtualTime()
        devTunedThisRun = false
        gameLoopJob?.cancel()

        val seed = System.currentTimeMillis()
        val balance = _uiState.value.balanceConfig
        val level = MapGenerator.generate(
            seed = seed,
            width = balance.mapColumns,
            height = balance.mapRows,
            minLaneLength = balance.minLaneLength,
            maxLaneLength = balance.maxLaneLength,
            targetOpenSlots = balance.getTargetOpenSlotsForDifficulty(_uiState.value.difficulty),
            minLaneOverlap = balance.minLaneOverlap,
            maxLaneOverlap = balance.maxLaneOverlap
        )
        val firstWave = WaveGenerator.generateWave(1, balance.enemiesPerRoundMultiplier, _uiState.value.difficulty)

        val initialParticles = createInitialTrackParticles(level.laneAlpha, level.laneBeta)

        _uiState.update { prev ->
            prev.copy(
                seed = seed, level = level, towers = emptyList(), enemies = emptyList(),
                projectiles = emptyList(), laserBeams = emptyList(), lightningArcs = emptyList(),
                explosions = emptyList(), floatingTexts = emptyList(), trackParticles = initialParticles,
                activeGlobalSynergies = emptyList(), activeBoardSynergies = emptyList(), isSynergyPanelExpanded = false, animTimeMs = 0L,
                harmonicGridDamageBoost = remoteConfigManager.harmonicGridDamageBoost,
                globalKineticSpeedBonus = 0f, ascensionFrostSlowBonus = 0f, globalFrostSlowBonus = 0f, globalTeslaArcsEnabled = false,
                globalMortarSplashBonus = 0f, globalSolarCritsEnabled = false, globalVoidShredEnabled = false,
                globalDamageBonus = 1.0f, scrapBountyBonus = 1.0f, coreHp = 30, maxCoreHp = 30,
                scrap = prev.balanceConfig.startingScrap, waveNumber = 1, currentWaveData = firstWave,
                isWaveActive = false, gameSpeed = GameSpeed.NORMAL, selectedTile = null, selectedTower = null, lastSelectedTowerId = null,
                isGameOver = false, isVictory = false, totalKills = 0, totalScrapEarned = prev.balanceConfig.startingScrap,
                totalDamageDealt = 0f, screenShakeIntensity = 0f, autoCallNextWave = false, overchargeRemainingMs = 0L,
                isTelemetryOpen = false, isSettingsOpen = false, isPauseMenuOpen = false, isAssetVaultOpen = false, hasClickedFirstTile = false,
                collapseWaves = emptyList(), fatalEnemyType = "None"
            )
        }

        waveCoordinator.clearPendingSpawns()
        simAccumulatorSec = 0f
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val frameStartMs = System.currentTimeMillis()
                val now = frameStartMs
                val deltaMs = (now - lastTime).coerceAtMost(50L)
                lastTime = now
                soundManager.updateTick(now, deltaMs)

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
                    for (i in 0 until count) { val diff = frameTimes[i] - avgDelta; sumSq += diff * diff }
                    frameJitterMs = Math.sqrt(sumSq / count).toFloat()
                }

                val speed = _uiState.value.gameSpeed.multiplier
                if (speed > 0f && !_uiState.value.isGameOver && !_uiState.value.isVictory && !isInternalDialogActive()) {
                    val effectiveSpeed = speed * BASE_SIM_SPEED_MULTIPLIER
                    simAccumulatorSec += (deltaMs / 1000f) * effectiveSpeed
                    val desiredSteps = (simAccumulatorSec / FIXED_SIM_STEP_SEC).toInt()
                    val steps = desiredSteps.coerceAtMost(MAX_PHYSICS_SUBSTEPS)
                    if (steps > 0) {
                        simAccumulatorSec -= steps * FIXED_SIM_STEP_SEC
                        if (desiredSteps > MAX_PHYSICS_SUBSTEPS) {
                            simAccumulatorSec = simAccumulatorSec.coerceAtMost(FIXED_SIM_STEP_SEC * 2f)
                        }
                        for (stepIdx in 0 until steps) {
                            val s = _uiState.value
                            if (s.isGameOver || s.isVictory || isInternalDialogActive()) {
                                simAccumulatorSec = 0f
                                break
                            }
                            simulationCoordinator.tickSimulation(
                                deltaSec = FIXED_SIM_STEP_SEC,
                                deltaMs = FIXED_SIM_STEP_MS,
                                nowMs = now,
                                publishUiState = (stepIdx == steps - 1)
                            )
                        }
                    } else {
                        _uiState.update { it.copy(animTimeMs = now) }
                    }
                } else {
                    simAccumulatorSec = 0f
                    _uiState.update { it.copy(animTimeMs = now) }
                }
                val workMs = System.currentTimeMillis() - frameStartMs
                delay((16L - workMs).coerceAtLeast(1L))
            }
        }
    }

    // === DELEGATIONS TO WAVE PROGRESSION & PLACEMENT COORDINATORS ===
    fun deployWave() = waveCoordinator.deployWave()
    fun spawnSandboxWave(enemyType: EnemyType, count: Int) = waveCoordinator.spawnSandboxWave(enemyType, count)
    fun selectTile(tile: MapTile?) = placementCoordinator.selectTile(tile)
    fun buildTower(tile: MapTile, towerClass: TowerClass = TowerClass.KINETIC_BALLISTIC) = placementCoordinator.buildTower(tile, towerClass)
    fun buildTower(tile: MapTile, type: TowerType) = placementCoordinator.buildTower(tile, TowerClass.KINETIC_BALLISTIC)
    fun upgradeTowerToVector(tower: TowerInstance, vector: TowerVector) = placementCoordinator.upgradeTowerToVector(tower, vector)
    fun upgradeTowerToArchetype(tower: TowerInstance, archetype: TowerArchetype) = placementCoordinator.upgradeTowerToArchetype(tower, archetype)
    fun upgradeTowerToTier3Global(tower: TowerInstance, auraIndex: Int = 0) = placementCoordinator.upgradeTowerToTier3Global(tower, auraIndex)
    fun upgradeTowerToTier1(tower: TowerInstance, branchKey: String) = placementCoordinator.upgradeTowerToTier1(tower, branchKey)
    fun upgradeTowerToFinalForm(tower: TowerInstance, finalFormCode: String) = placementCoordinator.upgradeTowerToFinalForm(tower, finalFormCode)
    fun upgradeTowerPath(tower: TowerInstance, pathIndex: Int) = placementCoordinator.upgradeTowerPath(tower, pathIndex)
    fun ascendTower(tower: TowerInstance) = placementCoordinator.ascendTower(tower)
    fun swapBoosterPads() = placementCoordinator.swapBoosterPads()
    fun sellSelectedTower() = placementCoordinator.sellSelectedTower()
    fun changeSelectedTowerPriority(priority: TargetPriority) = placementCoordinator.changeSelectedTowerPriority(priority)
    fun canInitiateSingularity(): Boolean = placementCoordinator.canInitiateSingularity()
    fun getSingularityAnchorTower(): TowerInstance? = placementCoordinator.getSingularityAnchorTower()
    fun requestSingularityCollapse() = placementCoordinator.requestSingularityCollapse()
    fun dismissSingularityWarning() = placementCoordinator.dismissSingularityWarning()
    fun confirmSingularityCollapse() = placementCoordinator.confirmSingularityCollapse(simulationCoordinator::computeActiveSynergies)

    fun setGameDifficulty(difficulty: GameDifficulty) {
        val oldDifficulty = _uiState.value.difficulty
        if (!_uiState.value.isWaveActive) {
            _uiState.update { it.copy(difficulty = difficulty) }
            val state = _uiState.value
            if (!state.isWaveActive && state.waveNumber == 1 && state.towers.isEmpty() && oldDifficulty != difficulty) {
                regeneratePreWave1Level(state.balanceConfig, difficulty)
            }
        }
    }

    private fun recordRunCompletion(isVictory: Boolean, waves: Int) {
        val state = _uiState.value
        if (waves <= 0 && state.totalKills <= 0 && state.totalDamageDealt <= 0f) return

        val mvp = state.towers.maxByOrNull { it.totalDamage }?.let { it.archetype?.displayName ?: it.type.displayName } ?: "Vulcan Minigun"
        val playerName = telemetryManager.getActivePlayerName()
        val isDev = devTunedThisRun || state.balanceConfig.isGameplayModified()
        val totalCollapses = maxOf(
            state.collapseWaves.size,
            state.towers.filter { it.isCapstone }.maxOfOrNull { it.generation } ?: 0
        )
        val uniqueAurasBanked = state.towers
            .filter { it.isCapstone }
            .flatMap { it.synergyVault }
            .flatMap { it.synergies }
            .map { it.auraName }
            .distinct()
            .size
        val fatalType = if (isVictory) "None" else state.fatalEnemyType

        val record = RunRecord(
            id = UUID.randomUUID().toString(), seed = state.seed,
            sectorName = state.level?.biome?.name ?: "Sector Zero", playerName = playerName,
            wavesCleared = waves, enemiesKilled = state.totalKills, totalDamage = state.totalDamageDealt,
            scrapEarned = state.totalScrapEarned, ascendedTowersCount = state.towers.count { it.isAscended },
            mvpTower = mvp, isVictory = isVictory, timestamp = System.currentTimeMillis(), isDevTuned = isDev,
            totalCollapses = totalCollapses,
            unspentGoldAtEnd = state.scrap,
            uniqueAurasBanked = uniqueAurasBanked,
            fatalEnemyType = fatalType,
            collapseWaves = state.collapseWaves
        )

        if (!isVictory) {
            val mvpClass = state.towers.maxByOrNull { it.totalDamage }?.towerClass?.name ?: "KINETIC_BALLISTIC"
            telemetryManager.logLevelFail(waves + 1, state.towers.size, state.scrap, mvpClass)
        }

        if (record.score > 0 || record.wavesCleared > 0 || record.enemiesKilled > 0) {
            _uiState.update { it.copy(lastRunRecord = record) }
        }

        val completionSessionId = runSessionId
        lastRecordRunJob = viewModelScope.launch(Dispatchers.IO) {
            val latestCallsign = FirestoreLeaderboardApi.sanitizeCallsign(telemetryManager.getActivePlayerName())
            val recordToSave = record.copy(playerName = latestCallsign)
            telemetryManager.recordRun(recordToSave)
            telemetryManager.recordTowerSession(state.towers)
            refreshTelemetry()
            val uid = authRepository.ensureAuthenticated() ?: authRepository.currentUserId
            val enqueueCallsign = FirestoreLeaderboardApi.sanitizeCallsign(telemetryManager.getActivePlayerName())
            leaderboardRepository.enqueueRun(
                run = recordToSave,
                callsign = enqueueCallsign,
                isDevTuned = isDev,
                userId = uid,
                isCurrentRun = (completionSessionId == runSessionId)
            )
            // Fire-and-forget whole-run balance telemetry upload using leaderboardRepository.api
            viewModelScope.launch(Dispatchers.IO) {
                runCatching<Unit> {
                    val idToken = authRepository.getCachedIdTokenIfValid()
                    leaderboardRepository.api.submitRunTelemetry(
                        record = recordToSave,
                        difficultyId = state.difficulty.id,
                        towerPerf = telemetryManager.getTowerPerformance(),
                        idToken = idToken
                    )
                }
                leaderboardRepository.fetchLeaderboard(forceRefresh = true)
            }
        }
    }

    fun setPlayerName(name: String) {
        val trimmed = name.trim()
        telemetryManager.updateLastRunPlayerName(trimmed)
        refreshTelemetry()
        val prevJob = lastSetPlayerNameJob
        lastSetPlayerNameJob = viewModelScope.launch(Dispatchers.IO) {
            lastRecordRunJob?.join()
            prevJob?.join()
            telemetryManager.updateLastRunPlayerName(trimmed)
            refreshTelemetry()
            val sanitizedCallsign = FirestoreLeaderboardApi.sanitizeCallsign(telemetryManager.getActivePlayerName())
            val lastRunId = _uiState.value.lastRunRecord?.id ?: telemetryManager.getLastRun()?.id
            if (!lastRunId.isNullOrBlank()) {
                leaderboardRepository.updatePendingCallsign(lastRunId, sanitizedCallsign)
            }
        }
    }
    fun toggleAutoWave() = _uiState.update { it.copy(autoCallNextWave = !it.autoCallNextWave) }
    fun setGameSpeed(speed: GameSpeed) = _uiState.update { current ->
        if (current.isPauseMenuOpen || current.showEndRunConfirmDialog) current.copy(prePauseSpeed = speed, gameSpeed = GameSpeed.PAUSED)
        else current.copy(gameSpeed = speed, prePauseSpeed = speed)
    }

    fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean = false) {
        _uiState.update { it.copy(floatingTexts = it.floatingTexts + FloatingText(id = nextId("ft_"), x = x, y = y, text = text, color = color, isCrit = isCrit)) }
    }

    fun launchCinematic(scenario: CinematicScenario) = cinematicController.launchCinematic(scenario)
    fun stopCinematic() = cinematicController.stopCinematic()
    fun jumpToSignatureWave(scenario: CinematicScenario) = cinematicController.jumpToSignatureWave(scenario)
}
