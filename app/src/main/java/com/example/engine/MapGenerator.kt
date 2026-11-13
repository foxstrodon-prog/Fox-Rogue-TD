package com.example.engine

import androidx.compose.ui.graphics.Color
import com.example.model.*
import kotlin.random.Random

typealias GeneratedLevel = com.example.model.GeneratedLevel

object MapGenerator {
    const val DEFAULT_WIDTH = 10
    const val DEFAULT_HEIGHT = 20

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
        minLaneLength: Int = 16,
        maxLaneLength: Int = 32
    ): GeneratedLevel {
        require(minLaneLength in 4..maxLaneLength) {
            "minLaneLength ($minLaneLength) must be >= 4 and <= maxLaneLength ($maxLaneLength)"
        }
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

        // 3. Guarantee that lanes touch all 4 corners of the map
        val cornerTL = GridPos(0, 0)
        val cornerTR = GridPos(width - 1, 0)
        val cornerBL = GridPos(0, height - 1)
        val cornerBR = GridPos(width - 1, height - 1)
        val corners = mutableListOf(cornerTL, cornerTR, cornerBL, cornerBR)

        // Assign corners to legs based on spatial proximity
        val cLeadAlpha = corners.minByOrNull { Math.hypot((it.x - spawnerAlpha.x).toDouble(), (it.y - spawnerAlpha.y).toDouble()) }!!
        corners.remove(cLeadAlpha)

        val cLeadBeta = corners.minByOrNull { Math.hypot((it.x - spawnerBeta.x).toDouble(), (it.y - spawnerBeta.y).toDouble()) }!!
        corners.remove(cLeadBeta)

        val cTailBeta = corners.minByOrNull { Math.hypot((it.x - corePos.x).toDouble(), (it.y - corePos.y).toDouble()) }!!
        corners.remove(cTailBeta)
        val cTailAlpha = corners.first()

        // 4. Multi-directional winding lead-ins touching outer corners
        val leadAlphaLeg1 = removeLoops(findPath(spawnerAlpha, cLeadAlpha, width, height, random))
        val entryAlpha = if (leadAlphaLeg1.size >= 2) leadAlphaLeg1[leadAlphaLeg1.size - 2] else null
        val blockedLeadAlpha2 = (leadAlphaLeg1.dropLast(1) + listOfNotNull(entryAlpha)).toSet()
        val leadAlphaLeg2 = removeLoops(findPath(cLeadAlpha, mergeStart, width, height, random, blocked = blockedLeadAlpha2))
        val leadAlpha = leadAlphaLeg1.dropLast(1) + leadAlphaLeg2

        val leadBetaLeg1 = removeLoops(findPath(spawnerBeta, cLeadBeta, width, height, random))
        val entryBeta = if (leadBetaLeg1.size >= 2) leadBetaLeg1[leadBetaLeg1.size - 2] else null
        val blockedLeadBeta2 = (leadBetaLeg1.dropLast(1) + listOfNotNull(entryBeta)).toSet()
        val leadBetaLeg2 = removeLoops(findPath(cLeadBeta, mergeStart, width, height, random, blocked = blockedLeadBeta2))
        val leadBeta = leadBetaLeg1.dropLast(1) + leadBetaLeg2

        // 5. Split tail segments winding towards remaining corners then converging to Core
        val blockedTailAlpha1 = (leadAlpha + mergedRail.dropLast(1)).toSet() - setOf(mergeEnd, cTailAlpha)
        val tailAlphaLeg1 = removeLoops(findPath(mergeEnd, cTailAlpha, width, height, random, blocked = blockedTailAlpha1))
        val entryTailAlpha = if (tailAlphaLeg1.size >= 2) tailAlphaLeg1[tailAlphaLeg1.size - 2] else null
        val blockedTailAlpha2 = (blockedTailAlpha1 + tailAlphaLeg1.dropLast(1) + listOfNotNull(entryTailAlpha)).toSet() - setOf(cTailAlpha, corePos)
        val tailAlphaLeg2 = removeLoops(findPath(cTailAlpha, corePos, width, height, random, blocked = blockedTailAlpha2))
        val tailAlpha = tailAlphaLeg1.dropLast(1) + tailAlphaLeg2

        val blockedTailBeta1 = (leadBeta + mergedRail.dropLast(1)).toSet() - setOf(mergeEnd, cTailBeta)
        val tailBetaLeg1 = removeLoops(findPath(mergeEnd, cTailBeta, width, height, random, blocked = blockedTailBeta1))
        val entryTailBeta = if (tailBetaLeg1.size >= 2) tailBetaLeg1[tailBetaLeg1.size - 2] else null
        val blockedTailBeta2 = (blockedTailBeta1 + tailBetaLeg1.dropLast(1) + listOfNotNull(entryTailBeta)).toSet() - setOf(cTailBeta, corePos)
        val tailBetaLeg2 = removeLoops(findPath(cTailBeta, corePos, width, height, random, blocked = blockedTailBeta2))
        val tailBeta = tailBetaLeg1.dropLast(1) + tailBetaLeg2

        val laneAlpha = leadAlpha.dropLast(1) + mergedRail.dropLast(1) + tailAlpha
        val laneBeta = leadBeta.dropLast(1) + mergedRail.dropLast(1) + tailBeta

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

    private fun removeLoops(path: List<GridPos>): List<GridPos> {
        val result = mutableListOf<GridPos>()
        for (pos in path) {
            val idx = result.indexOf(pos)
            if (idx >= 0) {
                while (result.size > idx + 1) {
                    result.removeAt(result.size - 1)
                }
            } else {
                result.add(pos)
            }
        }
        return result
    }

    private fun findPath(
        start: GridPos,
        target: GridPos,
        width: Int,
        height: Int,
        random: Random,
        blocked: Set<GridPos> = emptySet()
    ): List<GridPos> {
        if (start == target) return listOf(start)

        fun search(blockedPenalty: Int): List<GridPos>? {
            val cameFrom = mutableMapOf<GridPos, GridPos>()
            val costSoFar = mutableMapOf<GridPos, Int>()
            val openSet = java.util.PriorityQueue<Pair<GridPos, Double>>(compareBy { it.second })

            openSet.add(start to 0.0)
            costSoFar[start] = 0

            while (openSet.isNotEmpty()) {
                val (current, _) = openSet.poll() ?: break
                if (current == target) {
                    val path = mutableListOf<GridPos>()
                    var curr: GridPos? = target
                    while (curr != null) {
                        path.add(curr)
                        curr = cameFrom[curr]
                    }
                    return path.reversed()
                }

                val neighbors = listOf(
                    GridPos(current.x + 1, current.y),
                    GridPos(current.x - 1, current.y),
                    GridPos(current.x, current.y + 1),
                    GridPos(current.x, current.y - 1)
                ).filter { next ->
                    next.x in 0 until width && next.y in 0 until height
                }.shuffled(random)

                for (next in neighbors) {
                    val isBlocked = next != target && next in blocked
                    if (isBlocked && blockedPenalty >= 10000) continue
                    val stepCost = if (isBlocked) blockedPenalty else 1
                    val newCost = costSoFar[current]!! + stepCost
                    if (newCost < (costSoFar[next] ?: Int.MAX_VALUE)) {
                        costSoFar[next] = newCost
                        cameFrom[next] = current
                        val h = Math.abs(next.x - target.x) + Math.abs(next.y - target.y)
                        val jitter = random.nextDouble() * 0.5
                        openSet.add(next to (newCost + h + jitter))
                    }
                }
            }
            return null
        }

        return search(10000) ?: search(100) ?: fallbackManhattan(start, target)
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
