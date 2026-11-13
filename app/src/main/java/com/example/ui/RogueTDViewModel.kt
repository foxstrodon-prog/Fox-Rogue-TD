package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicTrack
import com.example.audio.SoundFx
import com.example.audio.SoundManager
import com.example.audio.SoundProfile
import com.example.engine.*
import com.example.engine.coordinator.SimulationCoordinator
import com.example.engine.coordinator.TowerPlacementCoordinator
import com.example.engine.coordinator.WaveProgressionCoordinator
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
    val soundManager = SoundManager(application)
    val cinematicController = CinematicController(this)

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
        addFloatingText = ::addFloatingText, onRecordRunCompletion = ::recordRunCompletion
    )

    val placementCoordinator = TowerPlacementCoordinator(
        uiStateFlow = _uiState, soundManager = soundManager, telemetryManager = telemetryManager,
        nextId = ::nextId, addFloatingText = ::addFloatingText, onSynergyDirty = ::markSynergyDirty
    )

    internal fun markSynergyDirty() { simulationCoordinator.isSynergyCacheDirty = true }
    internal fun clearPendingSpawns() { waveCoordinator.clearPendingSpawns() }
    var currentFps: Int = 60; private set
    var frameJitterMs: Float = 0.5f; private set
    private val frameTimes = LongArray(30)
    private var frameCount = 0

    init {
        remoteConfigManager.initialize()
        telemetryManager.logTutorialStart()
        viewModelScope.launch(Dispatchers.IO) { authRepository.ensureAuthenticated() }

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
                                if (boardState.pendingCount > 0) "Offline Queue: ${boardState.pendingCount} score(s) pending sync"
                                else boardState.failureKind?.name ?: "Connection error"
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
                    prev.copy(updateUIState = updateState, showUpdateModal = shouldOpenModal)
                }
            }
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
        viewModelScope.launch { leaderboardRepository.fetchLeaderboard(forceRefresh = forceRefresh) }
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

    fun toggleTelemetryView(open: Boolean, initialTab: Int = 0) {
        if (open) { refreshTelemetry(); fetchOnlineLeaderboard(forceRefresh = true) }
        _uiState.update { it.copy(isTelemetryOpen = open, telemetryInitialTab = initialTab) }
    }

    fun toggleSettingsView(open: Boolean) {
        if (open) refreshTelemetry()
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun openTelemetryFromPauseMenu() {
        refreshTelemetry()
        fetchOnlineLeaderboard(forceRefresh = true)
        _uiState.update { it.copy(isPauseMenuOpen = false, isTelemetryOpen = true, telemetryInitialTab = 2) }
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
        val wavesCompleted = if (state.isWaveActive) maxOf(0, state.waveNumber - 1) else state.waveNumber
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
        if (newConfig.isGameplayModified()) devTunedThisRun = true
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
                balanceConfig = defaultConfig, masterVolume = defaultConfig.masterVolume,
                musicVolume = defaultConfig.musicVolume, sfxVolume = defaultConfig.sfxVolume,
                upgradeVolume = defaultConfig.upgradeVolume, isAudioMuted = defaultConfig.isMuted,
                kineticSfxSlot = defaultConfig.kineticSfxSlot, elementalSfxSlot = defaultConfig.elementalSfxSlot,
                siegeSfxSlot = defaultConfig.siegeSfxSlot, turretSkinSlot = defaultConfig.turretSkinSlot,
                mapThemeSlot = defaultConfig.mapThemeSlot
            )
        }
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

    fun playTurretTestSound(towerClass: TowerClass, slot: Int) = soundManager.playTurretTestSound(towerClass, slot)
    fun playTowerUpgradeTestSound(towerClass: TowerClass, slot: Int) = soundManager.playTowerUpgradeTestSound(towerClass, slot)
    fun setTowerVisualSkin(skin: String) = _uiState.update { it.copy(towerVisualSkin = skin) }
    fun playTestSound(fx: SoundFx) = soundManager.play(fx)

    fun startNewRun() {
        simulationCoordinator.resetVirtualTime()
        devTunedThisRun = false
        gameLoopJob?.cancel()

        val seed = System.currentTimeMillis()
        val balance = _uiState.value.balanceConfig
        val level = MapGenerator.generate(seed = seed, minLaneLength = balance.minLaneLength, maxLaneLength = balance.maxLaneLength)
        val firstWave = WaveGenerator.generateWave(1, balance.enemiesPerRoundMultiplier, _uiState.value.difficulty)

        val initialParticles = ArrayList<TrackParticle>(12)
        for (i in 0 until 12) {
            val lane = i % 2
            val path = if (lane == 0) level.laneAlpha else level.laneBeta
            val maxSeg = (path.size - 2).coerceAtLeast(0)
            initialParticles.add(
                TrackParticle(
                    id = "tp_$i", laneIndex = lane, segmentIndex = (i * 2) % (maxSeg + 1),
                    progress = (i * 0.15f) % 1.0f, speed = 0.8f + (i % 3) * 0.2f,
                    color = if (lane == 0) 0xFF00F0FF else 0xFFFFB800, size = 2.5f + (i % 2) * 1.5f
                )
            )
        }

        _uiState.update { prev ->
            prev.copy(
                seed = seed, level = level, towers = emptyList(), enemies = emptyList(),
                projectiles = emptyList(), laserBeams = emptyList(), lightningArcs = emptyList(),
                explosions = emptyList(), floatingTexts = emptyList(), trackParticles = initialParticles,
                activeGlobalSynergies = emptyList(), isSynergyPanelExpanded = false, animTimeMs = 0L,
                globalKineticSpeedBonus = 0f, globalFrostSlowBonus = 0f, globalTeslaArcsEnabled = false,
                globalMortarSplashBonus = 0f, globalSolarCritsEnabled = false, globalVoidShredEnabled = false,
                globalDamageBonus = 1.0f, scrapBountyBonus = 1.0f, coreHp = 30, maxCoreHp = 30,
                scrap = prev.balanceConfig.startingScrap, waveNumber = 1, currentWaveData = firstWave,
                isWaveActive = false, gameSpeed = GameSpeed.NORMAL, selectedTile = null, selectedTower = null,
                isGameOver = false, isVictory = false, totalKills = 0, totalScrapEarned = prev.balanceConfig.startingScrap,
                totalDamageDealt = 0f, screenShakeIntensity = 0f, autoCallNextWave = false, overchargeRemainingMs = 0L,
                isTelemetryOpen = false, isSettingsOpen = false, isPauseMenuOpen = false, hasClickedFirstTile = false
            )
        }

        waveCoordinator.clearPendingSpawns()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val deltaMs = (now - lastTime).coerceAtMost(50L)
                lastTime = now

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
                val simDeltaSec = (deltaMs / 1000f) * (if (speed > 0f) speed else 1.0f)
                if (speed > 0f && !_uiState.value.isGameOver && !isInternalDialogActive()) {
                    simulationCoordinator.tickSimulation(simDeltaSec, (deltaMs * speed).toLong(), now)
                } else {
                    _uiState.update { it.copy(animTimeMs = now) }
                }
                delay(16)
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
        if (!_uiState.value.isWaveActive) _uiState.update { it.copy(difficulty = difficulty) }
    }

    private fun recordRunCompletion(isVictory: Boolean, waves: Int) {
        val state = _uiState.value
        if (waves <= 0 && state.totalKills <= 0 && state.totalDamageDealt <= 0f) return

        val mvp = state.towers.maxByOrNull { it.totalDamage }?.let { it.archetype?.displayName ?: it.type.displayName } ?: "Vulcan Minigun"
        val playerName = telemetryManager.getActivePlayerName()
        val isDev = devTunedThisRun || state.balanceConfig.isGameplayModified()
        val record = RunRecord(
            id = UUID.randomUUID().toString(), seed = state.seed,
            sectorName = state.level?.biome?.name ?: "Sector Zero", playerName = playerName,
            wavesCleared = waves, enemiesKilled = state.totalKills, totalDamage = state.totalDamageDealt,
            scrapEarned = state.totalScrapEarned, ascendedTowersCount = state.towers.count { it.isAscended },
            mvpTower = mvp, isVictory = isVictory, timestamp = System.currentTimeMillis(), isDevTuned = isDev
        )

        if (!isVictory) {
            val mvpClass = state.towers.maxByOrNull { it.totalDamage }?.towerClass?.name ?: "KINETIC_BALLISTIC"
            telemetryManager.logLevelFail(waves + 1, state.towers.size, state.scrap, mvpClass)
        }

        viewModelScope.launch(Dispatchers.IO) {
            telemetryManager.recordRun(record)
            telemetryManager.recordTowerSession(state.towers)
            refreshTelemetry()
            leaderboardRepository.enqueueRun(record, playerName, isDev, authRepository.currentUserId)
            leaderboardRepository.fetchLeaderboard(forceRefresh = true)
        }
    }

    fun setPlayerName(name: String) { telemetryManager.updateLastRunPlayerName(name); refreshTelemetry() }
    fun toggleAutoWave() = _uiState.update { it.copy(autoCallNextWave = !it.autoCallNextWave) }
    fun setGameSpeed(speed: GameSpeed) = _uiState.update { current ->
        if (current.isPauseMenuOpen || current.showEndRunConfirmDialog) current.copy(prePauseSpeed = speed, gameSpeed = GameSpeed.PAUSED)
        else current.copy(gameSpeed = speed, prePauseSpeed = speed)
    }

    internal fun addFloatingText(x: Float, y: Float, text: String, color: Long, isCrit: Boolean = false) {
        _uiState.update { it.copy(floatingTexts = it.floatingTexts + FloatingText(id = nextId("ft_"), x = x, y = y, text = text, color = color, isCrit = isCrit)) }
    }

    fun launchCinematic(scenario: CinematicScenario) = cinematicController.launchCinematic(scenario)
    fun stopCinematic() = cinematicController.stopCinematic()
    fun jumpToSignatureWave(scenario: CinematicScenario) = cinematicController.jumpToSignatureWave(scenario)
}
