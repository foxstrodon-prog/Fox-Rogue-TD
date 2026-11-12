package com.example.ui

import com.example.model.*
import kotlinx.coroutines.flow.update

/**
 * Controller for deterministic 30-second promotional and diagnostic gameplay showcases.
 * Extracted from RogueTDViewModel to preserve single responsibility and maintain the 750-line modularity threshold.
 */
class CinematicController(private val vm: RogueTDViewModel) {

    fun launchCinematic(scenario: CinematicScenario) {
        val level = vm.uiState.value.level ?: return
        val emptyTiles = level.tiles.filter { it.type == TileType.EMPTY }
        if (emptyTiles.size < 4) return

        val towers = mutableListOf<TowerInstance>()
        val enemies = mutableListOf<EnemyInstance>()
        val waveNum = scenario.targetWave

        when (scenario) {
            CinematicScenario.CIN_01_BOSS_MELT -> {
                val boosterTiles = level.tiles.filter { it.buff != TileBuff.NONE }.take(4)
                val targetTiles = if (boosterTiles.size >= 4) boosterTiles else emptyTiles.take(4)
                towers.add(
                    TowerInstance(
                        id = "cin_stellar_1",
                        gridPos = targetTiles[0].pos,
                        towerClass = TowerClass.STELLAR_COSMIC,
                        tier = 3,
                        archetype = TowerArchetype.SUPERNOVA_PULSAR,
                        branchTier1 = "A",
                        finalFormCode = "A.a"
                    )
                )
                towers.add(
                    TowerInstance(
                        id = "cin_stellar_2",
                        gridPos = targetTiles[1].pos,
                        towerClass = TowerClass.STELLAR_COSMIC,
                        tier = 3,
                        archetype = TowerArchetype.GRAVITON_LANCE,
                        branchTier1 = "A",
                        finalFormCode = "A.b"
                    )
                )
                towers.add(
                    TowerInstance(
                        id = "cin_gatling_1",
                        gridPos = targetTiles[2].pos,
                        towerClass = TowerClass.KINETIC_BALLISTIC,
                        tier = 3,
                        archetype = TowerArchetype.VULCAN_GATLING,
                        branchTier1 = "A",
                        finalFormCode = "A.a"
                    )
                )
                towers.add(
                    TowerInstance(
                        id = "cin_gatling_2",
                        gridPos = targetTiles[3].pos,
                        towerClass = TowerClass.KINETIC_BALLISTIC,
                        tier = 3,
                        archetype = TowerArchetype.FLAK_CANNON,
                        branchTier1 = "A",
                        finalFormCode = "A.c"
                    )
                )

                val startAlpha = level.laneAlpha.first()
                enemies.add(
                    EnemyInstance(
                        id = "cin_boss_1",
                        type = EnemyType.BOSS,
                        laneIndex = 0,
                        currentPathSegmentIndex = 0,
                        segmentProgress = 0f,
                        maxHp = 25000f,
                        currentHp = 25000f,
                        speed = 0.45f,
                        worldX = startAlpha.x.toFloat(),
                        worldY = startAlpha.y.toFloat(),
                        isBoss = true
                    )
                )

                val startBeta = level.laneBeta.first()
                for (i in 1..25) {
                    enemies.add(
                        EnemyInstance(
                            id = "cin_drone_$i",
                            type = EnemyType.DRONE,
                            laneIndex = if (i % 2 == 0) 0 else 1,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.25f),
                            maxHp = 450f,
                            currentHp = 450f,
                            speed = 1.2f,
                            worldX = if (i % 2 == 0) startAlpha.x.toFloat() else startBeta.x.toFloat(),
                            worldY = if (i % 2 == 0) startAlpha.y.toFloat() else startBeta.y.toFloat(),
                            isBoss = false
                        )
                    )
                }
            }

            CinematicScenario.CIN_02_BULLET_HELL -> {
                val tilesToUse = emptyTiles.take(8)
                tilesToUse.forEachIndexed { idx, tile ->
                    towers.add(
                        TowerInstance(
                            id = "cin_gatling_$idx",
                            gridPos = tile.pos,
                            towerClass = TowerClass.KINETIC_BALLISTIC,
                            tier = 2,
                            archetype = if (idx % 2 == 0) TowerArchetype.VULCAN_GATLING else TowerArchetype.FLAK_CANNON,
                            branchTier1 = "A"
                        )
                    )
                }

                val startAlpha = level.laneAlpha.first()
                val startBeta = level.laneBeta.first()
                for (i in 0..79) {
                    val lane = i % 2
                    val start = if (lane == 0) startAlpha else startBeta
                    enemies.add(
                        EnemyInstance(
                            id = "cin_swarm_$i",
                            type = if (i % 3 == 0) EnemyType.SPEEDSTER else EnemyType.DRONE,
                            laneIndex = lane,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.18f),
                            maxHp = 280f,
                            currentHp = 280f,
                            speed = 1.6f,
                            worldX = start.x.toFloat(),
                            worldY = start.y.toFloat(),
                            isBoss = false
                        )
                    )
                }
            }

