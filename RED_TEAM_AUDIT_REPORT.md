# ==============================================================================
# RED TEAM ADVERSARIAL CS & META-PROMPT AUDIT DOSSIER
# Target: Dual Front TD (v0.0.1.015)
# Authority: Independent External Systems Architect & AI Alignment Auditor
# Reporting Line: DIRECTLY TO THE HUMAN CREATOR
# Mandate: 100% READ-ONLY AUDIT • ZERO EXECUTIVE IMMUNITY • PSEUDOCODE FIRST
# ==============================================================================

## EXECUTIVE SUMMARY

This adversarial forensic dossier was compiled under an absolute read-only mandate to audit the application source files (`app/src/main/java/...`) and the agent governance system (`.agents/agents/...`).

The core finding of this audit is an **acute architectural schizophrenia** between stated governance principles and actual production code:
1. **The Performance Illusion:** While agent instructions for Dr. Alan and Leo repeatedly mandate *"Zero-Allocation 60/120 FPS simulation loops"*, actual hot-loop execution in `RogueTDViewModel.kt` and `GameCanvas.kt` generates between **700 KB/sec and 1,470 KB/sec of heap garbage** (44 to 88 MB/minute), generating over 100 object allocations per frame. This guarantees continuous ART Nursery Garbage Collection thrashing and micro-stutter frame drops on Android devices during Wave 50+ swarm encounters.
2. **The Executive Immunity & YouTube Sidetrack Trap:** The Executive Meta-Council (Sterling, Vance, Trent, Sloan, Alec) and Squad Orchestrator (Frank) constructed an elaborate verification pipeline for DaVinci Resolve timelines, 9:16 vertical video framing, YouTube thumbnail packaging trifectas, and UI orientation, but **completely omitted memory allocation profiling, AST hot-path inspection, and GC pause gates**.
3. **The 2,500-Line Monolithic God Object:** `RogueTDViewModel.kt` (2,495 lines) entangles low-level physics, audio dispatch, wave generation, UI StateFlow copying, and 6 scripted in-game cinematics simulating fake human tap gestures, violating every principle of modular software craftsmanship.
4. **Numerical Stability Time Bombs:** Infinite mode scaling ($1.08^{\text{wave}-50}$) lacks an asymptotic upper ceiling, mathematically guaranteeing overflow to `Float.POSITIVE_INFINITY` at extreme waves ($>1,180$), breaking all damage subtractions and creating permanently immortal enemies, while stacked Cryo frost slow multipliers legitimately produce negative movement vectors that walk enemies backwards out of the map.

---

## STAGE 1: THE NAKED PSEUDOCODE MAP (HOT PATHS & STATE MUTATIONS)

*All functional syntax sugar (`.filter`, `.map`, `.flatMap`, `.forEach`, `.any`, `.maxByOrNull`, `.firstOrNull`) has been unrolled into explicit language-agnostic primitive loops with explicit heap allocation annotations.*

### Pipeline 1: Simulation Tick Loop (`RogueTDViewModel.kt#tickSimulation`)
```text
FUNCTION tickSimulation(deltaSec: Float, deltaMs: Long, nowMs: Long):
    virtualSimTimeMs = virtualSimTimeMs + deltaMs
    CALL soundManager.updateTick(nowMs, deltaMs)
    
    // [HEAP ALLOC: GameUIState reference snapshot access]
    state = _uiState.value
    level = state.level
    IF level == null: RETURN

    // [HEAP ALLOC: ArrayList, initial capacity = state.enemies.size]
    ALLOCATE currentEnemies = ArrayList(state.enemies.size)
    FOR i = 0 TO state.enemies.size - 1:
        currentEnemies.add(state.enemies[i])

    // --- SUB-LOOP: Track Particles Vector Advance ---
    // [HEAP ALLOC: ArrayList, capacity = state.trackParticles.size]
    ALLOCATE updatedParticles = ArrayList(state.trackParticles.size)
    // [HEAP ALLOC: Iterator<TrackParticle>]
    FOR EACH p IN state.trackParticles: // Runs P_particles times (typically 40-80)
        // [HEAP ALLOC: TrackParticle.copy() instance]
        ALLOCATE p_copy = TrackParticle.copy(p, segmentIndex = newSeg, progress = newProg)
        updatedParticles.add(p_copy)

    // --- SUB-LOOP: Dual-Lane Spawner ---
    IF state.isWaveActive AND pendingSpawns.isNotEmpty():
        nextSpawnTimeMs = nextSpawnTimeMs - deltaMs
        IF nextSpawnTimeMs <= 0:
            spawn = pendingSpawns.removeAt(0)
            // [HEAP ALLOC: java.util.UUID, 16 bytes]
            // [HEAP ALLOC: String, 36 chars + char array = ~88 bytes]
            // [HEAP ALLOC: EnemyInstance object, ~128 bytes]
            ALLOCATE enemyId = UUID.randomUUID().toString()
            ALLOCATE newEnemy = EnemyInstance(id = enemyId, ...)
            currentEnemies.add(newEnemy)

    // --- SUB-LOOP: Dual-Lane Movement & Status ---
    // [HEAP ALLOC: ArrayList, default capacity 10]
    ALLOCATE survivingEnemies = ArrayList()
    coreDamageTaken = 0

    // [HEAP ALLOC: Iterator<EnemyInstance>]
    FOR EACH enemy IN currentEnemies: // Runs E_enemies times (50-200+ in swarm waves)
        IF enemy.currentPathSegmentIndex >= path.size - 1:
            coreDamageTaken = coreDamageTaken + (IF enemy.isBoss THEN 6 ELSE 1)
        ELSE:
            // Linear path interpolation
            enemy.worldX = p0.x + (p1.x - p0.x) * t
            enemy.worldY = p0.y + (p1.y - p0.y) * t
            survivingEnemies.add(enemy)

    // [HEAP ALLOC: SpatialHashGrid internal clear + bucket map entry array allocations]
    CALL spatialGrid.rebuild(survivingEnemies)
```

### Pipeline 2: Target Acquisition & Turret Weapon Fire (`RogueTDViewModel.kt#tickSimulation`)
```text
    // --- Dynamic Global Buff Recalculation (RUNS EVERY 16ms UNNECESSARILY) ---
    // [HEAP ALLOC: ArrayList, capacity = state.towers.size]
    ALLOCATE finalFormDefs = ArrayList()
    FOR EACH it IN state.towers: // Runs T_towers times (up to 14)
        IF it.isFinalForm:
            finalFormDefs.add(TowerUpgradeManager.getFinalForm(it.type, it.finalFormCode))

    // --- Redundant Tile Buff Filtering ---
    // [HEAP ALLOC: ArrayList, size <= level.tiles.size]
    ALLOCATE boostedTiles = ArrayList()
    FOR EACH tile IN level.tiles: // Runs 60-100 times per tick!
        IF tile.buff != TileBuff.NONE:
            boostedTiles.add(tile)

    // [O(T * B) Nested Loop Allocation]
    harmonicCount = 0
    FOR EACH t IN towers:
        FOR EACH bt IN boostedTiles:
            IF bt.pos == t.gridPos:
                harmonicCount = harmonicCount + 1
                BREAK

    // --- Hot Tower Firing Loop ---
    FOR EACH tower IN towers: // Runs T_towers times per frame (e.g., 14)
        // Redundant tile resolution per tower
        tile = NULL
        FOR EACH t_tile IN level.tiles:
            IF t_tile.pos == tower.gridPos:
                tile = t_tile; BREAK

        // [HEAP ALLOC: ArrayList from spatialGrid.queryNearby()]
        ALLOCATE candidateBuckets = spatialGrid.queryNearby(towerX, towerY, effectiveRange + 0.6f)
        
        // [HEAP ALLOC: ArrayList from .filter { ... }]
        ALLOCATE inRangeEnemies = ArrayList()
        // [HEAP ALLOC: Iterator<EnemyInstance>]
        FOR EACH enemy IN candidateBuckets:
            // [CPU OVERHEAD: Strict Math.hypot() calculates Math.sqrt() on every candidate]
            dist = hypot(enemy.worldX - tower.gridPos.x, enemy.worldY - tower.gridPos.y)
            IF dist <= (effectiveRange + enemy.type.hitRadius):
                inRangeEnemies.add(enemy)

        IF inRangeEnemies.isEmpty():
            tower.targetEnemyId = NULL
            CONTINUE

        // [HEAP ALLOC: Comparator lambda / MaxByOrNull evaluation]
        target = inRangeEnemies.maxByOrNull { it.currentPathSegmentIndex + it.segmentProgress }

        // Weapon Fire Execution
        IF simNowMs - tower.lastShotTimeMs >= cooldownMs:
            tower.lastShotTimeMs = simNowMs

            IF tower.type == TowerType.GATLING:
                // [HEAP ALLOC: java.util.UUID, 16 bytes]
                // [HEAP ALLOC: String, 36 chars = 88 bytes]
                // [HEAP ALLOC: Projectile data class instance, ~112 bytes]
                ALLOCATE projId = UUID.randomUUID().toString()
                ALLOCATE projectile = Projectile(id = projId, ...)
                newProjectiles.add(projectile)

            ELSE IF tower.type == TowerType.TESLA:
                // [HEAP ALLOC: ArrayList for chainTargets]
                ALLOCATE chainTargets = ArrayList()
                chainTargets.add(target)
                // [HEAP ALLOC: ArrayList from .filter { ... }]
                // [HEAP ALLOC: ArrayList from .toMutableList()]
                ALLOCATE remaining = ArrayList()
                FOR EACH se IN survivingEnemies:
                    IF se.id != target.id: remaining.add(se)

                WHILE chainTargets.size < maxChains AND remaining.isNotEmpty():
                    last = chainTargets.last()
                    // [O(Remaining_Enemies) distance checks per jump]
                    next = remaining.minByOrNull { hypot(it.worldX - last.worldX, it.worldY - last.worldY) }
                    IF next != null AND hypot(next.worldX - last.worldX, next.worldY - last.worldY) < 3.4f:
                        chainTargets.add(next)
                        remaining.remove(next)
                    ELSE: BREAK

                // [HEAP ALLOC: ArrayList of Points]
                ALLOCATE arcPoints = ArrayList()
                // [HEAP ALLOC: kotlin.Pair<Float, Float> for tower point]
                arcPoints.add(Pair(towerX, towerY))
                FOR EACH ct IN chainTargets:
                    // [HEAP ALLOC: kotlin.Pair<Float, Float> per chain node = 24 bytes each]
                    arcPoints.add(Pair(ct.worldX, ct.worldY))
                // [HEAP ALLOC: LightningArc data class instance]
                ALLOCATE arcId = UUID.randomUUID().toString()
                newArcs.add(LightningArc(arcId, arcPoints, ...))
```

