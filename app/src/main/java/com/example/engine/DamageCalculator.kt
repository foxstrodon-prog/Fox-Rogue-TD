package com.example.engine

import com.example.model.ElementAffinity
import com.example.model.EnemyShape

/**
 * Pure, extensible combat damage calculation engine for Fox-Rogue-TD.
 * Evaluates geometric shape compatibility (Circle, Triangle, Square) and
 * rock-paper-scissors elemental color affinities (Red > Green > Yellow > Red).
 */
object DamageCalculator {

    data class DamageResult(
        val finalDamage: Float,
        val shapeMultiplier: Float,
        val colorMultiplier: Float,
        val isShapeMatch: Boolean,
        val isColorAdvantage: Boolean,
        val isColorDisadvantage: Boolean
    )

    /**
     * Calculates the damage dealt to an enemy taking into account:
     * - Base weapon damage
     * - Shape compatibility (Tower Shape vs Enemy Shape)
     *   - Shape Match: 1.20x bonus
     *   - Shape Mismatch: 0.50x resistance penalty
     * - Elemental color affinity (Red > Green > Yellow > Red)
     *   - Advantage: 1.30x damage (+30%)
     *   - Disadvantage: 0.70x mitigation (-30%)
     *   - Neutral / Same: 1.00x
     * - Enemy armor mitigation
     */
    fun calculateDamage(
        baseDamage: Float,
        towerShape: EnemyShape,
        towerAffinity: ElementAffinity,
        enemyShape: EnemyShape,
        enemyAffinity: ElementAffinity,
        enemyArmor: Float = 0f
    ): DamageResult {
        // 1. Shape matching logic
        val isShapeMatch = (towerShape == enemyShape)
        val shapeMultiplier = if (isShapeMatch) 1.20f else 0.50f

        // 2. Elemental color counter loop (Red > Green > Yellow > Red)
        val isAdvantage = towerAffinity.counters(enemyAffinity)
        val isDisadvantage = enemyAffinity.counters(towerAffinity)
        val colorMultiplier = when {
            isAdvantage -> 1.30f
            isDisadvantage -> 0.70f
            else -> 1.00f
        }

        // 3. Enemy armor reduction
        val armorFactor = (1.0f - enemyArmor.coerceIn(0f, 0.90f))
        val computed = baseDamage * shapeMultiplier * colorMultiplier * armorFactor

        return DamageResult(
            finalDamage = computed.coerceAtLeast(1.0f),
            shapeMultiplier = shapeMultiplier,
            colorMultiplier = colorMultiplier,
            isShapeMatch = isShapeMatch,
            isColorAdvantage = isAdvantage,
            isColorDisadvantage = isDisadvantage
        )
    }
}
