package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.*
import kotlin.random.Random

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

object MapGenerator {
    const val DEFAULT_WIDTH = 10
    const val DEFAULT_HEIGHT = 14

    val BIOMES = listOf(
        Biome(
            name = "Sector Zero: Cyber Convergence",
            description = "High-voltage dual-conduit grid with overlapping superconductor intersection.",
            groundColor = Color(0xFF090E17),
            groundSecondary = Color(0xFF111C2E),
            pathColor = Color(0xFF1E293B),
            accentColor = Color(0xFF00F0FF),
            secondaryConduitColor = Color(0xFFFFB800),
            obstacleColor = Color(0xFF1E3A5F)
        ),
        Biome(
            name = "Dual Sub-Void Nexus",
            description = "Gravitational divergence rift with intersecting dark matter lanes.",
            groundColor = Color(0xFF0D081F),
            groundSecondary = Color(0xFF181036),
            pathColor = Color(0xFF2E1065),
            accentColor = Color(0xFFA855F7),
            secondaryConduitColor = Color(0xFF00FF9D),
            obstacleColor = Color(0xFF4C1D95)
        ),
        Biome(
            name = "Overlapping Magma Caldera",
            description = "Volcanic rock plateau split by intersecting geothermal lava flows.",
            groundColor = Color(0xFF170C08),
            groundSecondary = Color(0xFF29150E),
            pathColor = Color(0xFF451A03),
            accentColor = Color(0xFFFF7A00),
            secondaryConduitColor = Color(0xFFFF2A55),
            obstacleColor = Color(0xFF78350F)
        ),
        Biome(
            name = "Glacial Crossing Chasm",
            description = "Sub-zero permafrost crag with crystalline double glacial fissures crossing paths.",
            groundColor = Color(0xFF08121E),
            groundSecondary = Color(0xFF0F2033),
            pathColor = Color(0xFF0C4A6E),
            accentColor = Color(0xFF38BDF8),
            secondaryConduitColor = Color(0xFF00F0FF),
            obstacleColor = Color(0xFF164E63)
        )
    )

    enum class BorderEdge { NORTH, SOUTH, EAST, WEST }

    private fun getBorderPos(edge: BorderEdge, width: Int, height: Int, random: Random): GridPos {
        return when (edge) {
            BorderEdge.NORTH -> GridPos(random.nextInt(1, width - 1), 0)
            BorderEdge.SOUTH -> GridPos(random.nextInt(1, width - 1), height - 1)
            BorderEdge.WEST -> GridPos(0, random.nextInt(1, height - 1))
            BorderEdge.EAST -> GridPos(width - 1, random.nextInt(1, height - 1))
        }
    }