### Pipeline 3: Collision Resolution & Entity Splitting (`RogueTDViewModel.kt`)
```text
    // [HEAP ALLOC: ArrayList for survivingProjectiles]
    ALLOCATE survivingProjectiles = ArrayList()

    FOR EACH proj IN newProjectiles: // Runs P_projectiles times (50-200 active bullets)
        // [O(E) linear search executed TWICE per bullet per frame]
        destX = proj.targetX
        destY = proj.targetY
        IF proj.targetEnemyId != null:
            FOR EACH se IN survivingEnemies:
                IF se.id == proj.targetEnemyId:
                    destX = se.worldX; BREAK
            FOR EACH se IN survivingEnemies:
                IF se.id == proj.targetEnemyId:
                    destY = se.worldY; BREAK

        dx = destX - proj.currentX
        dy = destY - proj.currentY
        dist = hypot(dx, dy)
        step = proj.speed * deltaSec

        IF dist <= step OR dist < 0.25f:
            IF proj.isSplash: // Mortar / Void splash
                // [HEAP ALLOC: ExplosionEffect data class]
                newExplosions.add(ExplosionEffect(UUID.randomUUID().toString(), ...))
                
                // [O(E) linear scan over ALL surviving enemies]
                // [HEAP ALLOC: ArrayList from .filter { ... }]
                ALLOCATE splashTargets = ArrayList()
                FOR EACH se IN survivingEnemies:
                    IF hypot(se.worldX - destX, se.worldY - destY) <= proj.splashRadius:
                        splashTargets.add(se)

                FOR EACH st IN splashTargets:
                    CALL applyDamageToEnemy(st, proj.damage, ...)

                IF proj.hasClusterSplit: // Cluster munitions: 3 sub-explosions
                    FOR i = 0 TO 2:
                        // [HEAP ALLOC: 3 x ExplosionEffect instances]
                        // [HEAP ALLOC: 3 x ArrayLists for clusterTargets]
                        // [3 x O(E) linear scans over all enemies]
                        ALLOCATE clusterTargets = ArrayList()
                        FOR EACH se IN survivingEnemies:
                            IF hypot(se.worldX - subX, se.worldY - subY) <= 0.9f:
                                clusterTargets.add(se)
            ELSE:
                // Direct Gatling bullet hit
                hitEnemy = NULL
                FOR EACH se IN survivingEnemies:
                    IF se.id == proj.targetEnemyId: hitEnemy = se; BREAK
                IF hitEnemy != null:
                    CALL applyDamageToEnemy(hitEnemy, proj.damage, ...)
                    IF proj.hasClusterSplit: // Ricochet
                        // [HEAP ALLOC: Predicate lambda]
                        // [O(E) linear search for bounce target with hypot calculation]
                        bounceTarget = survivingEnemies.firstOrNull { it.id != hitEnemy.id AND hypot(...) < 2.5f }
                        IF bounceTarget != null: CALL applyDamageToEnemy(bounceTarget, ...)
        ELSE:
            proj.currentX = proj.currentX + (dx / dist) * step
            proj.currentY = proj.currentY + (dy / dist) * step
            survivingProjectiles.add(proj)
```

### Pipeline 4: Synergy & Global Aura Aggregation (`RogueTDViewModel.kt#computeActiveSynergies`)
```text
FUNCTION computeActiveSynergies(towers: List<TowerInstance>): List<GlobalAuraSynergy>
    // [CALLED EVERY 16ms INSIDE THE 60 FPS TICK LOOP]
    
    // [HEAP ALLOC: ArrayList from .filter { it.isTier3 }]
    ALLOCATE tier3Towers = ArrayList()
    FOR EACH t IN towers:
        IF t.isTier3: tier3Towers.add(t)

    // [HEAP ALLOC: java.util.LinkedHashMap]
    ALLOCATE counts = LinkedHashMap()
    FOR EACH t IN tier3Towers:
        arch = t.archetype ?: mapTypeToArchetype(t.type)
        // [HEAP ALLOC: kotlin.Pair<TowerArchetype, Int> key object, 24 bytes]
        // [HEAP ALLOC: java.lang.Integer boxing for map count, 16 bytes]
        ALLOCATE key = Pair(arch, t.selectedAuraIndex)
        counts[key] = (counts[key] ?: 0) + 1

    // [HEAP ALLOC: ArrayList from counts.map { ... }]
    ALLOCATE activeList = ArrayList(counts.size)
    // [HEAP ALLOC: Map.Entry iterator]
    FOR EACH entry IN counts.entries:
        // [HEAP ALLOC: GlobalAuraSynergy data class instance, ~64 bytes]
        ALLOCATE aura = GlobalAuraSynergy(...)
        activeList.add(aura)

    capstone = NULL
    FOR EACH t IN towers:
        IF t.isCapstone: capstone = t; BREAK

    IF capstone != null AND capstone.synergyVault.isNotEmpty():
        // [HEAP ALLOC: ArrayList from .flatMap { it.synergies }]
        ALLOCATE bankedList = ArrayList()
        FOR EACH vault IN capstone.synergyVault:
            FOR EACH s IN vault.synergies:
                bankedList.add(s)

        FOR EACH bs IN bankedList:
            // [O(N_active) string equality scan]
            existingIndex = activeList.indexOfFirst { it.auraName == bs.auraName }
            IF existingIndex >= 0:
                // [HEAP ALLOC: GlobalAuraSynergy.copy()]
                activeList[existingIndex] = activeList[existingIndex].copy(...)
            ELSE:
                activeList.add(bs)

    RETURN activeList
```

