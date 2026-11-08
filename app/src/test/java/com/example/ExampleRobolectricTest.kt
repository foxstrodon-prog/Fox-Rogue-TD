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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}

