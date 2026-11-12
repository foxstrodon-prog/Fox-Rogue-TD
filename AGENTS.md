# ==============================================================================
# DUAL FRONT TD — EXECUTIVE STUDIO GOVERNANCE & ARCHITECTURAL LAW
# Enacted by: Executive Meta-Council (Sterling & Vincent)
# Mandate: Zero-Lag 60/120 FPS Systems Rigor & Commercial-Grade Repo Health
# Target Release: v0.0.1.016+
# ==============================================================================

## 1. THE 750-LINE ARCHITECTURAL MODULARITY LAW (NON-NEGOTIABLE)

1. **Hard File-Size Limit:** No single source code file (`.kt`, `.java`) in this repository may exceed 750 total lines.
2. **Mixed-Responsibility Prohibition:** Any file exceeding 500 lines that combines more than one domain concern (e.g., Physics + Audio, Rendering + Business Logic, Marketing + Data Entities) must be refactored into distinct, single-responsibility modules immediately.
3. **Automated PR Rejection:** Any commit or PR that increases a file's length beyond 750 lines is automatically rejected by the Lead Architect (Dr. Alan) and Squad Lead (Frank), regardless of feature urgency or milestone targets.

---

## 2. ZERO-ALLOCATION SIMULATION & RENDER HOT-LOOP INVARIANTS

1. **Hot Simulation Loops (`tickSimulation`):**
   - **Zero Heap Allocations:** Steady-state frames during active combat must produce **0 bytes/second** of heap allocation.
   - **Forbidden Constructs in Hot Loops:**
     - `UUID.randomUUID().toString()` (Use incrementing primitive `Long` IDs: `nextEntityId++`).
     - Temporary collection copies: `.toMutableList()`, `ArrayList()`, `mutableListOf()`.
     - Functional lambdas with iterator churn: `.filter`, `.map`, `.flatMap`, `.forEach`, `.any`, `.maxByOrNull`. Use explicit indexed `for (i in 0 until count)` loops over reusable pre-allocated scratch arrays.
     - Ephemeral data class cloning (`.copy()`) inside 16ms/8ms ticks.
   - **Object Pooling:** Projectiles, particles, and combat effects must be managed through pre-allocated, fixed-capacity object pools with `acquire()` and `release()` lifecycle semantics.
2. **Canvas Render Pipeline (`GameCanvas` / `DrawScope`):**
   - **Zero Graphic Allocations in `onDraw`:** Never instantiate `Path()`, `listOf(Offset(...))`, `Paint()`, or `Size()` inside Compose `DrawScope`.
   - **Cached Singletons:** All complex silhouette paths and geometry must be pre-allocated, cached via `remember { Path() }` or static singletons, and transformed via matrices/scales using `.reset()`.
3. **Reactive Dirty-Flag Caching:**
   - Calculations dependent on board topology (synergy aggregations, booster pad harmonic status, global final-form buffs) must **NEVER** run every 16ms tick. They must be cached and recalculated **only** behind dirty flags toggled upon tower placement, upgrade, sale, or Singularity Collapse.

---

## 3. NUMERICAL STABILITY & CONCURRENCY SAFEGUARDS

1. **Asymptotic Scaling Ceilings:**
   - Any exponential or polynomial wave progression formula (e.g. $1.08^{\text{wave}-50}$) must have a hard mathematical ceiling and logarithmic dampener ensuring:
     $$0.0f < \text{baseHpScale} \le 1.0 \times 10^{12} \ll \text{Float.MAX\_VALUE}$$
   - Zero `NaN` or `POSITIVE_INFINITY` outputs are permitted under any integer wave input ($1..10,000$).
2. **Velocity & Movement Clamping:**
   - Combined slow multipliers must be strictly clamped with a positive floor:
     $$\text{combinedSpeedMultiplier} \ge 0.15 \times \text{baseSpeed}$$
   - Movement distance per tick must be strictly non-negative ($\ge 0.0f$) to prevent enemies from walking backwards or corrupting waypoint indices.
3. **Burst State Mutation Concurrency:**
   - High-impact global state transitions (Singularity Collapse, board-wide sales, wave restarts) must be guarded against rapid re-entrance (double-taps) and thread-safe against concurrent iteration in the simulation loop.

---

## 4. TWO-STAGE QA & RELEASE INTEGRITY GATES

1. **Stage 1 — Functional & Domain QA Gate:**
   - Quinn executes `./gradlew testDebugUnitTest`. Must achieve a **100% pass rate** across all domain test suites.
2. **Stage 2 — Static AST & Allocation Profiler Gate:**
   - Dr. Alan inspects `tickSimulation`, `GameCanvas.kt`, and collision sweeps. Any PR introducing `UUID.randomUUID()`, `Path()` in `DrawScope`, `.toMutableList()`, or un-indexed $O(N \cdot M)$ scans is **VETOED** immediately.
   - Android Studio Memory Profiler must confirm steady-state allocation rate $< 5 \text{ KB/sec}$ during active combat.
3. **Release Execution Protocol (Mike):**
   - Staging requires verification of:
     - 0 source files exceeding 750 lines in `app/src/main/java/`.
     - Zero unhandled compiler warnings.
     - Local APK and Windows ADB companion runner compiled.
   - Pushing to GitHub requires explicit sign-off from Frank and Dr. Alan.
