package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.MapGenerator
import com.example.engine.TowerUpgradeManager
import com.example.engine.WaveGenerator
import com.example.model.GridPos
import com.example.model.TileType
import com.example.model.TowerInstance
import com.example.model.TowerType
import com.example.model.EnemyInstance
import com.example.model.EnemyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rogue TD", appName)
  }

  @Test
  fun `procedural map generator produces 2 valid lanes and intersection tile`() {
    val level = MapGenerator.generate(12345L)
    assertNotNull(level)
    assertTrue(level.laneAlpha.size >= 10)
    assertTrue(level.laneBeta.size >= 10)
    assertEquals(TileType.SPAWNER_ALPHA, level.tiles.first { it.pos == level.laneAlpha.first() }.type)
    assertEquals(TileType.SPAWNER_BETA, level.tiles.first { it.pos == level.laneBeta.first() }.type)
    assertEquals(TileType.CORE, level.tiles.first { it.pos == level.laneAlpha.last() }.type)
    assertTrue("Map must contain overlapping PATH_INTERSECTION", level.tiles.any { it.type == TileType.PATH_INTERSECTION })
  }

  @Test
  fun `wave generator generates escalating dual lane threat`() {
    val wave1 = WaveGenerator.generateWave(1)
    val wave5 = WaveGenerator.generateWave(5)
    val wave10 = WaveGenerator.generateWave(10)

    assertEquals(1, wave1.waveNumber)
    assertFalse(wave1.isBossWave)
    assertTrue(wave5.isBossWave)
    assertTrue(wave10.isBossWave)
    assertTrue(wave1.spawnList.any { it.laneIndex == 0 })
    assertTrue(wave1.spawnList.any { it.laneIndex == 1 })
  }

  @Test
  fun `branching evolution system supports 3 Tier 1 branches and 9 Final Forms per tower`() {
    // Verify each of the 6 towers has 3 Tier-1 options and 9 Final Forms (54 total final forms)
    TowerType.values().forEach { type ->
      val t1Options = TowerUpgradeManager.getTier1Options(type)
      assertEquals(3, t1Options.size)

      val allFinalForms = TowerUpgradeManager.get9FinalForms(type)
      assertEquals(9, allFinalForms.size)

      // Each branch must have exactly 3 sub-forms (e.g. A -> A.a, A.b, A.c)
      listOf("A", "B", "C").forEach { branch ->
        val branchFinals = TowerUpgradeManager.getFinalFormOptions(type, branch)
        assertEquals(3, branchFinals.size)
        branchFinals.forEach { ff ->
          assertTrue(ff.code.startsWith(branch))
          assertTrue(ff.globalBuffName.isNotBlank())
          assertTrue(ff.globalBuffDescription.isNotBlank())
        }
      }
    }

    // Test Tower evolution lifecycle
    val tower = TowerInstance(
      id = "test_tower_1",
      type = TowerType.GATLING,
      gridPos = GridPos(2, 2)
    )
    assertTrue(tower.isBase)
    assertFalse(tower.isTier1)
    assertFalse(tower.isFinalForm)

    // Upgrade to Branch A
    val costA = TowerUpgradeManager.getTier1Cost(tower, "A")
    assertTrue(costA > 0)
    tower.branchTier1 = "A"
    assertTrue(tower.isTier1)
    assertFalse(tower.isBase)

    // Upgrade to Final Form A.a
    val costAa = TowerUpgradeManager.getFinalFormCost(tower, "A.a", listOf(tower))
    assertTrue(costAa > 0)
    tower.finalFormCode = "A.a"
    assertTrue(tower.isFinalForm)
    assertTrue(tower.getDisplayName().contains("Hyper-Gatling"))

    // Test Dynamic Cost Inflation:
    val tower2 = TowerInstance(id = "test_tower_2", type = TowerType.CRYO, gridPos = GridPos(3, 3), branchTier1 = "B")
    val costBeforeInflation = TowerUpgradeManager.getFinalFormCost(tower2, "B.a", listOf(tower2))

    // With tower 1 being final form, tower 2 final form cost scales up
    val costWithInflation = TowerUpgradeManager.getFinalFormCost(tower2, "B.a", listOf(tower, tower2))
    assertTrue("Cost must inflate when final forms exist on field", costWithInflation >= costBeforeInflation)

    // Selling / removing tower stabilizes economy
    val costAfterSelling = TowerUpgradeManager.getFinalFormCost(tower2, "B.a", listOf(tower2))
    assertEquals(costBeforeInflation, costAfterSelling)
  }

  @Test
  fun `three tier hierarchy and naming matrix test`() {
    // 1. Tier 0: Base Placement
    val tower = TowerInstance(
      id = "turret_001",
      gridPos = GridPos(1, 1),
      tier = 0
    )
    assertEquals("Basic Turret", tower.getDisplayName())
    assertEquals(80, TowerUpgradeManager.getBaseTurretCost())

    // 2. Tier 1: Subset Upgrade (Vector Selection)
    val vectors = TowerUpgradeManager.getTier1Vectors()
    assertEquals(3, vectors.size)
    tower.tier = 1
    tower.vector = com.example.model.TowerVector.OFFENSIVE
    assertEquals("Offensive Turret", tower.getDisplayName())

    // 3. Tier 2: Identity Lock (Archetype Selection)
    val offensiveArchetypes = TowerUpgradeManager.getTier2Archetypes(com.example.model.TowerVector.OFFENSIVE)
    assertEquals(3, offensiveArchetypes.size)
    tower.tier = 2
    tower.archetype = com.example.model.TowerArchetype.VULCAN_GATLING
    assertEquals("Vulcan Gatling", tower.getDisplayName())

    // 4. Tier 3: Global Buff Projection
    tower.tier = 3
    tower.isTier3GlobalActive = true
    assertEquals("Vulcan Gatling [Global Speed Aura]", tower.getDisplayName())
  }

  @Test
  fun `brio train track merged segment is generated`() {
    val level = MapGenerator.generate(99999L)
    val mergedTiles = level.tiles.filter { it.type == TileType.PATH_MERGED }
    assertTrue("Map must generate Brio train-track merged rail segment", mergedTiles.isNotEmpty())
    assertTrue("Map must have an entrance intersection junction", level.tiles.any { it.type == TileType.PATH_INTERSECTION })
  }

  @Test
  fun `27 archetypes and 54 total auras across 3 classes test`() {
    val classes = com.example.model.TowerClass.values()
    assertEquals(3, classes.size)

    val archetypes = com.example.model.TowerArchetype.values()
    assertEquals(27, archetypes.size)

    // 9 archetypes per class (3 Offensive, 3 Defensive, 3 Support)
    classes.forEach { cls ->
      val classArchetypes = archetypes.filter { it.towerClass == cls }
      assertEquals(9, classArchetypes.size)

      com.example.model.TowerVector.values().forEach { vec ->
        val vectorArchetypes = TowerUpgradeManager.getTier2ArchetypesForClass(cls, vec)
        assertEquals(3, vectorArchetypes.size)
      }
    }

    // Verify 54 total distinct auras (2 per archetype)
    val allAuras = mutableListOf<String>()
    archetypes.forEach { arch ->
      assertNotNull(arch.primaryAura)
      assertNotNull(arch.secondaryAura)
      assertTrue(arch.primaryAura.name.isNotBlank())
      assertTrue(arch.secondaryAura.name.isNotBlank())
      assertTrue(arch.primaryAura.description.isNotBlank())
      assertTrue(arch.secondaryAura.description.isNotBlank())
      allAuras.add(arch.primaryAura.name)
      allAuras.add(arch.secondaryAura.name)
    }
    assertEquals(54, allAuras.size)
    assertEquals(54, allAuras.distinct().size)
  }

  @Test
  fun `tower class unlock progression and dual aura selection test`() {
    // Stage 0: Initial run - Only Kinetic Ballistic is unlocked
    val emptyTowers = emptyList<TowerInstance>()
    assertTrue(TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.KINETIC_BALLISTIC, emptyTowers))
    assertFalse(TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.ELEMENTAL_VOID, emptyTowers))
    assertFalse(TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.HEAVY_SIEGE, emptyTowers))

    // Create a Tier 3 Kinetic Tower
    val tower1 = TowerInstance(
      id = "t1",
      gridPos = GridPos(1, 1),
      towerClass = com.example.model.TowerClass.KINETIC_BALLISTIC,
      tier = 3,
      isTier3GlobalActive = true,
      archetype = com.example.model.TowerArchetype.VULCAN_GATLING,
      selectedAuraIndex = 0
    )

    // Stage 1: 1 Tier 3 Tower - Elemental Void unlocks!
    val oneTier3List = listOf(tower1)
    assertTrue(TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.KINETIC_BALLISTIC, oneTier3List))
    assertTrue("Elemental Void must unlock after 1 Tier 3 tower", TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.ELEMENTAL_VOID, oneTier3List))
    assertFalse("Heavy Siege remains locked with only 1 Tier 3 tower", TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.HEAVY_SIEGE, oneTier3List))

    // Create a Tier 3 Elemental Tower with Secondary Aura (aura index 1)
    val tower2 = TowerInstance(
      id = "t2",
      gridPos = GridPos(2, 2),
      towerClass = com.example.model.TowerClass.ELEMENTAL_VOID,
      tier = 3,
      isTier3GlobalActive = true,
      archetype = com.example.model.TowerArchetype.FROST_SPIRE,
      selectedAuraIndex = 1
    )

    // Stage 2: 2 Tier 3 Towers - Heavy Siege unlocks!
    val twoTier3List = listOf(tower1, tower2)
    assertTrue("Heavy Siege unlocks with 2 Tier 3 towers", TowerUpgradeManager.isClassUnlocked(com.example.model.TowerClass.HEAVY_SIEGE, twoTier3List))

    // Test Dual Aura Selection Verification
    assertEquals("Global Speed Aura", tower1.activeAura?.name)
    assertTrue(tower1.getDisplayName().contains("Global Speed Aura"))

    assertEquals("Cryo Vulnerability", tower2.activeAura?.name)
    assertTrue(tower2.getDisplayName().contains("Cryo Vulnerability"))

    // Toggle aura index on tower1 to secondary aura
    tower1.selectedAuraIndex = 1
    assertEquals("Kinetic Windup Aura", tower1.activeAura?.name)
    assertTrue(tower1.getDisplayName().contains("Kinetic Windup Aura"))
  }

  @Test
  fun `tower class visual geometric shapes distinction test`() {
    // Circle for Kinetic Ballistics
    assertEquals("●", com.example.model.TowerClass.KINETIC_BALLISTIC.shapeSymbol)
    assertEquals("Circle", com.example.model.TowerClass.KINETIC_BALLISTIC.shapeName)

    // Triangle for Elemental Void
    assertEquals("▲", com.example.model.TowerClass.ELEMENTAL_VOID.shapeSymbol)
    assertEquals("Triangle", com.example.model.TowerClass.ELEMENTAL_VOID.shapeName)

    // Square for Heavy Siege
    assertEquals("■", com.example.model.TowerClass.HEAVY_SIEGE.shapeSymbol)
    assertEquals("Square", com.example.model.TowerClass.HEAVY_SIEGE.shapeName)
  }

  @Test
  fun `map generator intersections exclude spawners and core to avoid stray bottom lines`() {
    val level = MapGenerator.generate(42L)
    val spawnerAlpha = level.laneAlpha.first()
    val spawnerBeta = level.laneBeta.first()
    val corePos = level.laneAlpha.last()
    assertFalse("Spawners must not be in intersectionPoints", level.intersectionPoints.contains(spawnerAlpha))
    assertFalse("Spawners must not be in intersectionPoints", level.intersectionPoints.contains(spawnerBeta))
    assertFalse("Core must not be in intersectionPoints", level.intersectionPoints.contains(corePos))
  }

  @Test
  fun `dev balance config and tower effectiveness telemetry test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val telemetryManager = com.example.engine.TelemetryManager(context)

    // Verify initial balance config has reduced early income (220 scrap default)
    val initialConfig = telemetryManager.getBalanceConfig()
    assertEquals(220, initialConfig.startingScrap)
    assertEquals(1.0f, initialConfig.globalIncomeMultiplier, 0.001f)
    assertEquals(1.0f, initialConfig.kineticDamageMultiplier, 0.001f)
    assertEquals(1.0f, initialConfig.elementalDamageMultiplier, 0.001f)
    assertEquals(1.0f, initialConfig.heavyDamageMultiplier, 0.001f)

    // Save modified balance config (on-the-fly adjustment)
    val customConfig = initialConfig.copy(
      startingScrap = 150,
      globalIncomeMultiplier = 0.8f,
      kineticDamageMultiplier = 1.25f,
      elementalDamageMultiplier = 1.10f,
      heavyDamageMultiplier = 0.90f
    )
    telemetryManager.saveBalanceConfig(customConfig)

    val reloadedConfig = telemetryManager.getBalanceConfig()
    assertEquals(150, reloadedConfig.startingScrap)
    assertEquals(0.8f, reloadedConfig.globalIncomeMultiplier, 0.001f)
    assertEquals(1.25f, reloadedConfig.kineticDamageMultiplier, 0.001f)
    assertEquals(1.10f, reloadedConfig.elementalDamageMultiplier, 0.001f)
    assertEquals(0.90f, reloadedConfig.heavyDamageMultiplier, 0.001f)

    // Test Tower Performance Telemetry
    val perfRecords = telemetryManager.getTowerPerformance()
    assertTrue("Should have baseline tower performance records", perfRecords.isNotEmpty())
    assertTrue("Should calculate dps cost efficiency", perfRecords.all { it.dpsCostEfficiency >= 0 })

    // Simulate session tower recording
    val testTower = TowerInstance(
      id = "t_test_perf",
      gridPos = GridPos(0, 0),
      towerClass = com.example.model.TowerClass.KINETIC_BALLISTIC,
      archetype = com.example.model.TowerArchetype.VULCAN_GATLING,
      kills = 15,
      totalDamage = 3500f
    )
    telemetryManager.recordTowerSession(listOf(testTower))
    val updatedRecords = telemetryManager.getTowerPerformance()
    val vulcanRecord = updatedRecords.firstOrNull { it.towerName == "Vulcan Gatling" }
    assertNotNull(vulcanRecord)
    assertEquals(15L, vulcanRecord!!.totalKills)
    assertEquals(3500.0, vulcanRecord.totalDamage, 0.1)
  }

  @Test
  fun `hyper 10x combat simulation speed is supported`() {
    val speeds = com.example.model.GameSpeed.values()
    assertTrue(speeds.any { it.multiplier == 10.0f && it.label == "10X" })
    val hyper = com.example.model.GameSpeed.HYPER
    assertEquals(10.0f, hyper.multiplier, 0.001f)
    assertEquals("10X", hyper.label)
  }

  @Test
  fun `tactical tile buffs use green teal orange matrices with 2-2-2 fixed spawn distribution`() {
    val amp = com.example.model.TileBuff.AMPLIFIER
    assertEquals(0xFF00E676L, amp.badgeColor)
    assertTrue(amp.label.contains("GREEN MATRIX"))

    val hg = com.example.model.TileBuff.HIGH_GROUND
    assertEquals(0xFF00E5FFL, hg.badgeColor)
    assertTrue(hg.label.contains("TEAL MATRIX"))

    val conduit = com.example.model.TileBuff.CONDUIT
    assertEquals(0xFFFF9100L, conduit.badgeColor)
    assertTrue(conduit.label.contains("ORANGE MATRIX"))
    assertEquals(0.25f, conduit.incomeBonusMultiplier, 0.001f)

    // Verify MapGenerator spawns exactly 2 Green, 2 Teal, and 2 Orange pads
    val level = MapGenerator.generate(777L)
    val greenCount = level.tiles.count { it.buff == com.example.model.TileBuff.AMPLIFIER }
    val tealCount = level.tiles.count { it.buff == com.example.model.TileBuff.HIGH_GROUND }
    val orangeCount = level.tiles.count { it.buff == com.example.model.TileBuff.CONDUIT }
    assertEquals(2, greenCount)
    assertEquals(2, tealCount)
    assertEquals(2, orangeCount)
    assertEquals(6, level.tiles.count { it.buff != com.example.model.TileBuff.NONE })
  }

  @Test
  fun `procedural generator produces extended winding paths`() {
    val level = MapGenerator.generate(54321L)
    assertTrue("Lane Alpha should have at least 15 steps", level.laneAlpha.size >= 15)
    assertTrue("Lane Beta should have at least 15 steps", level.laneBeta.size >= 15)
    assertTrue("Merged track spine must have multiple merged tiles", level.tiles.count { it.type == TileType.PATH_MERGED } >= 3)
    assertEquals(TileType.CORE, level.tiles.first { it.pos == level.laneAlpha.last() }.type)
  }

  @Test
  fun `score calculation and zero-score leaderboard omission test`() {
    val recordZero = com.example.model.RunRecord(
      id = "zero_run",
      timestamp = System.currentTimeMillis(),
      sectorName = "Sector Zero",
      wavesCleared = 0,
      enemiesKilled = 0,
      totalDamage = 0f,
      scrapEarned = 0,
      isVictory = false,
      mvpTower = "None",
      playerName = "TestOperative"
    )
    assertEquals(0, recordZero.score)

    val recordActive = com.example.model.RunRecord(
      id = "active_run",
      timestamp = System.currentTimeMillis(),
      sectorName = "Sector 1",
      wavesCleared = 5,
      enemiesKilled = 30,
      totalDamage = 2000f,
      scrapEarned = 500,
      isVictory = true,
      mvpTower = "Vulcan Gatling",
      playerName = "Viper"
    )
    // 5 * 1000 + 30 * 15 + 2000 * 0.1 = 5000 + 450 + 200 = 5650
    assertEquals(5650, recordActive.score)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val telemetryManager = com.example.engine.TelemetryManager(context)

    // Save active run
    telemetryManager.recordRun(recordActive)
    // Attempt saving zero-score run
    telemetryManager.recordRun(recordZero)

    val leaderboard = telemetryManager.getLeaderboard()
    assertTrue("Leaderboard must not contain 0-score runs", leaderboard.all { it.score > 0 })
    assertTrue("Leaderboard must contain active run", leaderboard.any { it.playerName == "Viper" && it.score == 5650 })
  }

  @Test
  fun `player callsign management and isolated lifetime telemetry test`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val telemetryManager = com.example.engine.TelemetryManager(context)

    // Set callsign
    telemetryManager.setActivePlayerName("CommanderFox")
    assertEquals("CommanderFox", telemetryManager.getActivePlayerName())

    // Record run for CommanderFox
    val run1 = com.example.model.RunRecord(
      id = "run_fox_1",
      timestamp = System.currentTimeMillis(),
      sectorName = "Sector Alpha",
      wavesCleared = 8,
      enemiesKilled = 50,
      totalDamage = 4000f,
      scrapEarned = 800,
      isVictory = false,
      mvpTower = "Laser Sentry",
      playerName = "CommanderFox"
    )
    telemetryManager.recordRun(run1)

    // Verify last run
    val lastRun = telemetryManager.getLastRun()
    assertNotNull(lastRun)
    assertEquals("CommanderFox", lastRun!!.playerName)
    assertEquals(8, lastRun.wavesCleared)

    // Verify personal lifetime metrics for CommanderFox
    val foxLifetime = telemetryManager.getLifetimeTelemetryForPlayer("CommanderFox")
    assertTrue(foxLifetime.totalRuns >= 1)
    assertTrue(foxLifetime.bestWaveEver >= 8)
    assertTrue(foxLifetime.totalHostilesKilled >= 50)

    // Switch player name
    telemetryManager.setActivePlayerName("RookiePilot")
    val rookieLifetime = telemetryManager.getLifetimeTelemetryForPlayer("RookiePilot")
    assertEquals(0, rookieLifetime.totalRuns)
    assertEquals(0, rookieLifetime.bestWaveEver)
  }

  @Test
  fun `omnidirectional procedural map generator respects min and max lane length constraints`() {
    val level = MapGenerator.generate(
      seed = 98765L,
      minLaneLength = 14,
      maxLaneLength = 28
    )
    assertNotNull(level)
    assertTrue("Lane Alpha must be >= minLaneLength (14)", level.laneAlpha.size >= 14)
    assertTrue("Lane Beta must be >= minLaneLength (14)", level.laneBeta.size >= 14)
    assertTrue("Merged tiles must exist along crossover", level.tiles.any { it.type == TileType.PATH_MERGED })
    assertEquals(TileType.CORE, level.tiles.first { it.pos == level.laneAlpha.last() }.type)
    assertEquals(6, level.tiles.count { it.buff != com.example.model.TileBuff.NONE })
  }

  @Test
  fun `diff-only json export produces delta containing only modified parameters`() {
    val defaultCfg = com.example.model.DevBalanceConfig()
    val cleanDiff = defaultCfg.exportDiffJson()
    assertEquals("{}", cleanDiff.trim())

    // Modify only startingScrap and Vulcan Gatling range
    val modifiedCfg = defaultCfg.copy(
      startingScrap = 180,
      customTowerConfigs = mapOf(
        "GATLING" to defaultCfg.getTowerBalance(TowerType.GATLING).copy(range = 4.5f)
      )
    )

    val diffJson = modifiedCfg.exportDiffJson()
    assertTrue(diffJson.contains("\"startingScrap\": 180"))
    assertTrue(diffJson.contains("\"customTowers\""))
    assertTrue(diffJson.contains("\"range\": 4.5"))
    assertFalse(diffJson.contains("\"globalIncomeMultiplier\"")) // Unchanged must be omitted!
    assertFalse(diffJson.contains("\"customEnemies\"")) // Unchanged must be omitted!
  }

  @Test
  fun `wave generator scales enemy spawn count using enemiesPerRoundMultiplier`() {
    val baseWave = WaveGenerator.generateWave(waveNumber = 3, enemyCountMultiplier = 1.0f)
    val heavyWave = WaveGenerator.generateWave(waveNumber = 3, enemyCountMultiplier = 2.0f)

    assertTrue(
      "Doubling enemyCountMultiplier should increase enemy spawn count",
      heavyWave.spawnList.size > baseWave.spawnList.size
    )
  }

  @Test
  fun `verify app build version is 0 0 0 008`() {
    assertEquals("0.0.0.008", com.example.BuildConfig.VERSION_NAME)
    assertEquals(16, com.example.BuildConfig.VERSION_CODE)
  }

  @Test
  fun `verify app update manager version comparison`() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val updater = com.example.engine.AppUpdateManager(context)
    assertTrue(updater.isNewerVersion("v0.0.0.009", "0.0.0.008"))
    assertTrue(updater.isNewerVersion("0.0.0.009", "0.0.0.008"))
    assertFalse(updater.isNewerVersion("v0.0.0.008", "0.0.0.008"))
    assertFalse(updater.isNewerVersion("0.0.0.007", "0.0.0.008"))
  }

  @Test
  fun `verify player identity device uuid and anonymous commander name`() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val uuid = com.example.engine.storage.PlayerIdentity.getOrCreateDeviceUuid(context)
    assertNotNull(uuid)
    assertTrue(uuid.isNotBlank())
    assertEquals(36, uuid.length)
    val defaultName = com.example.engine.storage.PlayerIdentity.getPlayerCallsign(context)
    assertTrue(defaultName.startsWith("Commander-"))
  }

  @Test
  fun `verify device rotation angle updates in ui state`() {
    val app = org.robolectric.RuntimeEnvironment.getApplication()
    val vm = com.example.ui.RogueTDViewModel(app)
    assertEquals(0f, vm.uiState.value.deviceRotationAngle)
    vm.updateDeviceRotation(90f)
    assertEquals(90f, vm.uiState.value.deviceRotationAngle)
    vm.updateDeviceRotation(270f)
    assertEquals(270f, vm.uiState.value.deviceRotationAngle)
  }

  @Test
  fun `generated lanes must touch all 4 corners of the map across diverse seeds`() {
    val seeds = listOf(1L, 42L, 777L, 12345L, 54321L, 99999L, 20261109L)
    for (seed in seeds) {
      val level = com.example.engine.MapGenerator.generate(seed)
      val allLaneTiles = (level.laneAlpha + level.laneBeta).toSet()

      val cornerTL = com.example.model.GridPos(0, 0)
      val cornerTR = com.example.model.GridPos(level.width - 1, 0)
      val cornerBL = com.example.model.GridPos(0, level.height - 1)
      val cornerBR = com.example.model.GridPos(level.width - 1, level.height - 1)

      assertTrue("Seed $seed must touch Top-Left corner", allLaneTiles.contains(cornerTL))
      assertTrue("Seed $seed must touch Top-Right corner", allLaneTiles.contains(cornerTR))
      assertTrue("Seed $seed must touch Bottom-Left corner", allLaneTiles.contains(cornerBL))
      assertTrue("Seed $seed must touch Bottom-Right corner", allLaneTiles.contains(cornerBR))
    }
  }

  @Test
  fun `verify firestore leaderboard api fails safely when unconfigured`() = kotlinx.coroutines.runBlocking {
    val api = com.example.engine.network.FirestoreLeaderboardApi(projectId = "", apiKey = "")
    assertFalse(api.isConfigured)
    val submission = com.example.model.PendingSubmission(
      runId = "test_run",
      callsign = "TestPlayer",
      score = 1000,
      wavesCleared = 5,
      scrapEarned = 200,
      totalDamage = 5000L,
      timestampMs = System.currentTimeMillis(),
      clientVersion = "0.0.0.008"
    )
    val result = api.submitScore(submission)
    assertTrue(result is com.example.model.LeaderboardResult.Failure)
    assertEquals(com.example.model.FailureKind.UNCONFIGURED, (result as com.example.model.LeaderboardResult.Failure).kind)
  }

  @Test
  fun `map generator paths guarantee strict continuity with manhattan distance 1 and proper spawner core endpoints`() {
    val testSeeds = listOf(1L, 7L, 42L, 99L, 777L, 1024L, 12345L, 54321L, 99999L, 20261109L)
    for (seed in testSeeds) {
      val level = com.example.engine.MapGenerator.generate(seed)
      val spawnerAlpha = level.tiles.first { it.type == TileType.SPAWNER_ALPHA }.pos
      val spawnerBeta = level.tiles.first { it.type == TileType.SPAWNER_BETA }.pos
      val core = level.tiles.first { it.type == TileType.CORE }.pos

      assertEquals("Lane Alpha must start at spawner Alpha on seed $seed", spawnerAlpha, level.laneAlpha.first())
      assertEquals("Lane Alpha must terminate at core on seed $seed", core, level.laneAlpha.last())
      assertEquals("Lane Beta must start at spawner Beta on seed $seed", spawnerBeta, level.laneBeta.first())
      assertEquals("Lane Beta must terminate at core on seed $seed", core, level.laneBeta.last())

      for (i in 0 until level.laneAlpha.size - 1) {
        val p0 = level.laneAlpha[i]
        val p1 = level.laneAlpha[i + 1]
        val dist = kotlin.math.abs(p1.x - p0.x) + kotlin.math.abs(p1.y - p0.y)
        assertEquals("Lane Alpha step $i -> ${i + 1} on seed $seed must have Manhattan distance 1", 1, dist)
      }

      for (i in 0 until level.laneBeta.size - 1) {
        val p0 = level.laneBeta[i]
        val p1 = level.laneBeta[i + 1]
        val dist = kotlin.math.abs(p1.x - p0.x) + kotlin.math.abs(p1.y - p0.y)
        assertEquals("Lane Beta step $i -> ${i + 1} on seed $seed must have Manhattan distance 1", 1, dist)
      }
    }
  }

  @Test
  fun `verify callsign sanitization and store pending submission queue`() = kotlinx.coroutines.runBlocking {
    val sanitized = com.example.engine.network.FirestoreLeaderboardApi.sanitizeCallsign("Commander-XYZ [DEV]!@#")
    assertEquals("Commander-XYZ DE", sanitized)

    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val store = com.example.engine.storage.LeaderboardStore(context)
    val submission = com.example.model.PendingSubmission(
      runId = "test_run_123",
      callsign = sanitized,
      score = 12500,
      wavesCleared = 12,
      scrapEarned = 750,
      totalDamage = 45000L,
      timestampMs = System.currentTimeMillis(),
      clientVersion = "0.0.0.008"
    )
    store.enqueuePending(submission)
    val queue = store.getPendingQueue()
    assertEquals(1, queue.size)
    assertEquals("test_run_123", queue[0].runId)
    assertEquals("Commander-XYZ DE", queue[0].callsign)

    store.removePending("test_run_123")
    val emptyQueue = store.getPendingQueue()
    assertTrue(emptyQueue.isEmpty())
  }

  @Test
  fun `app update manager correctly compares semantic versions`() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val manager = com.example.engine.AppUpdateManager(context)
    try {
      assertTrue(manager.isNewerVersion("v0.0.0.005", "0.0.0.004"))
      assertTrue(manager.isNewerVersion("0.0.1.000", "0.0.0.005"))
      assertFalse(manager.isNewerVersion("v0.0.0.004", "0.0.0.004"))
      assertFalse(manager.isNewerVersion("v0.0.0.003", "0.0.0.004"))
    } finally {
      manager.unregisterReceiver()
    }
  }

  @Test
  fun `orientation bucket calculation applies hysteresis deadband without jitter`() {
    // Initial bucket at 0 degrees
    val b0 = MainActivity.computeOrientationBucket(0, 0f)
    assertEquals(0f, b0)

    // Jitter around 45 degrees boundary (e.g. 40, 45, 50) stays in current bucket due to deadband
    val b44 = MainActivity.computeOrientationBucket(44, 0f)
    assertEquals(0f, b44)
    val b46 = MainActivity.computeOrientationBucket(46, 0f)
    assertEquals(0f, b46)

    // Firmly inside 90 degree quadrant (within 90 +/- 30)
    val b90 = MainActivity.computeOrientationBucket(85, 0f)
    assertEquals(90f, b90)

    // Tilting back towards boundary (e.g. 130) retains 90 degrees
    val b130 = MainActivity.computeOrientationBucket(130, b90)
    assertEquals(90f, b130)

    // Firmly inside 180 quadrant
    val b180 = MainActivity.computeOrientationBucket(175, b90)
    assertEquals(180f, b180)

    // Firmly inside 270 quadrant
    val b270 = MainActivity.computeOrientationBucket(265, b180)
    assertEquals(270f, b270)
  }

  @Test
  fun `leaderboard store submitted ids dedup prevents duplicate entries`() = kotlinx.coroutines.runBlocking {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val store = com.example.engine.storage.LeaderboardStore(context)
    assertFalse(store.isAlreadySubmitted("run_abc"))
    store.recordSubmittedId("run_abc")
    assertTrue(store.isAlreadySubmitted("run_abc"))
  }

  @Test
  fun `damage calculator applies shape bonus on match and resistance penalty on mismatch`() {
    val baseDmg = 100f
    // Shape match: Circle vs Circle
    val matchRes = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = com.example.model.EnemyShape.CIRCLE,
      towerAffinity = com.example.model.ElementAffinity.RED,
      enemyShape = com.example.model.EnemyShape.CIRCLE,
      enemyAffinity = com.example.model.ElementAffinity.RED,
      enemyArmor = 0f
    )
    assertTrue(matchRes.isShapeMatch)
    assertEquals(1.20f, matchRes.shapeMultiplier, 0.001f)
    assertEquals(120f, matchRes.finalDamage, 0.01f)

    // Shape mismatch: Circle vs Triangle
    val mismatchRes = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = com.example.model.EnemyShape.CIRCLE,
      towerAffinity = com.example.model.ElementAffinity.RED,
      enemyShape = com.example.model.EnemyShape.TRIANGLE,
      enemyAffinity = com.example.model.ElementAffinity.RED,
      enemyArmor = 0f
    )
    assertFalse(mismatchRes.isShapeMatch)
    assertEquals(0.50f, mismatchRes.shapeMultiplier, 0.001f)
    assertEquals(50f, mismatchRes.finalDamage, 0.01f)
  }

  @Test
  fun `damage calculator elemental color affinity rock-paper-scissors counter loop`() {
    val baseDmg = 100f
    val shape = com.example.model.EnemyShape.CIRCLE

    // Red > Green (Advantage)
    val redVsGreen = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = shape,
      towerAffinity = com.example.model.ElementAffinity.RED,
      enemyShape = shape,
      enemyAffinity = com.example.model.ElementAffinity.GREEN
    )
    assertTrue(redVsGreen.isColorAdvantage)
    assertFalse(redVsGreen.isColorDisadvantage)
    assertEquals(1.30f, redVsGreen.colorMultiplier, 0.001f)

    // Green > Yellow (Advantage)
    val greenVsYellow = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = shape,
      towerAffinity = com.example.model.ElementAffinity.GREEN,
      enemyShape = shape,
      enemyAffinity = com.example.model.ElementAffinity.YELLOW
    )
    assertTrue(greenVsYellow.isColorAdvantage)
    assertEquals(1.30f, greenVsYellow.colorMultiplier, 0.001f)

    // Yellow > Red (Advantage)
    val yellowVsRed = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = shape,
      towerAffinity = com.example.model.ElementAffinity.YELLOW,
      enemyShape = shape,
      enemyAffinity = com.example.model.ElementAffinity.RED
    )
    assertTrue(yellowVsRed.isColorAdvantage)
    assertEquals(1.30f, yellowVsRed.colorMultiplier, 0.001f)

    // Green vs Red (Disadvantage)
    val greenVsRed = com.example.engine.DamageCalculator.calculateDamage(
      baseDamage = baseDmg,
      towerShape = shape,
      towerAffinity = com.example.model.ElementAffinity.GREEN,
      enemyShape = shape,
      enemyAffinity = com.example.model.ElementAffinity.RED
    )
    assertFalse(greenVsRed.isColorAdvantage)
    assertTrue(greenVsRed.isColorDisadvantage)
    assertEquals(0.70f, greenVsRed.colorMultiplier, 0.001f)
  }

  @Test
  fun `tier-1 archetype vector colors and 27 advanced tower distinct colors constraint`() {
    val offensiveRed = 0xFFFF2A55
    val defensiveGreen = 0xFF00E676
    val supportYellow = 0xFFFFD700

    assertEquals(offensiveRed, com.example.model.TowerVector.OFFENSIVE.color)
    assertEquals(defensiveGreen, com.example.model.TowerVector.DEFENSIVE.color)
    assertEquals(supportYellow, com.example.model.TowerVector.SUPPORT.color)

    val reservedColors = setOf(offensiveRed, defensiveGreen, supportYellow)
    val archetypes = com.example.model.TowerArchetype.entries
    assertEquals("Must have 27 specialization upgrades", 27, archetypes.size)

    for (arch in archetypes) {
      assertFalse(
        "Archetype ${arch.name} must not reuse reserved Tier-1 archetype colors",
        reservedColors.contains(arch.color)
      )
    }
  }

  @Test
  fun `effective combat range and Euclidean distance check are synchronized`() {
    val tower = TowerInstance(
      id = "test-tower",
      type = TowerType.GATLING,
      gridPos = GridPos(2, 2)
    )
    val baseRange = tower.getEffectiveCombatRange()
    assertEquals(TowerType.GATLING.baseRange, baseRange, 0.001f)

    // High ground tile buff grants +35% range
    val buffedRange = tower.getEffectiveCombatRange(tileBuff = com.example.model.TileBuff.HIGH_GROUND)
    assertEquals(TowerType.GATLING.baseRange * 1.35f, buffedRange, 0.001f)

    // Global range aura (+25%)
    val globalBuffedRange = tower.getEffectiveCombatRange(globalRangeBonus = 0.25f)
    assertEquals(TowerType.GATLING.baseRange * 1.25f, globalBuffedRange, 0.001f)

    // Verify Euclidean targeting boundary: dist <= effectiveRange + enemy.type.hitRadius
    val enemyInRange = EnemyInstance(
      id = "e1",
      type = EnemyType.DRONE,
      laneIndex = 0,
      maxHp = 100f,
      currentHp = 100f,
      speed = 1.0f,
      worldX = 2.0f + baseRange + EnemyType.DRONE.hitRadius - 0.01f,
      worldY = 2.0f
    )
    val distInRange = kotlin.math.hypot(enemyInRange.worldX - tower.gridPos.x, enemyInRange.worldY - tower.gridPos.y)
    assertTrue(distInRange <= (baseRange + enemyInRange.type.hitRadius))

    val enemyOutOfRange = EnemyInstance(
      id = "e2",
      type = EnemyType.DRONE,
      laneIndex = 0,
      maxHp = 100f,
      currentHp = 100f,
      speed = 1.0f,
      worldX = 2.0f + baseRange + EnemyType.DRONE.hitRadius + 0.1f,
      worldY = 2.0f
    )
    val distOutOfRange = kotlin.math.hypot(enemyOutOfRange.worldX - tower.gridPos.x, enemyOutOfRange.worldY - tower.gridPos.y)
    assertFalse(distOutOfRange <= (baseRange + enemyOutOfRange.type.hitRadius))
  }
}



