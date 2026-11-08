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

    fun generate(seed: Long = System.currentTimeMillis(), width: Int = DEFAULT_WIDTH, height: Int = DEFAULT_HEIGHT): GeneratedLevel {
        val random = Random(seed)
        val biome = BIOMES[random.nextInt(BIOMES.size)]

        val coreX = width / 2
        val corePos = GridPos(coreX, height - 1)

        // Spawners for both flanks
        val spawnerAlpha = GridPos(random.nextInt(1, 3), 0)
        val spawnerBeta = GridPos(random.nextInt(width - 3, width - 1), 0)

        // Brio Train-Track Central Rail Merge Segment
        val mergeX = if (random.nextBoolean()) width / 2 else (width / 2 - 1)
        val mergeStartY = 4
        val mergeLength = 5 // 5-cell continuous merged track spine for longer shared combat
        val mergeEndY = mergeStartY + mergeLength - 1
        val mergeStart = GridPos(mergeX, mergeStartY)
        val mergeEnd = GridPos(mergeX, mergeEndY)

        val mergedRail = (mergeStartY..mergeEndY).map { y -> GridPos(mergeX, y) }

        // Non-self-intersecting lead-in segments into the shared track (extended steps for longer winding paths)
        val leadAlpha = generateNonSelfIntersectingWinding(
            start = spawnerAlpha,
            target = mergeStart,
            xMin = 0,
            xMax = mergeX,
            yMin = 0,
            yMax = mergeStartY,
            random = random,
            minSteps = 9
        )

        val leadBeta = generateNonSelfIntersectingWinding(
            start = spawnerBeta,
            target = mergeStart,
            xMin = mergeX,
            xMax = width - 1,
            yMin = 0,
            yMax = mergeStartY,
            random = random,
            minSteps = 9
        )

        // Split off or converge naturally towards Core with extended winding
        val tailAlpha = generateNonSelfIntersectingWinding(
            start = mergeEnd,
            target = corePos,
            xMin = 0,
            xMax = width / 2,
            yMin = mergeEndY,
            yMax = height - 1,
            random = random,
            minSteps = 7
        )

        val tailBeta = generateNonSelfIntersectingWinding(
            start = mergeEnd,
            target = corePos,
            xMin = width / 2,
            xMax = width - 1,
            yMin = mergeEndY,
            yMax = height - 1,
            random = random,
            minSteps = 7
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
                        val touchesCrossing = adjacent.any { it in intersections }

                        val isObstacle = when {
                            touchesCrossing -> false // Prime tactical high ground near the merged rail!
                            touchesAlpha && touchesBeta -> false
                            touchesAlpha || touchesBeta -> random.nextFloat() < 0.05f
                            else -> random.nextFloat() < 0.18f
                        }

                        if (isObstacle) {
                            tileList.add(MapTile(pos, TileType.OBSTACLE))
                        } else {
                            if (touchesAlpha || touchesBeta) {
                                candidateBuffPositions.add(pos)
                            }
                            tileList.add(MapTile(pos, TileType.EMPTY))
                        }
                    }
                }
            }
        }

        // Apply tactical terrain buffs (+Damage, +Range, +Speed) near intersections and bends
        candidateBuffPositions.shuffle(random)
        val buffedTilesCount = minOf(candidateBuffPositions.size, random.nextInt(6, 10))
        val buffsToAssign = listOf(TileBuff.AMPLIFIER, TileBuff.HIGH_GROUND, TileBuff.CONDUIT)

        val finalTiles = tileList.map { tile ->
            val indexInCandidates = candidateBuffPositions.take(buffedTilesCount).indexOf(tile.pos)
            if (indexInCandidates >= 0) {
                tile.copy(buff = buffsToAssign[indexInCandidates % buffsToAssign.size])
            } else {
                tile
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
