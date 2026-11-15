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
        maxLaneLength: Int = 32,
        targetOpenSlots: Int = 15,
        minLaneOverlap: Int = 4,
        maxLaneOverlap: Int = 12
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
        val clampedMinOverlap = minLaneOverlap.coerceIn(1, (width * height / 3).coerceAtLeast(1))
        val clampedMaxOverlap = maxLaneOverlap.coerceAtLeast(clampedMinOverlap)
        val targetOverlap = if (clampedMaxOverlap > clampedMinOverlap) {
            random.nextInt(clampedMinOverlap, clampedMaxOverlap + 1)
        } else {
            clampedMinOverlap
        }

        val centerX = width / 2
        val centerY = height / 2

        val mergedRail: List<GridPos> = if (height >= width) {
            if (targetOverlap <= height - 2) {
                val startY = (centerY - targetOverlap / 2).coerceIn(1, (height - 1 - targetOverlap).coerceAtLeast(1))
                (0 until targetOverlap).map { GridPos(centerX, startY + it) }
            } else {
                val col1 = (1 until height - 1).map { GridPos(centerX, it) }
                val nextX = if (centerX + 1 < width - 1) centerX + 1 else (centerX - 1).coerceAtLeast(1)
                val remaining = targetOverlap - col1.size
                val col2 = (height - 2 downTo (height - 1 - remaining).coerceAtLeast(1)).map { GridPos(nextX, it) }
                (col1 + col2).take(targetOverlap)
            }
        } else {
            if (targetOverlap <= width - 2) {
                val startX = (centerX - targetOverlap / 2).coerceIn(1, (width - 1 - targetOverlap).coerceAtLeast(1))
                (0 until targetOverlap).map { GridPos(startX + it, centerY) }
            } else {
                val row1 = (1 until width - 1).map { GridPos(it, centerY) }
                val nextY = if (centerY + 1 < height - 1) centerY + 1 else (centerY - 1).coerceAtLeast(1)
                val remaining = targetOverlap - row1.size
                val row2 = (width - 2 downTo (width - 1 - remaining).coerceAtLeast(1)).map { GridPos(it, nextY) }
                (row1 + row2).take(targetOverlap)
            }
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

        val blockedLeadBeta1 = (leadAlpha - setOf(mergeStart)).toSet()
        val leadBetaLeg1 = removeLoops(findPath(spawnerBeta, cLeadBeta, width, height, random, blocked = blockedLeadBeta1))
        val entryBeta = if (leadBetaLeg1.size >= 2) leadBetaLeg1[leadBetaLeg1.size - 2] else null
        val blockedLeadBeta2 = (blockedLeadBeta1 + leadBetaLeg1.dropLast(1) + listOfNotNull(entryBeta)).toSet()
        val leadBetaLeg2 = removeLoops(findPath(cLeadBeta, mergeStart, width, height, random, blocked = blockedLeadBeta2))
        val leadBeta = leadBetaLeg1.dropLast(1) + leadBetaLeg2

        // 5. Split tail segments winding towards remaining corners then converging to Core
        val blockedTailAlpha1 = (leadAlpha + mergedRail.dropLast(1)).toSet() - setOf(mergeEnd, cTailAlpha)
        val tailAlphaLeg1 = removeLoops(findPath(mergeEnd, cTailAlpha, width, height, random, blocked = blockedTailAlpha1))
        val entryTailAlpha = if (tailAlphaLeg1.size >= 2) tailAlphaLeg1[tailAlphaLeg1.size - 2] else null
        val blockedTailAlpha2 = (blockedTailAlpha1 + tailAlphaLeg1.dropLast(1) + listOfNotNull(entryTailAlpha)).toSet() - setOf(cTailAlpha, corePos)
        val tailAlphaLeg2 = removeLoops(findPath(cTailAlpha, corePos, width, height, random, blocked = blockedTailAlpha2))
        val tailAlpha = tailAlphaLeg1.dropLast(1) + tailAlphaLeg2

        val blockedTailBeta1 = (leadBeta + mergedRail.dropLast(1) + tailAlpha.dropLast(1)).toSet() - setOf(mergeEnd, cTailBeta)
        val tailBetaLeg1 = removeLoops(findPath(mergeEnd, cTailBeta, width, height, random, blocked = blockedTailBeta1))
        val entryTailBeta = if (tailBetaLeg1.size >= 2) tailBetaLeg1[tailBetaLeg1.size - 2] else null
        val blockedTailBeta2 = (blockedTailBeta1 + tailBetaLeg1.dropLast(1) + listOfNotNull(entryTailBeta)).toSet() - setOf(cTailBeta, corePos)
        val tailBetaLeg2 = removeLoops(findPath(cTailBeta, corePos, width, height, random, blocked = blockedTailBeta2))
        val tailBeta = tailBetaLeg1.dropLast(1) + tailBetaLeg2

        val laneAlpha = (leadAlpha.dropLast(1) + mergedRail.dropLast(1) + tailAlpha).toMutableList()
        val laneBeta = (leadBeta.dropLast(1) + mergedRail.dropLast(1) + tailBeta).toMutableList()

        val protectedSet = setOf(spawnerAlpha, spawnerBeta, corePos)
        val occupied = (laneAlpha.toSet() + laneBeta.toSet() + protectedSet).toMutableSet()
        var emptyCount = width * height - occupied.size

        // 6. Parity adjustment: If emptyCount and targetOpenSlots have different parities,
        // use a 1-tile detour across an existing other-lane tile to flip parity by 1.
        if (Math.abs(emptyCount - targetOpenSlots) % 2 != 0) {
            val fixed = trySingleTileDetour(laneAlpha, laneBeta.toSet(), occupied, width, height, protectedSet) ||
                trySingleTileDetour(laneBeta, laneAlpha.toSet(), occupied, width, height, protectedSet)
            if (fixed) {
                emptyCount--
            }
        }

        // 7. Dense Winding Lane Expansion:
        // Iteratively expand straight segments of laneAlpha and laneBeta into adjacent unused empty tiles
        // using 2-tile U-bends (A -> B replaced by A -> A' -> B' -> B) until emptyCount <= targetOpenSlots.
        data class UBendCandidate(val isAlpha: Boolean, val index: Int, val aPrime: GridPos, val bPrime: GridPos)

        var stallCount = 0
        while (emptyCount >= targetOpenSlots + 2 && stallCount < 10) {
            val candidates = mutableListOf<UBendCandidate>()

            val alphaSet = laneAlpha.toSet()
            for (i in 0 until laneAlpha.size - 1) {
                val a = laneAlpha[i]
                val b = laneAlpha[i + 1]
                val pairs = if (a.x == b.x) {
                    listOf(-1, 1).map { dx -> GridPos(a.x + dx, a.y) to GridPos(b.x + dx, b.y) }
                } else {
                    listOf(-1, 1).map { dy -> GridPos(a.x, a.y + dy) to GridPos(b.x, b.y + dy) }
                }
                for ((aPrime, bPrime) in pairs) {
                    if (aPrime.x in 0 until width && aPrime.y in 0 until height &&
                        bPrime.x in 0 until width && bPrime.y in 0 until height &&
                        aPrime !in protectedSet && bPrime !in protectedSet &&
                        aPrime !in occupied && bPrime !in occupied &&
                        aPrime !in alphaSet && bPrime !in alphaSet
                    ) {
                        candidates.add(UBendCandidate(true, i, aPrime, bPrime))
                    }
                }
            }

            val betaSet = laneBeta.toSet()
            for (i in 0 until laneBeta.size - 1) {
                val a = laneBeta[i]
                val b = laneBeta[i + 1]
                val pairs = if (a.x == b.x) {
                    listOf(-1, 1).map { dx -> GridPos(a.x + dx, a.y) to GridPos(b.x + dx, b.y) }
                } else {
                    listOf(-1, 1).map { dy -> GridPos(a.x, a.y + dy) to GridPos(b.x, b.y + dy) }
                }
                for ((aPrime, bPrime) in pairs) {
                    if (aPrime.x in 0 until width && aPrime.y in 0 until height &&
                        bPrime.x in 0 until width && bPrime.y in 0 until height &&
                        aPrime !in protectedSet && bPrime !in protectedSet &&
                        aPrime !in occupied && bPrime !in occupied &&
                        aPrime !in betaSet && bPrime !in betaSet
                    ) {
                        candidates.add(UBendCandidate(false, i, aPrime, bPrime))
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                val pick = candidates[random.nextInt(candidates.size)]
                if (pick.isAlpha) {
                    laneAlpha.addAll(pick.index + 1, listOf(pick.aPrime, pick.bPrime))
                } else {
                    laneBeta.addAll(pick.index + 1, listOf(pick.aPrime, pick.bPrime))
                }
                occupied.add(pick.aPrime)
                occupied.add(pick.bPrime)
                emptyCount -= 2
                stallCount = 0
            } else {
                stallCount++
            }
        }

        val finalAlphaSet = laneAlpha.toSet()
        val finalBetaSet = laneBeta.toSet()
        val intersections = (finalAlphaSet.intersect(finalBetaSet) - setOf(corePos, spawnerAlpha, spawnerBeta)).toSet()

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
                    pos in finalAlphaSet -> tileList.add(MapTile(pos, TileType.PATH_ALPHA))
                    pos in finalBetaSet -> tileList.add(MapTile(pos, TileType.PATH_BETA))
                    else -> {
                        val adjacent = listOf(
                            GridPos(x + 1, y), GridPos(x - 1, y),
                            GridPos(x, y + 1), GridPos(x, y - 1)
                        )
                        val touchesAlpha = adjacent.any { it in finalAlphaSet }
                        val touchesBeta = adjacent.any { it in finalBetaSet }

                        if (touchesAlpha || touchesBeta) {
                            candidateBuffPositions.add(pos)
                        }
                        tileList.add(MapTile(pos, TileType.EMPTY))
                    }
                }
            }
        }

        // Guarantee 2 Green, 2 Teal, 2 Orange boosted pads (6 total) selected from empty tiles adjacent to lanes.
        // Track 3.1: Enforce minimum separation distance (Manhattan >= 4) between pads of the same color.
        val emptyTilePositions = tileList.filter { it.type == TileType.EMPTY }.map { it.pos }
        val pool = (candidateBuffPositions.shuffled(random) + emptyTilePositions.filter { it !in candidateBuffPositions }.shuffled(random)).distinct().toMutableList()

        fun selectSeparatedPair(): Pair<GridPos, GridPos>? {
            if (pool.size < 2) return null
            val p1 = pool.removeAt(0)
            val farCandidates = pool.filter { (kotlin.math.abs(it.x - p1.x) + kotlin.math.abs(it.y - p1.y)) >= 4 }
            val p2 = if (farCandidates.isNotEmpty()) {
                val chosen = farCandidates.random(random)
                pool.remove(chosen)
                chosen
            } else {
                val nonAdjacent = pool.filter { (kotlin.math.abs(it.x - p1.x) + kotlin.math.abs(it.y - p1.y)) > 1 }
                val fallbackPool = if (nonAdjacent.isNotEmpty()) nonAdjacent else pool
                val chosen = fallbackPool.maxByOrNull { kotlin.math.abs(it.x - p1.x) + kotlin.math.abs(it.y - p1.y) } ?: pool.first()
                pool.remove(chosen)
                chosen
            }
            return Pair(p1, p2)
        }

        val greenPair = selectSeparatedPair()
        val tealPair = selectSeparatedPair()
        val orangePair = selectSeparatedPair()

        val greenPads = listOfNotNull(greenPair?.first, greenPair?.second).toSet()
        val tealPads = listOfNotNull(tealPair?.first, tealPair?.second).toSet()
        val orangePads = listOfNotNull(orangePair?.first, orangePair?.second).toSet()

        val finalTiles = tileList.map { tile ->
            when {
                tile.pos in greenPads -> tile.copy(buff = TileBuff.AMPLIFIER)
                tile.pos in tealPads -> tile.copy(buff = TileBuff.HIGH_GROUND)
                tile.pos in orangePads -> tile.copy(buff = TileBuff.CONDUIT)
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

    private fun trySingleTileDetour(
        primaryLane: MutableList<GridPos>,
        otherLaneSet: Set<GridPos>,
        occupied: MutableSet<GridPos>,
        width: Int,
        height: Int,
        protectedSet: Set<GridPos>
    ): Boolean {
        val laneSet = primaryLane.toSet()
        for (i in 0 until primaryLane.size - 1) {
            val a = primaryLane[i]
            val b = primaryLane[i + 1]
            val candidates = if (a.x == b.x) {
                listOf(-1, 1).map { dx -> GridPos(a.x + dx, a.y) to GridPos(b.x + dx, b.y) }
            } else {
                listOf(-1, 1).map { dy -> GridPos(a.x, a.y + dy) to GridPos(b.x, b.y + dy) }
            }
            for ((p1, p2) in candidates) {
                if (p1.x !in 0 until width || p1.y !in 0 until height) continue
                if (p2.x !in 0 until width || p2.y !in 0 until height) continue
                if (p1 in protectedSet || p2 in protectedSet) continue
                if (p1 in laneSet || p2 in laneSet) continue

                if (p1 in otherLaneSet && p2 !in occupied) {
                    primaryLane.addAll(i + 1, listOf(p1, p2))
                    occupied.add(p2)
                    return true
                } else if (p2 in otherLaneSet && p1 !in occupied) {
                    primaryLane.addAll(i + 1, listOf(p1, p2))
                    occupied.add(p1)
                    return true
                }
            }
        }
        return false
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
