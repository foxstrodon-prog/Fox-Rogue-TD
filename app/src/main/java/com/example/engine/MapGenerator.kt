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

        // Core located at Bottom-Right to maximize diagonal screen travel
        val corePos = GridPos(width - 2, height - 1)

        // Spawners: Top-Left (Alpha) and Top-Right (Beta)
        val spawnerAlpha = GridPos(1, 0)
        val spawnerBeta = GridPos(width - 2, 0)

        // Central Rail Merge Segment in middle rows (e.g. y = 5..8)
        val mergeX = if (random.nextBoolean()) width / 2 else (width / 2 - 1)
        val mergeStartY = 5
        val mergeLength = 4 // 4-cell continuous merged track spine
        val mergeEndY = mergeStartY + mergeLength - 1
        val mergeStart = GridPos(mergeX, mergeStartY)
        val mergeEnd = GridPos(mergeX, mergeEndY)

        val mergedRail = (mergeStartY..mergeEndY).map { y -> GridPos(mergeX, y) }

        // Non-self-intersecting lead-in segments:
        // Alpha sweeps across top-left quadrant into mergeStart
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

        // Beta sweeps across top-right quadrant into mergeStart
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

        // Split off segments from mergeEnd towards Bottom-Right Core:
        // Tail Alpha snakes down through the lower-left region first before crossing to Core
        val lowerLeftWaypoint = GridPos(1, height - 2)
        val tailAlphaLeg1 = generateNonSelfIntersectingWinding(
            start = mergeEnd,
            target = lowerLeftWaypoint,
            xMin = 0,
            xMax = mergeX,
            yMin = mergeEndY,
            yMax = height - 1,
            random = random,
            minSteps = 6
        )
        val tailAlphaLeg2 = generateNonSelfIntersectingWinding(
            start = lowerLeftWaypoint,
            target = corePos,
            xMin = 0,
            xMax = width - 1,
            yMin = height - 3,
            yMax = height - 1,
            random = random,
            minSteps = 5
        )
        val tailAlpha = (tailAlphaLeg1.dropLast(1) + tailAlphaLeg2).distinct()

        // Tail Beta winds through the right flank directly to Core
        val tailBeta = generateNonSelfIntersectingWinding(
            start = mergeEnd,
            target = corePos,
            xMin = mergeX,
            xMax = width - 1,
            yMin = mergeEndY,
            yMax = height - 1,
            random = random,
            minSteps = 8
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

                        // Eliminate random barriers so players have full build real estate
                        if (touchesAlpha || touchesBeta) {
                            candidateBuffPositions.add(pos)
                        }
                        tileList.add(MapTile(pos, TileType.EMPTY))
                    }
                }
            }
        }

        // Guarantee exactly 2 Green, 2 Teal, and 2 Orange boosted pads on every map!
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