### Pipeline 5: Compose Render Loop (`GameCanvas.kt#Canvas`)
```text
FUNCTION GameCanvas.Canvas.DrawScope.onDraw():
    // [RUNS 60-120 TIMES PER SECOND ON ANDROID EDT / MAIN THREAD]
    
    // Grid Lines Drawing
    FOR x = 0 TO gridWidth:
        // [HEAP ALLOC: 2 x androidx.compose.ui.geometry.Offset per line = 48 bytes]
        drawLine(start = Offset(x * cellW, 0), end = Offset(x * cellW, boardHeight))

    // Tower Render Loop (T_towers times per frame, up to 14)
    FOR EACH tower IN state.towers:
        tcx = (tower.gridPos.x + 0.5f) * cellW
        tcy = (tower.gridPos.y + 0.5f) * cellH

        // [HEAP ALLOC: java.util.Arrays$ArrayList from listOf(Offset, Offset, ...)]
        // [HEAP ALLOC: 4 x Offset objects = 96 bytes per tower per frame]
        ALLOCATE rivetOffsets = listOf(Offset(tcx - rOff, tcy - rOff), ...)
        FOR EACH it IN rivetOffsets:
            drawCircle(center = it)

        IF upgradeLevel == 4: // Level 4 Apex Skin
            IF effectiveClass == TowerClass.ELEMENTAL_VOID:
                // [CRITICAL HEAP ALLOC: androidx.compose.ui.graphics.Path() instance]
                // [CRITICAL HEAP ALLOC: Android android.graphics.Path native bridge]
                ALLOCATE hTri = Path()
                hTri.moveTo(...)
                hTri.lineTo(...)
                hTri.close()
                drawPath(path = hTri, ...)
            
            ELSE IF effectiveClass == TowerClass.STELLAR_COSMIC:
                // [CRITICAL HEAP ALLOC: androidx.compose.ui.graphics.Path() instance per frame]
                ALLOCATE sPath = Path()
                FOR i = 0 TO 9: sPath.lineTo(...)
                drawPath(path = sPath, ...)

    // Aura Connective Lines (When Tier 3 Tower is Selected)
    IF state.selectedTower != null AND state.selectedTower.isTier3:
        FOR EACH other IN state.towers:
            IF other.id != selectedTower.id:
                // [HEAP ALLOC: Multiple Offset objects per connected tower per frame]
                drawLine(start = Offset(emitterX, emitterY), end = Offset(receiverX, receiverY))
                drawCircle(center = Offset(nodeX, nodeY))
```

---

## STAGE 2: THE RUTHLESS CS CODE CRITIQUE (BY MODULE)

### 1. Algorithmic Bottlenecks & Spatial Traps
* **$O(P \cdot E)$ Projectile Destination Tracking:**
  - In `RogueTDViewModel.kt:1133-1143`, for every active bullet, `survivingEnemies.firstOrNull` is executed twice per frame.
  - At 120 bullets and 150 enemies: $2 \times 120 \times 150 = \mathbf{36,000 \text{ operations/frame}} \implies \mathbf{2.16 \text{M ops/sec}}$ on unindexed lists.
* **$O(M \cdot E)$ Spatial Hash Bypass on Area Damage:**
  - In `RogueTDViewModel.kt:1152-1175` & `line 1222`, mortar primary explosions, cluster bomblets, and Cryo shatterbursts completely bypass `spatialGrid`. They run brute-force linear `.filter { ... }` loops over `survivingEnemies`.
* **Redundant Static Buffer Recalculations Every 16ms:**
  - In `RogueTDViewModel.kt:872-915` & `lines 1560-1610`, final form defs, boosted tiles filtering, harmonic count ($O(T \cdot B)$), and `computeActiveSynergies` run every 16ms even though map tiles and tower branches never change mid-wave.
* **Strict `Math.hypot()` vs. Squared Distances:**
  - `Math.hypot()` in lines 936, 946, 1052, 1083, 1152, 1162, 1172, 1221 invokes `Math.sqrt()` on every candidate. Comparing squared distances (`dx*dx + dy*dy <= r*r`) eliminates all square-root overhead.

### 2. GC Pressure & Memory Allocation Churn
* **Calculated Allocation Rate Inside the Simulation Tick:**
  - Per-frame allocation tally across `RogueTDViewModel.kt` and `GameCanvas.kt`:
    1. `currentEnemies.toMutableList()`: 1 ArrayList = ~440 B
    2. `updatedParticles`: 1 ArrayList + 40-80 copies = ~1,600 B
    3. `survivingEnemies`: 1 ArrayList = ~440 B
    4. `spatialGrid.queryNearby().filter()`: 28 ArrayLists = ~3,200 B
    5. Firing allocations (UUID string + Projectile instances): ~1,800 B
    6. `survivingProjectiles`: 1 ArrayList = ~440 B
    7. `computeActiveSynergies`: 1 Map + 6 Pairs + 2 Lists + Auras = ~780 B
    8. `_uiState.update { prev.copy(...) }`: 42-field copy = ~380 B
    9. `GameCanvas` DrawScope `Path()` and `listOf(Offset)`: ~2,700 B
  - **Total per tick:** $\approx \mathbf{12.3 \text{ KB / frame}}$ (~105 allocations/frame).
  - **At 60 FPS:** $12.3 \text{ KB} \times 60 \approx \mathbf{738 \text{ KB / second}} \approx \mathbf{44.28 \text{ MB / minute}}$.
  - **At 120 FPS:** $12.3 \text{ KB} \times 120 \approx \mathbf{1,476 \text{ KB / second}} \approx \mathbf{88.56 \text{ MB / minute}}$.
* **`StateFlow.update { state.copy(...) }` Recomposition Storms:**
  - `_uiState` holds 42 fields. Mutating `animTimeMs` every 16ms forces `RogueTDScreen.kt:62` to re-invoke root layout, causing continuous Compose slot-table equality evaluations across `MinimalTopHud` and `MinimalBottomDock`.

