package com.example.model

import androidx.compose.ui.graphics.Color

data class GridPos(val x: Int, val y: Int)

enum class TileType {
    EMPTY,               // Buildable
    PATH_ALPHA,          // Creep path lane 1
    PATH_BETA,           // Creep path lane 2
    PATH_MERGED,         // Brio train-track merged shared rail segment!
    PATH_INTERSECTION,   // Intersection crossover tile
    OBSTACLE,            // Unbuildable terrain/rock/crystal
    SPAWNER_ALPHA,       // Spawn portal Lane 1
    SPAWNER_BETA,        // Spawn portal Lane 2
    CORE                 // Nexus Core to protect
}

enum class TileBuff(
    val label: String,
    val desc: String,
    val badgeColor: Long,
    val vectorCategory: TowerVector? = null,
    val incomeBonusMultiplier: Float = 0f,
    val damageMultiplier: Float = 1.0f,
    val rangeMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f
) {
    NONE("", "", 0x00000000, null, 0f, 1.0f, 1.0f, 1.0f),
    AMPLIFIER("GREEN MATRIX (+30% DMG)", "Power Grid: +30% Raw Damage to any turret placed here", 0xFF00E676, TowerVector.OFFENSIVE, 0.10f, 1.30f, 1.0f, 1.0f),
    HIGH_GROUND("TEAL MATRIX (+35% RNG)", "Sensor Perch: +35% Targeting Range to any turret placed here", 0xFF00E5FF, TowerVector.DEFENSIVE, 0.10f, 1.0f, 1.35f, 1.0f),
    CONDUIT("ORANGE MATRIX (+35% SPD)", "Accelerator Node: +35% Attack Speed & +25% Scrap Yield", 0xFFFF9100, TowerVector.SUPPORT, 0.25f, 1.0f, 1.0f, 1.35f)
}

data class MapTile(
    val pos: GridPos,
    val type: TileType,
    val buff: TileBuff = TileBuff.NONE,
    /**
     * Clearance-masked buffer tile (inner corner of a lane turn). These are emitted as
     * [TileType.OBSTACLE] with isClearance = true so towers never visually clip walking creeps.
     */
    val isClearance: Boolean = false
) {
    /** True only for ordinary empty tiles that are not clearance-masked. */
    val isBuildable: Boolean get() = type == TileType.EMPTY && !isClearance
}

data class Biome(
    val name: String,
    val description: String,
    val groundColor: Color,
    val groundSecondary: Color,
    val pathColor: Color,
    val accentColor: Color,
    val secondaryConduitColor: Color,
    val obstacleColor: Color
)

data class GeneratedLevel(
    val seed: Long,
    val width: Int,
    val height: Int,
    val biome: Biome,
    val tiles: List<MapTile>,
    val laneAlpha: List<GridPos>,
    val laneBeta: List<GridPos>,
    val intersectionPoints: Set<GridPos>
) {
    val path: List<GridPos> get() = laneAlpha
}
