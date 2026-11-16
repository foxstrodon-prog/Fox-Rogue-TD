package com.example.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DevBalanceConfig
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DevEconomyTab(
    config: DevBalanceConfig,
    onConfigChanged: (DevBalanceConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "GLOBAL COMBAT & ECONOMY MULTIPLIERS",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        item {
            BalanceSliderCard(
                title = "Starting Credits (Early Economy)",
                subtitle = "Default 220 Credits for tactical round placement",
                value = config.startingScrap.toFloat(),
                range = 100f..600f,
                steps = 49,
                valueFormatter = { "${it.toInt()} C" },
                color = Color(0xFFFFD700),
                onValueChange = { onConfigChanged(config.copy(startingScrap = it.roundToInt())) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Overall Tower Fire Rate Multiplier",
                subtitle = "Accelerates/slows all 52 towers firing speed",
                value = config.globalTowerFireRateMultiplier,
                range = 0.2f..3.0f,
                steps = 27,
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                color = Color(0xFF00FF9D),
                onValueChange = { onConfigChanged(config.copy(globalTowerFireRateMultiplier = (it * 100).roundToInt() / 100f)) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Overall Unit Health Multiplier",
                subtitle = "Scales enemy base HP pool across all waves",
                value = config.enemyHpMultiplier,
                range = 0.5f..3.0f,
                steps = 24,
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                color = Color(0xFFFF2A55),
                onValueChange = { onConfigChanged(config.copy(enemyHpMultiplier = (it * 100).roundToInt() / 100f)) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Overall Enemy Speed Multiplier",
                subtitle = "Scales traversal rate along paths",
                value = config.enemySpeedMultiplier,
                range = 0.5f..2.5f,
                steps = 19,
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                color = Color(0xFF38BDF8),
                onValueChange = { onConfigChanged(config.copy(enemySpeedMultiplier = (it * 100).roundToInt() / 100f)) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Enemies Per Round Multiplier",
                subtitle = "Scale hostile assault volume per wave across both lanes",
                value = config.enemiesPerRoundMultiplier,
                range = 0.5f..3.0f,
                steps = 24,
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                color = Color(0xFFFF6600),
                onValueChange = { onConfigChanged(config.copy(enemiesPerRoundMultiplier = (it * 100).roundToInt() / 100f)) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Overall Income Multiplier",
                subtitle = "Scales enemy bounties and resource drops on-the-fly",
                value = config.globalIncomeMultiplier,
                range = 0.2f..3.0f,
                steps = 27,
                valueFormatter = { String.format(Locale.US, "%.2fx", it) },
                color = Color(0xFFFFB800),
                onValueChange = { onConfigChanged(config.copy(globalIncomeMultiplier = (it * 100).roundToInt() / 100f)) }
            )
        }

        item {
            Text(
                text = "MAP GEOMETRY & LANE COVERAGE",
                color = Color(0xFF00F0FF),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }

        val totalCells = config.mapColumns * config.mapRows

        item {
            BalanceSliderCard(
                title = "Easy Mode Available Tower Spots (Lane Length)",
                subtitle = "Winds lanes across board leaving target buildable tiles",
                value = config.easyTargetOpenSlots.toFloat(),
                range = 6f..40f,
                steps = 33,
                valueFormatter = { "${it.toInt()} Tower Spots (${(totalCells - it.toInt()).coerceAtLeast(0)} Path/Core Tiles)" },
                color = Color(0xFF00FF9D),
                onValueChange = { onConfigChanged(config.copy(easyTargetOpenSlots = it.roundToInt())) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Medium Mode Available Tower Spots (Lane Length)",
                subtitle = "Winds lanes across board leaving target buildable tiles",
                value = config.mediumTargetOpenSlots.toFloat(),
                range = 6f..40f,
                steps = 33,
                valueFormatter = { "${it.toInt()} Tower Spots (${(totalCells - it.toInt()).coerceAtLeast(0)} Path/Core Tiles)" },
                color = Color(0xFFFFB800),
                onValueChange = { onConfigChanged(config.copy(mediumTargetOpenSlots = it.roundToInt())) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Hard Mode Available Tower Spots (Lane Length)",
                subtitle = "Winds lanes across board leaving target buildable tiles",
                value = config.hardTargetOpenSlots.toFloat(),
                range = 6f..40f,
                steps = 33,
                valueFormatter = { "${it.toInt()} Tower Spots (${(totalCells - it.toInt()).coerceAtLeast(0)} Path/Core Tiles)" },
                color = Color(0xFFFF2A55),
                onValueChange = { onConfigChanged(config.copy(hardTargetOpenSlots = it.roundToInt())) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Min Lane Overlap",
                subtitle = "Minimum shared spine crossover tiles",
                value = config.minLaneOverlap.toFloat(),
                range = 2f..20f,
                steps = 17,
                valueFormatter = { "${it.toInt()} Shared Tiles" },
                color = Color(0xFF00F0FF),
                onValueChange = {
                    val newMin = it.roundToInt()
                    onConfigChanged(
                        config.copy(
                            minLaneOverlap = newMin,
                            maxLaneOverlap = maxOf(config.maxLaneOverlap, newMin)
                        )
                    )
                }
            )
        }

        item {
            BalanceSliderCard(
                title = "Max Lane Overlap",
                subtitle = "Maximum shared spine crossover tiles",
                value = config.maxLaneOverlap.toFloat(),
                range = 4f..30f,
                steps = 25,
                valueFormatter = { "${it.toInt()} Shared Tiles" },
                color = Color(0xFFA855F7),
                onValueChange = {
                    val newMax = it.roundToInt()
                    onConfigChanged(
                        config.copy(
                            maxLaneOverlap = newMax,
                            minLaneOverlap = minOf(config.minLaneOverlap, newMax)
                        )
                    )
                }
            )
        }

        item {
            BalanceSliderCard(
                title = "Map Grid Columns (Width)",
                subtitle = "Grid cell columns across board",
                value = config.mapColumns.toFloat(),
                range = 6f..16f,
                steps = 9,
                valueFormatter = { "${it.toInt()} Cols (${it.toInt() * config.mapRows} Tiles)" },
                color = Color(0xFF38BDF8),
                onValueChange = { onConfigChanged(config.copy(mapColumns = it.roundToInt())) }
            )
        }

        item {
            BalanceSliderCard(
                title = "Map Grid Rows (Height)",
                subtitle = "Grid cell rows down board",
                value = config.mapRows.toFloat(),
                range = 10f..28f,
                steps = 17,
                valueFormatter = { "${it.toInt()} Rows (${config.mapColumns * it.toInt()} Tiles)" },
                color = Color(0xFFFF7A00),
                onValueChange = { onConfigChanged(config.copy(mapRows = it.roundToInt())) }
            )
        }
    }
}