    fun generate(
        seed: Long = System.currentTimeMillis(),
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        minLaneLength: Int = 12,
        maxLaneLength: Int = 26
    ): GeneratedLevel {
        val random = Random(seed)
        val biome = BIOMES[random.nextInt(BIOMES.size)]

        // 1. Omnidirectional Entry & Exit: pick randomized distinct borders for Spawners and Nexus Core
        val allEdges = BorderEdge.values().toList().shuffled(random)
        val spawnerAlphaEdge = allEdges[0]
        val spawnerBetaEdge = allEdges[1]
        val coreEdge = allEdges[2]

        val spawnerAlpha = getBorderPos(spawnerAlphaEdge, width, height, random)
        var spawnerBeta = getBorderPos(spawnerBetaEdge, width, height, random)
        while (spawnerBeta == spawnerAlpha) {
            spawnerBeta = getBorderPos(spawnerBetaEdge, width, height, random)
        }

        var corePos = getBorderPos(coreEdge, width, height, random)
        while (corePos == spawnerAlpha || corePos == spawnerBeta) {
            corePos = getBorderPos(coreEdge, width, height, random)
        }

        // 2. Central Nexus/Merge Zone (Multi-axis crossover and shared rail)
        val centerX = width / 2
        val centerY = height / 2
        val isHorizontalSpine = random.nextBoolean()
        
        val mergedRail = if (isHorizontalSpine) {
            val startX = (centerX - 2).coerceAtLeast(1)
            val endX = (centerX + 1).coerceAtMost(width - 2)
            (startX..endX).map { x -> GridPos(x, centerY) }
        } else {
            val startY = (centerY - 2).coerceAtLeast(1)
            val endY = (centerY + 1).coerceAtMost(height - 2)
            (startY..endY).map { y -> GridPos(centerX, y) }
        }

        val mergeStart = mergedRail.first()
        val mergeEnd = mergedRail.last()

        val spineLen = mergedRail.size
        val targetMinPerLeg = ((minLaneLength - spineLen) / 2).coerceAtLeast(3)
        val targetMaxPerLeg = ((maxLaneLength - spineLen) / 2).coerceAtLeast(targetMinPerLeg + 1)

        // 3. Multi-directional winding lead-ins
        val leadAlpha = generateWindingPath(
            start = spawnerAlpha,
            target = mergeStart,
            width = width,
            height = height,
            random = random,
            minSteps = targetMinPerLeg,
            maxSteps = targetMaxPerLeg
        )

        val leadBeta = generateWindingPath(
            start = spawnerBeta,
            target = mergeStart,
            width = width,
            height = height,
            random = random,
            minSteps = targetMinPerLeg,
            maxSteps = targetMaxPerLeg
        )

        // 4. Split tail segments winding towards destination Core
        val intermediateWaypoint = if (isHorizontalSpine) {
            GridPos(
                if (random.nextBoolean()) random.nextInt(1, 3) else random.nextInt(width - 3, width - 1),
                if (corePos.y > centerY) random.nextInt(centerY, height - 1) else random.nextInt(1, centerY)
            )
        } else {
            GridPos(
                if (corePos.x > centerX) random.nextInt(centerX, width - 1) else random.nextInt(1, centerX),
                if (random.nextBoolean()) random.nextInt(1, 3) else random.nextInt(height - 3, height - 1)
            )
        }

        val tailAlphaLeg1 = generateWindingPath(
            start = mergeEnd,
            target = intermediateWaypoint,
            width = width,
            height = height,
            random = random,
            minSteps = (targetMinPerLeg / 2).coerceAtLeast(2),
            maxSteps = (targetMaxPerLeg / 2).coerceAtLeast(3)
        )
        val tailAlphaLeg2 = generateWindingPath(
            start = intermediateWaypoint,
            target = corePos,
            width = width,
            height = height,
            random = random,
            minSteps = (targetMinPerLeg / 2).coerceAtLeast(2),
            maxSteps = (targetMaxPerLeg / 2).coerceAtLeast(3)
        )
        val tailAlpha = (tailAlphaLeg1.dropLast(1) + tailAlphaLeg2).distinct()

        val tailBeta = generateWindingPath(
            start = mergeEnd,
            target = corePos,
            width = width,
            height = height,
            random = random,
            minSteps = targetMinPerLeg,
            maxSteps = targetMaxPerLeg
        )

        val laneAlpha = (leadAlpha.dropLast(1) + mergedRail.dropLast(1) + tailAlpha).distinct()
        val laneBeta = (leadBeta.dropLast(1) + mergedRail.dropLast(1) + tailBeta).distinct()

        val alphaSet = laneAlpha.toSet()
        val betaSet = laneBeta.toSet()
        val intersections = (alphaSet.intersect(betaSet) - setOf(corePos, spawnerAlpha, spawnerBeta)).toSet()

        val tileList = mutableListOf<MapTile>()
        val candidateBuffPositions = mutableListOf<GridPos>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pos = GridPos(x, y)
                when {
                    pos == corePos -> tileList.add(MapTile(pos, TileType.CORE))
                    pos == spawnerAlpha -> tileList.add(MapTile(pos, TileType.SPAWNER_ALPHA))
                    pos == spawnerBeta -> tileList.add(MapTile(pos, TileType.SPAWNER_BETA))
                    pos == mergeStart -> tileList.add(MapTile(pos, TileType.PATH_INTERSECTION))
                    pos in intersections -> tileList.add(MapTile(pos, TileType.PATH_MERGED))
                    pos in alphaSet -> tileList.add(MapTile(pos, TileType.PATH_ALPHA))
                    pos in betaSet -> tileList.add(MapTile(pos, TileType.PATH_BETA))
                    else -> {
                        val adjacent = listOf(
                            GridPos(x + 1, y), GridPos(x - 1, y),
                            GridPos(x, y + 1), GridPos(x, y - 1)
                        )
                        val touchesAlpha = adjacent.any { it in alphaSet }
                        val touchesBeta = adjacent.any { it in betaSet }

                        if (touchesAlpha || touchesBeta) {
                            candidateBuffPositions.add(pos)
                        }
                        tileList.add(MapTile(pos, TileType.EMPTY))
                    }
                }
            }
        }

        // Guarantee 2 Green, 2 Teal, 2 Orange boosted pads
        candidateBuffPositions.shuffle(random)
        val greenPads = candidateBuffPositions.take(2).toSet()
        val tealPads = candidateBuffPositions.drop(2).take(2).toSet()
        val orangePads = candidateBuffPositions.drop(4).take(2).toSet()

        val finalTiles = tileList.map { tile ->
            when (tile.pos) {
                in greenPads -> tile.copy(buff = TileBuff.AMPLIFIER)
                in tealPads -> tile.copy(buff = TileBuff.HIGH_GROUND)
                in orangePads -> tile.copy(buff = TileBuff.CONDUIT)
                else -> tile
            }
        }

        return GeneratedLevel(
            seed = seed,
            width = width,
            height = height,
            biome = biome,
            tiles = finalTiles,
            laneAlpha = laneAlpha,
            laneBeta = laneBeta,
            intersectionPoints = intersections
        )
    }

    private fun generateWindingPath(
        start: GridPos,
        target: GridPos,
        width: Int,
        height: Int,
        random: Random,
        minSteps: Int,
        maxSteps: Int = 30
    ): List<GridPos> {
        var attempts = 0
        while (attempts < 40) {
            attempts++
            val path = mutableListOf(start)
            val visited = mutableSetOf(start)
            var current = start
            var stuck = false

            while (current != target && !stuck) {
                val neighbors = listOf(
                    GridPos(current.x + 1, current.y),
                    GridPos(current.x - 1, current.y),
                    GridPos(current.x, current.y + 1),
                    GridPos(current.x, current.y - 1)
                ).filter { next ->
                    next.x in 0 until width && next.y in 0 until height &&
                            next !in visited &&
                            (next == target || isNonSelfIntersectingStep(next, path))
                }

                if (neighbors.isEmpty()) {
                    stuck = true
                    break
                }

                if (target in neighbors && (path.size >= minSteps || path.size >= maxSteps - 2)) {
                    path.add(target)
                    return path
                }

                val sorted = neighbors.sortedBy { next ->
                    val dist = Math.hypot((next.x - target.x).toDouble(), (next.y - target.y).toDouble())
                    if (path.size >= maxSteps - 3) {
                        dist * 2.0 // Rapid direct homing to target to respect maxLaneLength
                    } else if (path.size < minSteps) {
                        dist * -0.5 + random.nextFloat() * 3.0 // Winding curve bias
                    } else {
                        dist + random.nextFloat() * 1.5
                    }
                }

                val chosen = sorted.first()
                path.add(chosen)
                visited.add(chosen)
                current = chosen
            }

            if (!stuck && current == target && path.size >= minSteps) {
                return path
            }
        }

        return fallbackManhattan(start, target)
    }

    private fun generateNonSelfIntersectingWinding(
        start: GridPos,
        target: GridPos,
        xMin: Int,
        xMax: Int,
        yMin: Int,
        yMax: Int,
        random: Random,
        minSteps: Int
    ): List<GridPos> {
        var attempts = 0
        while (attempts < 50) {
            attempts++
            val path = mutableListOf(start)
            val visited = mutableSetOf(start)
            var current = start
            var stuck = false

            while (current != target && !stuck) {
                val neighbors = listOf(
                    GridPos(current.x + 1, current.y),
                    GridPos(current.x - 1, current.y),
                    GridPos(current.x, current.y + 1),
                    GridPos(current.x, current.y - 1)
                ).filter { next ->
                    next.x in xMin..xMax && next.y in yMin..yMax &&
                            next !in visited &&
                            (next == target || isNonSelfIntersectingStep(next, path))
                }

                if (neighbors.isEmpty()) {
                    stuck = true
                    break
                }

                if (target in neighbors && path.size >= minSteps) {
                    path.add(target)
                    return path
                }

                val sorted = neighbors.sortedBy { next ->
                    val dist = Math.hypot((next.x - target.x).toDouble(), (next.y - target.y).toDouble())
                    if (path.size < minSteps) {
                        dist * -0.6 + random.nextFloat() * 2.5
                    } else {
                        dist + random.nextFloat() * 1.5
                    }
                }

                val chosen = sorted.first()
                path.add(chosen)
                visited.add(chosen)
                current = chosen
            }

            if (!stuck && current == target && path.size >= minSteps) {
                return path
            }
        }

        return fallbackManhattan(start, target)
    }

    private fun isNonSelfIntersectingStep(candidate: GridPos, path: List<GridPos>): Boolean {
        var touchingCount = 0
        for (i in 0 until path.size) {
            val p = path[i]
            if (Math.abs(p.x - candidate.x) + Math.abs(p.y - candidate.y) == 1) {
                touchingCount++
                if (i < path.size - 1) {
                    // Touches an earlier point on the path! Disallow to guarantee no self-crossings
                    return false
                }
            }
        }
        return touchingCount == 1
    }

    private fun fallbackManhattan(start: GridPos, target: GridPos): List<GridPos> {
        val path = mutableListOf<GridPos>()
        var cx = start.x
        var cy = start.y
        path.add(GridPos(cx, cy))

        while (cx != target.x) {
            cx += if (target.x > cx) 1 else -1
            path.add(GridPos(cx, cy))
        }
        while (cy != target.y) {
            cy += if (target.y > cy) 1 else -1
            path.add(GridPos(cx, cy))
        }
        return path
    }
}