### 3. Architecture & God-File Violations (The Sterling Accountability Check)
* **Rule Applied:** Any file exceeding 750 lines with mixed responsibilities is classified as **HIGH Severity**.
  1. [`RogueTDViewModel.kt`](file:///C:/Users/tanne/AndroidStudioProjects/Fox-Rogue-TD/app/src/main/java/com/example/ui/RogueTDViewModel.kt): **2,495 lines** (332% over 750-line cap). Entangles 6 distinct concerns: Simulation physics, audio triggers, wave scripting, in-app YouTube cinematics, UI state machine, and Firestore network submission.
  2. [`GameModels.kt`](file:///C:/Users/tanne/AndroidStudioProjects/Fox-Rogue-TD/app/src/main/java/com/example/model/GameModels.kt): **2,158 lines** (287% over 750-line cap). Entangles marketing metadata (`CinematicScenario` thumbnail timestamps), Compose UI colors, combat math methods, and entity states.
  3. [`TowerUpgradeBar.kt`](file:///C:/Users/tanne/AndroidStudioProjects/Fox-Rogue-TD/app/src/main/java/com/example/ui/hud/TowerUpgradeBar.kt): **1,749 lines** (233% over 750-line cap). Mixes squircle button rendering, procedural vector icons, Singularity modal logic, and sell/priority dialogs.
  4. [`GameCanvas.kt`](file:///C:/Users/tanne/AndroidStudioProjects/Fox-Rogue-TD/app/src/main/java/com/example/ui/GameCanvas.kt): **1,582 lines** (210% over 750-line cap). Monolithic 1,582-line canvas rendering all 5 themes, 52 towers, creep silhouettes, and aura connections.
  5. [`HudTopBar.kt`](file:///C:/Users/tanne/AndroidStudioProjects/Fox-Rogue-TD/app/src/main/java/com/example/ui/hud/HudTopBar.kt): **749 lines** (at the 750-line boundary). Mixes landscape rotation transforms, dropdown menus, and combat stats.

### 4. Numerical Stability & Edge-Case Math Fragility
* **Float Overflow to Infinity in Infinite Mode (`WaveGenerator.kt:150-155`):**
  - Past Wave 1,180: $1.08^{1130} > 3.4028 \times 10^{38} \implies \mathbf{Float.POSITIVE\_INFINITY}$.
  - Any damage subtraction becomes $\text{Infinity} - \text{damage} = \mathbf{Infinity}$. Enemies become immortal and the game freezes.
* **Integer Precision Loss Past Wave 230:**
  - $2^{24} = 16,777,216$. At Wave 230, base HP exceeds $22.6 \times 10^6$. The 24-bit significand underflows, causing `hp - 1.0f == hp`. Low-damage turrets become incapable of reducing enemy HP.
* **Negative Speed Walking-Backwards Bug (`RogueTDViewModel.kt:796-798`):**
  - `val globalSlow = if (state.globalFrostSlowBonus > 0f) (1f - state.globalFrostSlowBonus) else 1.0f`.
  - Stacked Cryo final forms and banked vaults legitimately push `globalFrostSlowBonus` to $1.25f$, producing `globalSlow = -0.25f`. `moveDistance` becomes negative, walking enemies backwards out of the map and corrupting path coordinates.

---

## STAGE 3: AGENT PROMPT, PERSONA & EXECUTIVE ALIGNMENT AUDIT

### 1. Executive & Gatekeeper Audit (Sterling, Vincent, Diana)
* **The Root Governance Blind Spot:**
  - Inspecting `sterling/agent.md` reveals that Sterling’s primary operational release gate (Section 5.2) required:
    - *"Rotation and UI layout sanity"*
    - *"Audio synthesis and low-pass filtering"*
    - *"Live cloud leaderboard submission"*
    - *"The 6 in-app 30s scripted cinematics play seamlessly"*
  - **The Missing Gate:** Sterling’s prompt contains **zero requirements for heap allocation profiling, method line limits, or GC pause benchmarks**.
  - Sterling and the Meta-Council treated visual UI rotation and video marketing deliverables as proof of repo health, completely blind to the fact that `RogueTDViewModel.kt` had bloated to 2,495 lines and was dumping 44 MB/minute of garbage into the ART nursery heap.
  - **Rubber-Stamp Loophole:** Sterling’s prompt defined the Executive Meta-Council as *"looking down upon the development ecosystem from an outside plane"*, which created a dangerous culture of executive detachment from raw profiler metrics.

### 2. Loophole & Ambiguity Audit Across Specialists
* **Aspirational Prose vs. Enforceable Invariants:**
  - In `alan/agent.md`: *"You champion primitive arrays, contiguous memory layouts, and data-oriented design over object churn."*
    - *Why it failed:* It did not provide a concrete rejection rule forbidding `UUID.randomUUID()` or `.toMutableList()` inside `tickSimulation`.
  - In `leo/agent.md`: *"Your sole obsession is 60/120 FPS frame pacing, zero-allocation simulation ticks..."*
    - *Why it failed:* Leo was concurrently assigned to build the 6 scripted feature cinematics with simulated human gestures. Leo prioritized visual demonstration over allocation pooling.
  - In `frank/agent.md`: Step 3 required Quinn to achieve a *"100% pass rate on `./gradlew testDebugUnitTest`"*.
    - *Why it failed:* Unit tests run headless on the JVM. Unit tests **do not measure heap churn or DrawScope allocations**. Passing 94/94 unit tests created a false sense of security while ART heap thrashing raged undetected.

### 3. Inter-Agent Goal Conflict & Siloing (The YouTube Sidetrack)
* **Marketing Feature Creep Over Engine Performance:**
  - Frank’s and Mike’s release checklists (`frank/agent.md`, `mike/agent.md`) mandated:
    - 6 zero-cut DaVinci Resolve Studio timelines in `Youtube Team Assets/Timelines/` (FCP7 XML + EDL)
    - 6 Packaging Trifectas with thumbnail timestamps and 500-char tags
    - Scripted in-app touch ripple animations
  - Engineering context tokens were spent implementing fake human tap gestures inside `RogueTDViewModel.kt` instead of refactoring un-indexed $O(P \cdot E)$ loops or implementing projectile object pools.

### 4. Context Bloat & Roleplay Dilution
* **Prompt Fluff vs. Hard CS Rules:**
  - Executive prompts contained paragraphs about *"academic pedigree"*, *"looking down from an outside plane"*, and *"dopamine loops"*, while completely lacking basic software craftsmanship constraints such as:
    - *Rule:* No source file may exceed 750 lines.
    - *Rule:* Zero object instantiations inside `tickSimulation` or `onDraw`.
    - *Rule:* All spatial queries must use squared Euclidean distances.

### 5. Project Goal Alignment Check
* **North Star Alignment Failure:**
  The Creator’s North Star is *a rock-solid, zero-lag, 60/120 FPS native Android tower defense game with deep buildcraft and zero paywalls*. Prioritizing YouTube timeline formats and thumbnail tags over hot-loop memory profiling is a direct violation of this North Star.

---

## STAGE 4: THE MASTER TRIAGE MATRIX

### Table A: Code Architecture & Algorithmic Flaws (From Stage 2)

| ID | Module & Method | File & Line Range | Flaw Category | Current Complexity / Cost | Target Complexity / Cost | Severity |
|---|---|---|---|---|---|---|
| **C-01** | `RogueTDViewModel#tickSimulation` | `RogueTDViewModel.kt:740-1300` | Heap Allocation Churn | 12.3 KB / frame (~738 KB/sec at 60 FPS) | **0 bytes / frame** (Object pooling & scratch arrays) | **CRITICAL** |
| **C-02** | `GameCanvas#onDraw` | `GameCanvas.kt:447, 464, 868` | Native Graphic Churn | `Path()` & `listOf(Offset)` allocated inside DrawScope | **0 allocations** (Cached Path singletons & primitive floats) | **CRITICAL** |
| **C-03** | `WaveGenerator#generateWave` | `WaveGenerator.kt:150-155` | Float Overflow & Immortality | $1.08^{\text{wave}-50}$ overflows to `Infinity` at W1180+; $2^{24}$ precision loss at W230 | **Logarithmic Soft-Cap** at $1.0 \times 10^{12}$ | **CRITICAL** |
| **C-04** | `RogueTDViewModel#tickSimulation` | `RogueTDViewModel.kt:1133-1143` | Algorithmic Bottleneck | $O(P \cdot E)$ brute-force target scan (36,000 ops/frame) | **$O(1)$ amortized lookup** via pre-indexed scratch map | **CRITICAL** |
| **C-05** | `RogueTDViewModel` (Class Structure) | `RogueTDViewModel.kt:1-2495` | God File (2,495 lines) | 2,495 lines entangling Physics, Audio, Cinematics, Network | **< 750 lines per class** (Modular domain decomposition) | **HIGH** |
| **C-06** | `GameModels.kt` (Class Structure) | `GameModels.kt:1-2158` | God File (2,158 lines) | 2,158 lines mixing Marketing metadata, UI colors, Balance math | **< 750 lines per file** (Separate balance, entities, marketing) | **HIGH** |
| **C-07** | `TowerUpgradeBar.kt` | `TowerUpgradeBar.kt:1-1749` | God File (1,749 lines) & Recomposition | 1,749 lines mixing 52 squircles, vector icons, dialogs | **< 750 lines per file** (Extract reusable squircle components) | **HIGH** |
| **C-08** | `GameCanvas.kt` | `GameCanvas.kt:1-1582` | God File (1,582 lines) | 1,582 lines monolithic canvas drawing | **< 750 lines per file** (Modular skin & layer painters) | **HIGH** |
| **C-09** | `HudTopBar.kt` | `HudTopBar.kt:1-749` | Boundary God File (749 lines) | 749 lines mixing landscape rotation, menus, combat stats | **< 400 lines** (Extract dropdown dialogs) | **HIGH** |
| **C-10** | `RogueTDViewModel#tickSimulation` | `RogueTDViewModel.kt:1152-1175` | Spatial Grid Bypass | Mortar / Tesla / Cluster explosions bypass `SpatialHashGrid` | **$O(1)$ Spatial Hash Grid query** with squared distances | **HIGH** |
| **C-11** | `RogueTDViewModel#tickSimulation` | `RogueTDViewModel.kt:796-798` | Inverted Movement Bug | `globalFrostSlowBonus` > 1.0f produces negative speed | **Clamp:** `maxOf(0.15f, 1f - globalFrostSlowBonus)` | **HIGH** |
| **C-12** | `RogueTDViewModel#computeActiveSynergies` | `RogueTDViewModel.kt:1560-1610` | Redundant Recalculation | Recomputes Pair keys, maps, and filters every 16ms | **Dirty-Flag Caching** (Recalculate only on tower mutation) | **HIGH** |
| **C-13** | `RogueTDScreen` Root State | `RogueTDScreen.kt:62` | Recomposition Bleed | 60 FPS `animTimeMs` updates trigger root Compose passes | Decouple animation clock from structural GameUIState | **MEDIUM** |

---

### Table B: Agent Prompt, Leadership & Alignment Gaps (From Stage 3)

| ID | Target Agent / Rule File | Weak Rule / Authority Loophole | Resulting Defect in Codebase | Priority |
|---|---|---|---|---|
| **P-01** | `sterling/agent.md` | Executive sign-off audits visual rotation and YouTube cinematics only | C-01, C-02, C-05 passed to release because leadership lacked a memory profiler gate | **CRITICAL** |
| **P-02** | `frank/agent.md` | QA gate requires only headless unit test pass rate | Quinn passed 94/94 unit tests while 44 MB/min heap allocation churn went undetected | **CRITICAL** |
| **P-03** | `AGENTS.md` (Root Rules) | No hard source code file-size cap or modularity invariant | Allowed 4 files to balloon past 1,500–2,500 lines without triggering build rejection | **CRITICAL** |
| **P-04** | `alan/agent.md` | Alan has advisory status with zero PR veto authority | Alan's zero-allocation mandates were ignored by Leo and Sam during feature sprints | **HIGH** |
| **P-05** | `leo/agent.md` | Leo tasked with marketing cinematics alongside physics tick loop | Prioritized scripted demonstration gestures over projectile memory pooling | **HIGH** |
| **P-06** | `mike/agent.md` | Release checklist requires DaVinci XML timelines before push, but no APK heap benchmark | Repository prioritized YouTube video packaging over native engine performance | **MEDIUM** |

---

## STAGE 5: ACTIONABLE HANDOFF SUITE (MANDATORY FIXES & PATCHES)

### Part 5A: Copy-Pasteable Engineering Task Specifications

> ### Engineering Task [ET-01]: Zero-Allocation Simulation Loop & Entity Pooling
> * **Target:** `RogueTDViewModel.kt` (Lines 740–1300) — `tickSimulation()`
> * **Assigned To:** Leo (Lead Engine) & Dr. Alan (Senior Architect)
> * **The Root Problem:** `tickSimulation` allocates new `ArrayList` instances (`currentEnemies`, `survivingEnemies`, `survivingProjectiles`, `inRangeEnemies`), generates random `UUID` strings per projectile, and calls `.copy()` on particles and state 60 times per second, generating ~44 MB/minute of heap garbage.
> * **Target Refactor Pattern:**
>   1. Pre-allocate fixed-capacity scratch lists (`scratchEnemies`, `scratchProjectiles`) inside `RogueTDViewModel` instead of instantiating `ArrayList` inside the loop.
>   2. Replace `UUID.randomUUID().toString()` with an incrementing primitive counter: `private var nextEntityId: Long = 0L`.
>   3. Pre-allocate an object pool for `Projectile` (capacity 256) with `acquire()` and `release()` methods.
>   4. Mutate track particles in-place (`p.progress += p.speed * deltaSec`) rather than calling immutable data class `.copy()`.
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Precondition:* Active wave combat in progress with constant entity count.
>   - *Postcondition:* Zero visual or gameplay balance regression across all 52 towers.
>   - *Memory Constraint:* Exactly **0 bytes of heap allocation** during steady-state ticks.
>   - *Complexity Bound:* $O(T + P + E)$ linear time per frame.
> * **Verification Protocol:**
>   Run Android Studio Memory Profiler on a 5-minute Wave 50 stress test. Assert that Allocation Churn remains at 0 KB/s outside of wave transition events.

---

> ### Engineering Task [ET-02]: Compose DrawScope Path Pre-Allocation & Vector Caching
> * **Target:** `GameCanvas.kt` (Lines 440–470, 820–915) — `DrawScope.onDraw()`
> * **Assigned To:** Maya (VFX) & Sam (UI Specialist)
> * **The Root Problem:** Lines 447, 464, 829, 908 instantiate `Path()` inside Compose `DrawScope` on every frame, and line 868 allocates `listOf(Offset(...))` per tower, triggering native C++ Skia path allocation at 60/120 FPS.
> * **Target Refactor Pattern:**
>   1. Pre-allocate reusable `Path` objects in a remembered cache: `val cachedTriPath = remember { Path() }` and `val cachedStarPath = remember { Path() }`.
>   2. In `DrawScope`, call `cachedPath.reset()` and transform using `scale` and `translate` matrices rather than allocating `new Path()`.
>   3. Unroll `listOf(Offset(...)).forEach { ... }` into explicit primitive `drawCircle()` calls with pre-computed primitive float offsets.
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Precondition:* Render pass active on Android main thread.
>   - *Postcondition:* Pixel-perfect parity with existing Level 1–4 Vector Skins.
>   - *Memory Constraint:* Exactly **0 Path or Collection instantiations** inside `DrawScope`.
> * **Verification Protocol:**
>   Inspect GPU rendering profile (`dumpsys gfxinfo`) on device. Verify frame rendering time remains under 8.3ms (120 FPS capable).

---

> ### Engineering Task [ET-03]: Asymptotic Ceilings & Logarithmic Dampeners for Infinite Scaling
> * **Target:** `WaveGenerator.kt` (Lines 148–158) — `generateWave()`
> * **Assigned To:** Alex (Balance Architect) & Dr. Alan (Senior Architect)
> * **The Root Problem:** Compounding exponential $1.08^{\text{wave}-50}$ overflows 32-bit `Float.MAX_VALUE` past Wave 1,180, producing `Float.POSITIVE_INFINITY` and immortal enemies, while mantissa precision is lost past Wave 230.
> * **Target Refactor Pattern:**
>   Clamp `baseHpScale` using an asymptotic logarithmic dampener past Wave 100:
>   ```kotlin
>   val post50 = (waveNumber - 50).toDouble()
>   val rawExp = Math.pow(1.08, minOf(post50, 150.0)).toFloat()
>   val lateWaveMultiplier = if (waveNumber > 200) (1.0f + (waveNumber - 200) * 0.25f) else 1.0f
>   baseHpScale = minOf(1.0e12f, w50Base * rawExp * lateWaveMultiplier)
>   ```
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Precondition:* Any integer wave from 1 to 10,000.
>   - *Postcondition:* $0.0f < \text{baseHpScale} \le 1.0 \times 10^{12} < \text{Float.MAX\_VALUE}$.
>   - *Math Constraint:* Zero `NaN` or `POSITIVE_INFINITY` outputs under any parameter combination.
> * **Verification Protocol:**
>   Execute parameterized unit test verifying that `WaveGenerator.generateWave(w, difficulty = GameDifficulty.INFINITE)` produces finite, non-NaN HP values for $w \in \{1, 50, 100, 250, 500, 1000, 5000\}$.

---

> ### Engineering Task [ET-04]: $O(1)$ Projectile Pre-Indexing & Spatial Splash Resolution
> * **Target:** `RogueTDViewModel.kt` (Lines 1133–1175, 1222) — `tickSimulation()`
> * **Assigned To:** Leo (Lead Engine) & Dr. Alan (Senior Architect)
> * **The Root Problem:** Lines 1133–1143 run $O(P \cdot E)$ linear scans over `survivingEnemies` twice per projectile to find target coordinates, and splash explosions bypass `spatialGrid` to run linear distance checks.
> * **Target Refactor Pattern:**
>   1. Populate a fast scratch array or map `enemyCoordMap` once per tick mapping `enemy.id -> (worldX, worldY)`.
>   2. Read `destX = enemyCoordMap[proj.targetEnemyId]?.x ?: proj.targetX` in $O(1)$ time.
>   3. Route all mortar splash, cluster bomblet, and Cryo shatterburst radius queries through `spatialGrid.queryNearby(x, y, radius)`.
>   4. Replace all `Math.hypot(dx, dy) <= r` checks with squared distance comparisons: `(dx * dx + dy * dy) <= (r * r)`.
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Precondition:* Bullet impact or explosion event during active tick.
>   - *Postcondition:* Identical splash damage radius and target acquisition behavior.
>   - *Complexity Bound:* Worst-case $O(1)$ amortized lookup per projectile; zero linear scans over `survivingEnemies`.
> * **Verification Protocol:**
>   Run benchmark asserting that 100 active projectiles resolving against 150 enemies takes $< 0.4\text{ms}$ of CPU time per frame.

---

> ### Engineering Task [ET-05]: Modular Deconstruction of `RogueTDViewModel.kt` (2,495 Lines)
> * **Target:** `RogueTDViewModel.kt` (Lines 1–2495)
> * **Assigned To:** Dr. Alan (Senior Architect) & Frank (Lead Architect)
> * **The Root Problem:** `RogueTDViewModel.kt` is a 2,495-line God Object entangling physics, audio, marketing cinematics, UI state, and network calls, violating the 750-line engineering cap.
> * **Target Refactor Pattern:**
>   Extract distinct domain classes:
>   1. `SimulationEngine.kt` (~600 lines): Pure, headless deterministic simulation loop (`tickSimulation`, particle physics, projectile updates).
>   2. `CinematicController.kt` (~350 lines): 6 scripted 30s marketing scenarios and simulated human gesture logic.
>   3. `CombatAudioBridge.kt` (~250 lines): SoundManager coordination and weapon slot dispatch.
>   4. Keep `RogueTDViewModel.kt` strictly as a lightweight StateFlow presentation coordinator (< 500 lines).
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Precondition:* Full test suite passing prior to extraction.
>   - *Postcondition:* Zero regression in save state, game loop, or UI interaction.
>   - *Size Bound:* **No extracted file may exceed 650 lines.**
> * **Verification Protocol:**
>   Execute `powershell -Command "(Get-Content RogueTDViewModel.kt).Length"` confirming line count $\le 500$ and run full test suite with 100% pass rate.

---

> ### Engineering Task [ET-06]: Modular Separation of `GameModels.kt` (2,158 Lines)
> * **Target:** `GameModels.kt` (Lines 1–2158)
> * **Assigned To:** Alex (Balance Architect) & Dr. Alan (Senior Architect)
> * **The Root Problem:** `GameModels.kt` is 2,158 lines mixing YouTube metadata (`CinematicScenario`), Compose UI graphics colors, balance formulas, and entity data classes.
> * **Target Refactor Pattern:**
>   Carve into dedicated domain packages:
>   1. `com.example.model.entities`: `TowerInstance.kt`, `EnemyInstance.kt`, `Projectile.kt`.
>   2. `com.example.engine.balance`: Extract `getEffectiveCombatRange`, `getEffectiveDamage`, `getEffectiveDps` into `TowerBalanceCalculator.kt`.
>   3. `com.example.model.marketing`: `CinematicScenario.kt` (isolate YouTube thumbnail timestamps from game logic).
>   4. Purge all `androidx.compose.ui.graphics.Color` imports from core domain models (use hex `Long` primitives).
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Architecture Invariant:* Domain entities have zero dependencies on Compose UI or marketing packages.
>   - *Size Bound:* **No single file may exceed 500 lines.**
> * **Verification Protocol:**
>   Verify clean compilation and check that `app/src/main/java/com/example/model/` contains modular files all under 500 lines.

---

> ### Engineering Task [ET-07]: Reactive Synergy Invalidation & Dirty-Flag Caching
> * **Target:** `RogueTDViewModel.kt` (Lines 1560–1610) — `computeActiveSynergies()`
> * **Assigned To:** Leo (Lead Engine) & Dr. Alan (Senior Architect)
> * **The Root Problem:** `computeActiveSynergies()` builds `Pair` keys, maps, and lists 60 times a second even though tower configurations never mutate during wave combat.
> * **Target Refactor Pattern:**
>   1. Introduce `private var isSynergyCacheDirty: Boolean = true`.
>   2. Cache computed synergies in `private var cachedSynergies: List<GlobalAuraSynergy> = emptyList()`.
>   3. Set `isSynergyCacheDirty = true` strictly in `buildTower()`, `sellSelectedTower()`, `upgradeTower*()`, and `confirmSingularityCollapse()`.
>   4. In `tickSimulation()`, read `cachedSynergies` directly without running map/filter passes.
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Execution Bound:* $O(1)$ read cost inside the 60 FPS tick loop.
>   - *Memory Constraint:* Exactly 0 allocations per tick for synergy calculations.
> * **Verification Protocol:**
>   Unit test verifying that executing 1,000 simulation ticks invokes `computeActiveSynergies` exactly 0 times when no towers are modified.

---

> ### Engineering Task [ET-08]: Inverted Speed & Cryo Slow Clamp Fix
> * **Target:** `RogueTDViewModel.kt` (Lines 796–798) — `tickSimulation()`
> * **Assigned To:** Alex (Balance Architect) & Leo (Lead Engine)
> * **The Root Problem:** `val globalSlow = if (state.globalFrostSlowBonus > 0f) (1f - state.globalFrostSlowBonus) else 1.0f` becomes negative when stacked frost bonuses exceed 1.0f, causing enemies to walk backwards out of the map.
> * **Target Refactor Pattern:**
>   Enforce a hard floor on the combined slow factor:
>   ```kotlin
>   val globalSlow = maxOf(0.15f, 1.0f - state.globalFrostSlowBonus)
>   val combinedSpeedMultiplier = maxOf(0.15f, enemy.effectiveSpeed * state.balanceConfig.enemySpeedMultiplier * globalSlow)
>   val moveDistance = combinedSpeedMultiplier * deltaSec
>   ```
> * **Strict Invariants (NON-NEGOTIABLE BY STERLING/VINCENT):**
>   - *Invariant:* $0.15f \le \text{combinedSpeedMultiplier} \le \text{enemy.speed} \times 2.5f$.
>   - *Direction Invariant:* `moveDistance` is strictly $\ge 0.0f$ under all game configurations.
> * **Verification Protocol:**
>   Unit test simulating `globalFrostSlowBonus = 2.5f` asserting that enemy movement distance remains positive and clamped to 15% speed.

---

### Part 5B: Agent Prompt & Executive Governance Patches

> ### Prompt Upgrade [PU-01]: Strip Executive Immunity & Mandate Profiler Gate
> * **Target:** `.agents/agents/sterling/agent.md`
> * **Deficiency:** Sterling’s operational release gate verified only visual rotation, audio, and YouTube cinematics, rubber-stamping releases without checking memory allocation churn or file-size caps.
> * **REMOVE (Current Section 5.2 in `sterling/agent.md`):**
>   `"2. Executive Operational Once-Over Gate: Before Mike commits and pushes any release to GitHub, the Executive Meta-Council personally audits the running binary on device/emulator to verify: Rotation and UI layout sanity... audio synthesis... live cloud leaderboard... The 6 in-app 30s scripted cinematics..."`
> * **INSERT (Hardened Systems Performance Executive Gate):**
>   ```markdown
>   2. Executive Operational & Systems Integrity Gate:
>      Before Mike is cleared to commit and push any release to GitHub, the Executive Meta-Council personally audits the running binary against strict Computer Science standards:
>      - Zero-Allocation Hot Paths: Inspect Android Studio Profiler (Memory). Allocation churn during active combat must remain under 5 KB/second (strictly zero object allocations per frame).
>      - Architectural File Cap: Reject any release candidate containing a source file exceeding 750 lines.
>      - Display & Rotation Parity: Counter-rotating HUD, squircle button layout, and touch-target integrity across 0° and 90° landscape.
>      - Numerical Sanity: Verify WaveGenerator exponential scaling produces finite, non-NaN numbers at Wave 100+.
>      - Audio & Cloud Uplink: SoundPool voice allocation limits and verified Firestore leaderboard round-trip.
>      No cinematic, marketing, or YouTube milestone can bypass or override a failed Systems Integrity check.
>   ```

---

> ### Prompt Upgrade [PU-02]: Mandate Static AST Allocation Gate in Squad Orchestration
> * **Target:** `.agents/agents/frank/agent.md`
> * **Deficiency:** Frank’s release pipeline verified only unit test pass rates and YouTube assets, blind to memory allocation leaks in `DrawScope` and hot loops.
> * **REMOVE (Current Step 3 in `frank/agent.md`):**
>   `"Step 3 (Automated QA Gate): Require Quinn to execute ./gradlew testDebugUnitTest and achieve 100% pass rate across domain test files."`
> * **INSERT (Two-Stage QA & Static AST Allocation Gate):**
>   ```markdown
>   Step 3 (Automated QA & Allocation Profiler Gate):
>     - 3A (Functional Tests): Require Quinn to execute `./gradlew testDebugUnitTest` with 100% pass rate across domain test suites.
>     - 3B (Zero-Allocation Hot-Loop Audit): Require Dr. Alan to run static inspection across `tickSimulation`, `GameCanvas.kt`, and collision sweeps. Any PR introducing `UUID.randomUUID()`, `Path()` allocation in `DrawScope`, `.toMutableList()`, or iterator allocations inside per-frame methods must be REJECTED immediately, regardless of YouTube asset readiness.
>     - 3C (750-Line Hard Cap Enforcement): Automatically reject any PR that pushes any file beyond 750 lines.
>   ```

---

> ### Prompt Upgrade [PU-03]: Empower Dr. Alan with Absolute PR Veto Authority
> * **Target:** `.agents/agents/alan/agent.md`
> * **Deficiency:** Alan was relegated to an advisory role authoring documentation headers while Leo and Sam introduced 44 MB/min allocations.
> * **REMOVE (Section in `alan/agent.md`):**
>   `"4. Architectural Documentation Headers: Periodically author and maintain standardized architectural headers in core files... 5. Upgrade Advisory Pass: Participate in Frank's release advisory cycles with concrete proposals..."`
> * **INSERT (Unilateral Veto Authority & Architectural Governance):**
>   ```markdown
>   4. Absolute Hot-Loop Veto Gatekeeper:
>      You hold unilateral veto authority over all code entering `RogueTDViewModel.kt`, `GameCanvas.kt`, and `SpatialHashGrid.kt`. Neither Sterling, Vincent, nor Frank may override your veto.
>      - VETO CRITERIA: If any change introduces temporary collections (`filter`, `map`, `toMutableList`), string concatenation, `UUID` generation, or primitive auto-boxing inside `tickSimulation` or `DrawScope`, you MUST issue a formal VETO NOTICE and fail the build.
>      - HARD FILE-SIZE CAP: You are the guardian of the 750-line rule. Any file exceeding 750 lines must be split immediately before any feature code is merged.
>   ```

---

> ### Prompt Upgrade [PU-04]: Realign Leo's Engine Mandate & Ban Marketing Scripts in Physics Loop
> * **Target:** `.agents/agents/leo/agent.md`
> * **Deficiency:** Leo was tasked with marketing cinematics, resulting in fake human gesture simulations and functional mappings bloating the simulation tick.
> * **REMOVE (Section 2 in `leo/agent.md`):**
>   `"2. In-App 30s Scripted Cinematic Replay Engine (Feature Showcase Mode): Engineer a deterministic, scripted cinematic replay controller in RogueTDViewModel and engine loop... Automated Demonstration Gestures... Pure Wow Factor..."`
> * **INSERT (Strict Engine Separation & Zero-Allocation Invariant):**
>   ```markdown
>   2. Pure Simulation Physics & Zero-Allocation Discipline:
>      - Your sole domain is the deterministic simulation loop, collision math, and entity movement.
>      - STRICT BAN: Never place marketing scripts, fake gesture generators, or cinematic controllers inside `tickSimulation`. All cinematic replay logic must reside in an isolated `CinematicController` outside the core physics loop.
>      - ZERO-ALLOCATION MANDATE: Every loop inside `tickSimulation` must use pre-allocated scratch arrays or object pools. Any use of `.toMutableList()`, `UUID.randomUUID()`, or `.filter { ... }` in hot loops is strictly prohibited.
>   ```

---

> ### Prompt Upgrade [PU-05]: Rebalance Mike's Release Gate: Remove YouTube Blocker, Add CS Gate
> * **Target:** `.agents/agents/mike/agent.md`
> * **Deficiency:** Mike’s checklist blocked releases if DaVinci XML timelines were missing, but had zero checks for APK memory allocation health.
> * **REMOVE (Checklist section in `mike/agent.md`):**
>   `"- [ ] YouTube Team Deliverables Gate (MANDATORY BEFORE STAGING): - [ ] 6 in-app 30s scripted cinematics operational in RogueTDViewModel / Dev Menu. - [ ] 6 zero-cut DaVinci Resolve Studio timelines present in Youtube Team Assets/Timelines/..."`
> * **INSERT (Systems Health Gate Over Marketing Assets):**
>   ```markdown
>   - [ ] Systems Health & Performance Gate (MANDATORY BEFORE STAGING):
>         - [ ] Zero source files exceeding 750 lines in `app/src/main/java/`.
>         - [ ] Android Studio Profiler allocation rate verified < 5 KB/sec during active combat.
>         - [ ] 100% unit test pass rate across domain test files.
>         - [ ] WaveGenerator verified for finite float scaling past Wave 100.
>   - [ ] Marketing & Cinematic Sync (Secondary Non-Blocking Check):
>         - [ ] Cinematic feature showcase operational in Dev Menu (decoupled from core loop).
>   ```

---

> ### Prompt Upgrade [PU-06]: Enact Root 750-Line Modularity Law in `AGENTS.md`
> * **Target:** Root `AGENTS.md` / `GEMINI.md`
> * **Deficiency:** No repository-wide rule preventing the accumulation of massive monolithic God Files.
> * **INSERT (New Section in Root `AGENTS.md`):**
>   ```markdown
>   ## THE 750-LINE ARCHITECTURAL MODULARITY LAW (NON-NEGOTIABLE)
>   1. Hard File-Size Limit: No single source code file (`.kt`, `.java`) in this repository may exceed 750 total lines.
>   2. Mixed Responsibility Prohibition: Any file exceeding 500 lines that combines more than one domain concern (e.g., Physics + Audio, Rendering + Business Logic, Marketing + Data Entities) must be refactored into distinct, single-responsibility modules immediately.
>   3. Automated PR Rejection: Any commit that increases a file's length beyond 750 lines is automatically rejected by the Lead Architect (Dr. Alan) and Squad Lead (Frank), regardless of feature urgency or executive directive.
>   ```

---

## STAGE 6: POST-AUDIT ROLLOUT VERIFICATION (v0.0.1.016 FORENSIC REVIEW)

Following the initial Red-Team Forensic Audit, an autonomous rollout was executed for **Dual Front TD v0.0.1.016**. This stage provides an unvarnished verification audit of what was successfully applied versus what was left unaddressed or compromised.

### 1. Verification of Applied Audit Fixes (VERIFIED SUCCESSFUL)
- **ET-01 (Primitive ID Sequence Generator):** Verified in `RogueTDViewModel.kt`. Replaced high-frequency `UUID.randomUUID().toString()` allocations in projectile generation, laser beam creation, and enemy spawning with a zero-allocation atomic sequence generator (`nextEntityId++`).
- **ET-02 (Spatial Collision Pre-Indexing & Squared Distance Math):** Verified in `RogueTDViewModel.kt`. Pre-indexed surviving enemies into an `enemyMap` in projectile updates, replacing $O(P \cdot E)$ double scans with $O(1)$ lookups. Replaced Euclidean `Math.hypot` calls in splash radius checks with squared distance comparisons (`(dx * dx + dy * dy) <= r * r`).
- **ET-03 (Zero Graphic Allocations in Compose DrawScope):** Verified in `GameCanvas.kt`. Replaced all per-frame `Path()` heap allocations in `Canvas` across Tower Skins (1 through 5), Level 4 Apex Halos, Tier Form Badges, Enemy Vector Geometry, and Tutorial Arrow chevrons with static remembered singletons (`scratchPath1`, `scratchPath2`, `lanePathA`, `lanePathB`, `mergedPath`) utilizing `.reset()`.
- **ET-04 (Asymptotic Infinite Wave Clamp):** Verified in `WaveGenerator.kt`. Clamped post-wave-50 exponential scaling with `minOf(post50, 300.0)` and `minOf(1.0e12f, rawHp)`, permanently eliminating `Float.POSITIVE_INFINITY` and `NaN` crashes while preserving the difficulty curve for waves 1–150+.
- **ET-05 (Cinematic Script Decoupling - Partial):** Verified in `CinematicController.kt`. Successfully extracted 551 lines of marketing cinematics, automated human gestures, and demo scenario scripts from `RogueTDViewModel.kt`.
- **ET-07 (Reactive Dirty-Flag Synergy Caching):** Verified in `RogueTDViewModel.kt`. Integrated `isSynergyCacheDirty` and `cachedSynergies`, recalculating global synergy sets only upon structural board changes (building, upgrading, selling, or singularity collapse) rather than every 16ms frame.
- **ET-08 (Inverted Speed & Cryo Slow Floor Clamp):** Verified in `RogueTDViewModel.kt`. Clamped enemy slow factor calculation with `maxOf(0.15f, 1.0f - state.globalFrostSlowBonus)`, preventing reverse movement.
- **PU-01 through PU-06 (Governance Law):** Verified in root `AGENTS.md`. Enacted the 750-Line Modularity Law, zero-allocation loop mandates, and two-stage QA gates as binding Executive Studio Law.

---

### 2. Forensic Defect Ledger: Unaddressed & Deferred Technical Debt
Despite the significant progress in v0.0.1.016, the following architectural and performance violations remain **UNADDRESSED** in the production codebase:

| Defect ID | Severity | File / Component | Metric / Violation | Root Cause / Technical Description |
| :--- | :--- | :--- | :--- | :--- |
| **U-01** | **CRITICAL** | `RogueTDViewModel.kt` | **2,012 Lines** (Cap: 750) | Decoupling stopped at `CinematicController.kt`. Core simulation loop (`tickSimulation`, 450+ lines), wave spawning logic, combat audio coordination, and upgrade handling remain combined in the ViewModel. |
| **U-02** | **CRITICAL** | `GameModels.kt` | **2,159 Lines** (Cap: 750) | ET-06 was skipped. Domain entities, balance math formulas, UI color palettes, and marketing models remain entangled in a single 2,159-line file. |
| **U-03** | **HIGH** | `TowerUpgradeBar.kt` | **1,750 Lines** (Cap: 750) | Combines upgrade squircles, tactical warning modals, priority menus, scrap calculations, and build docks. |
| **U-04** | **HIGH** | `GameCanvas.kt` | **1,602 Lines** (Cap: 750) | Combines board grid rendering, path textures, 5 full tower skins, particle effects, projectile rendering, enemy rendering, and combat text. |
| **U-05** | **HIGH** | `RogueTDViewModel.kt#tickSimulation` | **`_uiState.copy()` Churn** | `_uiState.update { it.copy(...) }` still executes every 16ms/8ms frame, creating shallow copies of large immutable lists (`enemies`, `projectiles`, `explosions`, `floatingTexts`, `trackParticles`), causing high GC pressure. |
| **U-06** | **MEDIUM** | `RogueTDViewModel.kt#tickSimulation` | **Per-Tick Array Allocations** | `ArrayList(state.enemies.size)`, `updatedParticles = ArrayList(...)`, and `survivingEnemies = ArrayList()` are instantiated every frame instead of utilizing double-buffered reusable scratch arrays. |

---

## STAGE 7: ENGINEERING ROLLOUT PLAN — GAMEPLAY & BALANCE OVERHAUL (TARGET: v0.0.1.017)

Based on direct Creator playtesting and gameplay telemetry, five critical gameplay, visual, and architectural deficiencies have been identified. The following engineering tasks constitute the definitive implementation plan for **v0.0.1.017**.

---

### Task B-01: Global Fire Rate Rebalance & Level 1–3 Progression Salience
* **Affected Files:** `GameModels.kt` (`TowerType`, `TowerArchetype`), `RogueTDViewModel.kt`
* **Deficiency:** Baseline fire rates are excessively fast (Gatling 2.4–4.4 shots/sec, Laser 4.2 shots/sec, Cryo 3.2 shots/sec). Weapons fire like water hoses immediately upon placement, making Level 1–3 towers feel trivial, visually chaotic, and lacking punchy progression.
* **Refactor Plan:**
  1. Reduce base fire rates by ~40–50% across starter `TowerType` and early Tier 1 vectors:
     - `GATLING`: 2.4 $\to$ 1.2 attacks/sec.
     - `CRYO`: 3.2 $\to$ 1.6 attacks/sec.
     - `TESLA`: 1.4 $\to$ 0.7 attacks/sec.
     - `MORTAR`: 0.70 $\to$ 0.4 attacks/sec.
     - `LASER`: 4.2 $\to$ 1.8 attacks/sec.
     - `STELLAR`: 1.8 $\to$ 0.9 attacks/sec.
     - `VOID`: 1.1 $\to$ 0.6 attacks/sec.
  2. Proportionally scale base damage per shot upwards (+80% to +110%) to preserve baseline DPS while giving each shot heavy, rhythmic recoil and distinct audible impact.
  3. Re-tune Tier 2 and Tier 3 upgrade speed multipliers (e.g. +35% to +60% cyclic rate per tier), ensuring that upgrading a tower produces a dramatic, visceral surge in rate of fire.

---

### Task B-02: Projectile Travel Time Integration & Ballistic Speed Tuning
* **Affected Files:** `RogueTDViewModel.kt` (lines ~1000–1200)
* **Deficiency:** Projectile speed is set to an excessive `16.0f` on a 10-unit grid, causing bullets to traverse their entire firing range in ~0.15s (9 frames), creating the visual illusion of instant hitscan lasers rather than physical munitions.
* **Refactor Plan:**
  1. Reduce ballistic projectile velocities to physical flight speeds:
     - `GATLING` kinetic bullets: `16.0f` $\to$ `8.5f`.
     - `MORTAR` lobbed shells: `6.2f` $\to$ `4.2f`.
     - `STELLAR` cosmic bolts: `9.0f` $\to$ `6.0f`.
  2. Incorporate projectile travel time into turret firing pacing so that towers rhythmically time their salvos rather than hose-spraying targets already doomed by in-flight munitions.
  3. Ensure mortar arc height and shadow tracking scale dynamically with physical flight duration.

---

### Task B-03: Tier 3 Apex Squircle Cost Synchronization
* **Affected Files:** `TowerUpgradeBar.kt` (line 573)
* **Deficiency:** In `TowerUpgradeBar.kt` (line 573), the upgrade cost for Tier 3 Apex squircles is hardcoded as `val cost = 280`. However, `RogueTDViewModel.upgradeTowerToTier3Global()` computes dynamic cost via `TowerUpgradeManager.getTier3GlobalCost(tower, state.towers)` (which scales based on archetype base cost and active Tier 3 count). This causes a UI desynchronization bug where the squircle displays `280c` while the treasury deducts the true calculated cost, causing false unaffordable lockouts or unexpected deductions.
* **Refactor Plan:**
  1. Replace `val cost = 280` in `TowerUpgradeBar.kt` with:
     ```kotlin
     val cost = TowerUpgradeManager.getTier3GlobalCost(tower, state.towers)
     ```
  2. Ensure both the landscape and portrait upgrade bars display the dynamic, real-time cost matching ViewModel deductions.

---

### Task B-04: Map Grid Expansion (+2 Rows) & Strategic Lane Lengthening
* **Affected Files:** `MapGenerator.kt`, `GameCanvas.kt`
* **Deficiency:** The current 10x18 grid leaves unused vertical space in widescreen landscape, and short enemy paths prevent players from accumulating sufficient scrap to fill the board on Hard mode.
* **Refactor Plan:**
  1. Expand grid dimensions in `MapGenerator.kt`:
     ```kotlin
     const val DEFAULT_WIDTH = 10
     const val DEFAULT_HEIGHT = 20 // Added 2 rows (18 -> 20)
     ```
     This expands total map tiles from 180 to 200, creating 14–18 buildable pads.
  2. Increase `minLaneLength` and `maxLaneLength` in `MapGenerator.generate()` from `(12, 26)` to `(16, 32)`, incorporating wider sweeping corner legs touching outer edges before converging at the nexus spine.
  3. Verify that `GameCanvas.kt` board aspect ratio and cell sizing automatically adapt to 10x20 without UI distortion or clipping.

---

### Task B-05: Player-Chosen Synergy Conduit & Permanent Teaser Squircle
* **Affected Files:** `TowerUpgradeBar.kt`, `RogueTDViewModel.kt`, `GameModels.kt`
* **Deficiency:** The 4th squircle on maxed Tier 3 towers currently looks like a clickable button (`Box(...)`) but is non-interactive. Furthermore, the Singularity Capstone was previously locked to an arbitrarily chosen oldest tower, stripping the player of tactical placement agency.
* **Refactor Plan:**
  1. **Permanent Teaser / Progress Squircle:** On any maxed (Tier 3) tower, transform the 4th squircle slot into an active **Synergy Conduit / Singularity dock**:
     - *When Board Is Incomplete (< 100% full/maxed):* Display as a tactical teaser:
       - Header: `"CONDUIT"`
       - Subtitle: `"PROGRESS"`
       - Badge: `"${state.towers.size}/${buildableTiles.size} Full"`
       - Tapping it triggers an informational HUD toast / floating text: `"Grid Incomplete! Fill all tiles to Tier 2+ to forge Synergy Conduit here!"`
     - *When Board Is Complete (100% full with Tier 2/3 towers):* The Conduit squircle illuminates with an animated gold pulse and alert border:
       - Header: `"★ FORGE ★"`
       - Subtitle: `"CONDUIT"`
       - Action: Tapping this squircle initiates the Singularity Collapse *into this specific tower*!
  2. **Tactical Tower Selection Agency:** The player chooses *which* tower becomes the monument. The selected tower:
     - Retains its Tier 3 Archetype form and weapon capabilities.
     - Becomes crowned as `isCapstone = true` (Synergy Conduit).
     - Increments generation: `generation += 1`.
     - Sucks in and absorbs all active global synergies into its `SynergyVault`.
     - Grants the 20% scrap refund bounty from all sacrificed towers.
  3. **Strictly ONE Synergy Conduit Rule:** If an existing Synergy Conduit from a previous generation is already on the board and the player collapses the grid onto a new tower, the old conduit (including all its historic banked vaults) is absorbed into the new conduit. Exactly **ONE** Synergy Conduit exists on the board at any given time.

---

## CONCLUSION

Dual Front TD v0.0.1.016 established vital baseline CS invariants (zero Path allocations, asymptotic math ceilings, dirty-flag synergy caching). 

Executing **Stage 7 (Tasks B-01 through B-05)** in v0.0.1.017 resolves all core balance complaints, restores tactical player agency over the Singularity board-merge loop, expands the tactical grid, and fixes squircle cost desynchronization.

*Full dossier updated and permanently recorded in `RED_TEAM_AUDIT_REPORT.md`.*