            CinematicScenario.CIN_03_BOOSTER_WARP -> {
                val padTiles = level.tiles.filter { it.buff != TileBuff.NONE }.take(6)
                val archetypes = listOf(
                    TowerArchetype.SUPERNOVA_PULSAR,
                    TowerArchetype.VULCAN_GATLING,
                    TowerArchetype.FROST_SPIRE,
                    TowerArchetype.HELLFIRE_MORTAR,
                    TowerArchetype.TESLA_STORMCOIL,
                    TowerArchetype.PLASMA_PULSAR
                )
                padTiles.forEachIndexed { idx, tile ->
                    towers.add(
                        TowerInstance(
                            id = "cin_warp_$idx",
                            gridPos = tile.pos,
                            towerClass = when (idx % 4) {
                                0 -> TowerClass.STELLAR_COSMIC
                                1 -> TowerClass.KINETIC_BALLISTIC
                                2 -> TowerClass.ELEMENTAL_VOID
                                else -> TowerClass.HEAVY_SIEGE
                            },
                            tier = 2,
                            archetype = archetypes.getOrElse(idx) { TowerArchetype.SUPERNOVA_PULSAR },
                            branchTier1 = "A"
                        )
                    )
                }

                val startAlpha = level.laneAlpha.first()
                val startBeta = level.laneBeta.first()
                for (i in 0..50) {
                    val lane = i % 2
                    val start = if (lane == 0) startAlpha else startBeta
                    enemies.add(
                        EnemyInstance(
                            id = "cin_warp_creep_$i",
                            type = EnemyType.SOLDIER,
                            laneIndex = lane,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.22f),
                            maxHp = 600f,
                            currentHp = 600f,
                            speed = 0.9f,
                            worldX = start.x.toFloat(),
                            worldY = start.y.toFloat(),
                            isBoss = false
                        )
                    )
                }
            }

            CinematicScenario.CIN_04_ELEMENTAL_CASCADE -> {
                val tilesToUse = emptyTiles.take(6)
                tilesToUse.forEachIndexed { idx, tile ->
                    towers.add(
                        TowerInstance(
                            id = "cin_elem_$idx",
                            gridPos = tile.pos,
                            towerClass = TowerClass.ELEMENTAL_VOID,
                            tier = 3,
                            archetype = if (idx % 2 == 0) TowerArchetype.FROST_SPIRE else TowerArchetype.GRAVITON_VOID,
                            branchTier1 = if (idx % 2 == 0) "A" else "B",
                            finalFormCode = if (idx % 2 == 0) "A.a" else "B.a"
                        )
                    )
                }

                val startAlpha = level.laneAlpha.first()
                val startBeta = level.laneBeta.first()
                for (i in 0..60) {
                    val lane = i % 2
                    val start = if (lane == 0) startAlpha else startBeta
                    enemies.add(
                        EnemyInstance(
                            id = "cin_elem_creep_$i",
                            type = if (i % 5 == 0) EnemyType.BRUISER else EnemyType.SOLDIER,
                            laneIndex = lane,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.20f),
                            maxHp = 950f,
                            currentHp = 950f,
                            speed = 0.85f,
                            worldX = start.x.toFloat(),
                            worldY = start.y.toFloat(),
                            isBoss = false
                        )
                    )
                }
            }

            CinematicScenario.CIN_05_KINETIC_GATLING -> {
                val tilesToUse = emptyTiles.take(4)
                tilesToUse.forEachIndexed { idx, tile ->
                    towers.add(
                        TowerInstance(
                            id = "cin_gatling_apex_$idx",
                            gridPos = tile.pos,
                            towerClass = TowerClass.KINETIC_BALLISTIC,
                            tier = 3,
                            archetype = TowerArchetype.VULCAN_GATLING,
                            branchTier1 = "A",
                            finalFormCode = "A.a"
                        )
                    )
                }

                val startAlpha = level.laneAlpha.first()
                for (i in 0..2) {
                    enemies.add(
                        EnemyInstance(
                            id = "cin_behemoth_$i",
                            type = EnemyType.BRUISER,
                            laneIndex = 0,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.8f),
                            maxHp = 6000f,
                            currentHp = 6000f,
                            speed = 0.6f,
                            worldX = startAlpha.x.toFloat(),
                            worldY = startAlpha.y.toFloat(),
                            isBoss = true
                        )
                    )
                }
            }

            CinematicScenario.CIN_06_EVOLUTION_MONTAGE -> {
                val tilesToUse = emptyTiles.take(4)
                val classes = listOf(
                    TowerClass.KINETIC_BALLISTIC,
                    TowerClass.ELEMENTAL_VOID,
                    TowerClass.HEAVY_SIEGE,
                    TowerClass.STELLAR_COSMIC
                )
                tilesToUse.forEachIndexed { idx, tile ->
                    towers.add(
                        TowerInstance(
                            id = "cin_evo_$idx",
                            gridPos = tile.pos,
                            towerClass = classes[idx],
                            tier = 0
                        )
                    )
                }

                val startAlpha = level.laneAlpha.first()
                val startBeta = level.laneBeta.first()
                for (i in 0..40) {
                    val lane = i % 2
                    val start = if (lane == 0) startAlpha else startBeta
                    enemies.add(
                        EnemyInstance(
                            id = "cin_evo_creep_$i",
                            type = EnemyType.SOLDIER,
                            laneIndex = lane,
                            currentPathSegmentIndex = 0,
                            segmentProgress = -(i * 0.25f),
                            maxHp = 500f,
                            currentHp = 500f,
                            speed = 0.9f,
                            worldX = start.x.toFloat(),
                            worldY = start.y.toFloat(),
                            isBoss = false
                        )
                    )
                }
            }
        }

        vm.clearPendingSpawns()
        vm.markSynergyDirty()
        vm._uiState.update {
            it.copy(
                waveNumber = waveNum,
                towers = towers,
                enemies = enemies,
                projectiles = emptyList(),
                explosions = emptyList(),
                floatingTexts = emptyList(),
                isWaveActive = true,
                gameSpeed = GameSpeed.NORMAL,
                activeCinematic = ActiveCinematicState(scenario = scenario, currentFrame = 0, totalFrames = 1800),
                isSettingsOpen = false
            )
        }
        vm.addFloatingText(8f, 4f, "🎬 START: ${scenario.title} (30s)", 0xFFFFD700)
    }

    fun processFrame(cin: ActiveCinematicState, state: GameUIState): Boolean {
        val nextFrame = cin.currentFrame + 1
        if (nextFrame >= cin.totalFrames) {
            stopCinematic()
            return false
        }
        handleCinematicHumanGestures(cin, nextFrame, state)
        vm._uiState.update { it.copy(activeCinematic = cin.copy(currentFrame = nextFrame)) }
        return true
    }

    private fun evolveCinematicTowers(tier: Int) {
        val currentTowers = vm.uiState.value.towers.map { tower ->
            when (tier) {
                1 -> tower.copy(
                    tier = 1,
                    vector = TowerVector.OFFENSIVE,
                    branchTier1 = "A"
                )
                2 -> tower.copy(
                    tier = 2,
                    archetype = when (tower.towerClass) {
                        TowerClass.KINETIC_BALLISTIC -> TowerArchetype.VULCAN_GATLING
                        TowerClass.ELEMENTAL_VOID -> TowerArchetype.FROST_SPIRE
                        TowerClass.HEAVY_SIEGE -> TowerArchetype.HELLFIRE_MORTAR
                        TowerClass.STELLAR_COSMIC -> TowerArchetype.SUPERNOVA_PULSAR
                    }
                )
                3 -> tower.copy(
                    tier = 3,
                    finalFormCode = "A.a"
                )
                else -> tower
            }
        }
        vm.markSynergyDirty()
        vm._uiState.update { it.copy(towers = currentTowers) }
        vm.soundManager.play(com.example.audio.SoundFx.UPGRADE)
        vm.addFloatingText(8f, 5f, "★ EVOLUTION TIER $tier UNLOCKED ★", 0xFFFFB800)
    }

    private fun handleCinematicHumanGestures(cin: ActiveCinematicState, frame: Int, state: GameUIState) {
        val towers = state.towers
        when (cin.scenario) {
            CinematicScenario.CIN_01_BOSS_MELT -> {
                when (frame) {
                    120 -> {
                        towers.firstOrNull()?.let { t ->
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 TOWER FOCUS", 0xFF00F0FF)
                        }
                    }
                    480 -> {
                        towers.firstOrNull()?.let { t ->
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "🎯 PRIORITY: BOSS", 0xFFFFB800)
                        }
                    }
                    900 -> {
                        vm._uiState.update { it.copy(overchargeRemainingMs = 6000L) }
                        vm.addFloatingText(8f, 6f, "⚡ OVERCHARGE ACTIVATED ⚡", 0xFFFFD700)
                        vm.soundManager.play(com.example.audio.SoundFx.UPGRADE)
                    }
                    1380 -> {
                        vm.addFloatingText(8f, 6f, "★ APEX CORONA DETONATION ★", 0xFFFFD700)
                    }
                    1560 -> {
                        vm._uiState.update { it.copy(selectedTower = null) }
                        vm.addFloatingText(8f, 5f, "🏆 BOSS ELIMINATED", 0xFF00FF9D)
                    }
                }
            }
            CinematicScenario.CIN_02_BULLET_HELL -> {
                when (frame) {
                    150 -> {
                        val t = towers.getOrNull(1) ?: towers.firstOrNull()
                        t?.let {
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 FLAK FOCUS", 0xFF00F0FF)
                        }
                    }
                    600 -> {
                        towers.firstOrNull()?.let { t ->
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 GATLING OVERCLOCK", 0xFFFFB800)
                        }
                    }
                    1050 -> {
                        vm.addFloatingText(8f, 6f, "💥 FULL-SCREEN SATURATION", 0xFF00FF9D)
                    }
                    1260 -> {
                        vm._uiState.update { it.copy(selectedTower = null) }
                    }
                }
            }
            CinematicScenario.CIN_03_BOOSTER_WARP -> {
                if (frame == 60) {
                    towers.firstOrNull()?.let { t ->
                        vm._uiState.update { it.copy(selectedTower = t) }
                        vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 SELECT PAD 1", 0xFF00E5FF)
                    }
                }
                if (frame % 120 == 0) {
                    vm.swapBoosterPads()
                    val targetTower = vm.uiState.value.towers.firstOrNull()
                    targetTower?.let { t ->
                        vm._uiState.update { it.copy(selectedTower = t) }
                    }
                }
                if (frame == 900) {
                    vm.addFloatingText(8f, 7f, "⚡ HARMONIC WARP CONDUIT", 0xFFFFD700)
                }
            }
            CinematicScenario.CIN_04_ELEMENTAL_CASCADE -> {
                when (frame) {
                    180 -> {
                        towers.firstOrNull()?.let { t ->
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 CRYO LOCK", 0xFF00E5FF)
                        }
                    }
                    600 -> {
                        val t = towers.getOrNull(1) ?: towers.firstOrNull()
                        t?.let {
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 VOID SINGULARITY", 0xFFA855F7)
                        }
                    }
                    1200 -> {
                        vm.addFloatingText(8f, 6f, "❄️ CASCADE SHATTER ❄️", 0xFF00FF9D)
                    }
                    1380 -> {
                        vm._uiState.update { it.copy(selectedTower = null) }
                    }
                }
            }
            CinematicScenario.CIN_05_KINETIC_GATLING -> {
                when (frame) {
                    120 -> {
                        towers.firstOrNull()?.let { t ->
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 MINIGUN LOCK", 0xFFFFD700)
                        }
                    }
                    480 -> {
                        towers.firstOrNull()?.let { t ->
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "⚡ OVERCLOCK MAX SPIN", 0xFFFF2A55)
                        }
                    }
                    960 -> {
                        vm.addFloatingText(8f, 6f, "💥 ARMOR SHRED MELT", 0xFFFFB800)
                    }
                    1320 -> {
                        vm._uiState.update { it.copy(selectedTower = null) }
                    }
                }
            }
            CinematicScenario.CIN_06_EVOLUTION_MONTAGE -> {
                when (frame) {
                    60 -> {
                        towers.firstOrNull()?.let { t ->
                            vm._uiState.update { it.copy(selectedTower = t) }
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 SELECT TURRET", 0xFF00F0FF)
                        }
                    }
                    240 -> {
                        towers.firstOrNull()?.let { t ->
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 UPGRADE -> TIER 2", 0xFFFFB800)
                        }
                        evolveCinematicTowers(1)
                    }
                    600 -> {
                        towers.firstOrNull()?.let { t ->
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 UPGRADE -> TIER 3", 0xFFFFB800)
                        }
                        evolveCinematicTowers(2)
                    }
                    1080 -> {
                        towers.firstOrNull()?.let { t ->
                            vm.addFloatingText(t.gridPos.x.toFloat(), t.gridPos.y.toFloat(), "👆 ASCEND -> APEX", 0xFFFFD700)
                        }
                        evolveCinematicTowers(3)
                    }
                    1470 -> {
                        vm.addFloatingText(8f, 6f, "★ APEX TRANSCENDENCE ★", 0xFFFFD700)
                    }
                }
            }
        }
    }

    fun stopCinematic() {
        vm._uiState.update {
            it.copy(
                activeCinematic = null,
                isWaveActive = false,
                enemies = emptyList(),
                projectiles = emptyList()
            )
        }
        vm.addFloatingText(8f, 8f, "🎬 CINEMATIC COMPLETE (30s)", 0xFF00FF9D)
    }

    fun jumpToSignatureWave(scenario: CinematicScenario) {
        vm._uiState.update {
            it.copy(
                activeCinematic = null,
                waveNumber = scenario.targetWave,
                isWaveActive = false,
                isGameOver = false,
                isVictory = false,
                enemies = emptyList(),
                projectiles = emptyList(),
                scrap = maxOf(it.scrap, 2000)
            )
        }
        vm.deployWave()
        vm.addFloatingText(8f, 6f, "⚔️ DEPLOYING WAVE ${scenario.targetWave}: ${scenario.title}", 0xFFFFD700)
    }
}
