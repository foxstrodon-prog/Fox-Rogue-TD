package com.example.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EnemyType
import com.example.model.EnemyUnitBalance
import com.example.model.TowerBalanceDescriptor
import com.example.model.TowerUnitBalance
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BalanceSliderCard(
    title: String,
    subtitle: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueFormatter: (Float) -> String,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var textInput by remember(value) { mutableStateOf(String.format(Locale.US, "%.2f", value)) }

    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = valueFormatter(value),
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { showCustomInput = !showCustomInput }
                        .padding(start = 6.dp)
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            if (showCustomInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BasicTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 10.sp),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                            .border(1.dp, color, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    Button(
                        onClick = {
                            val parsed = textInput.toFloatOrNull()
                            if (parsed != null) {
                                onValueChange(parsed.coerceIn(range.start, range.endInclusive))
                            }
                            showCustomInput = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("SET", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showCustomInput = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("X", color = Color.White, fontSize = 8.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Collapsible39TowerCard(
    descriptor: TowerBalanceDescriptor,
    balance: TowerUnitBalance,
    onBalanceChanged: (TowerUnitBalance) -> Unit,
    onResetToDefault: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(balance.customName) { mutableStateOf(balance.customName ?: descriptor.displayName) }

    val currentName = balance.customName ?: descriptor.displayName
    val towerColor = Color(descriptor.family.color)
    val levelColor = when (descriptor.level) {
        1 -> Color(0xFF00FF9D)
        2 -> Color(0xFF00F0FF)
        else -> Color(0xFFFFB800)
    }
    val levelLabel = when (descriptor.level) {
        1 -> "L1 BASE"
        2 -> "L2 SPEC"
        else -> "L3 FINAL"
    }

    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) towerColor else Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(towerColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = towerColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = levelColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = levelLabel,
                                    color = levelColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 7.5.sp,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "DMG: ${balance.damage.toInt()} | RNG: ${String.format(Locale.US, "%.1f", balance.range)}u | SPD: ${String.format(Locale.US, "%.1f", balance.fireRate)}/s",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isEditingName) {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == descriptor.displayName) null else trimmed))
                            isEditingName = false
                        } else {
                            nameDraft = currentName
                            isEditingName = true
                            isExpanded = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingName) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "Rename Tower",
                        tint = if (isEditingName) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (isEditingName) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        placeholder = { Text(descriptor.displayName, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = towerColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == descriptor.displayName) null else trimmed))
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = towerColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("SAVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BalanceSliderCard(
                        title = "Base Bullet Damage",
                        subtitle = "Per-hit impact damage value",
                        value = balance.damage,
                        range = 5f..800f,
                        steps = 79,
                        valueFormatter = { "${it.toInt()} DMG" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(damage = it.roundToInt().toFloat())) }
                    )

                    BalanceSliderCard(
                        title = "Targeting Engagement Range",
                        subtitle = "Combat radius in grid units",
                        value = balance.range,
                        range = 1.0f..8.0f,
                        steps = 70,
                        valueFormatter = { String.format(Locale.US, "%.1f u", it) },
                        color = Color(0xFF00F0FF),
                        onValueChange = { onBalanceChanged(balance.copy(range = (it * 10).roundToInt() / 10f)) }
                    )

                    BalanceSliderCard(
                        title = "Firing Cadence / Speed",
                        subtitle = "Shots fired per second",
                        value = balance.fireRate,
                        range = 0.2f..15.0f,
                        steps = 74,
                        valueFormatter = { String.format(Locale.US, "%.1f /s", it) },
                        color = Color(0xFF00FF9D),
                        onValueChange = { onBalanceChanged(balance.copy(fireRate = (it * 10).roundToInt() / 10f)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onResetToDefault,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset ${descriptor.displayName} Defaults", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleEnemyBalanceCard(
    enemyType: EnemyType,
    balance: EnemyUnitBalance,
    onBalanceChanged: (EnemyUnitBalance) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember(balance.customName) { mutableStateOf(balance.customName ?: enemyType.displayName) }

    val currentName = balance.customName ?: enemyType.displayName
    val enemyColor = Color(enemyType.color)

    Surface(
        color = Color(0xFF131F37),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) enemyColor else Color(0xFF1E293B)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(enemyColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = enemyColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${enemyType.name})",
                                color = Color(0xFF64748B),
                                fontSize = 8.5.sp
                            )
                        }
                        Text(
                            text = "HP: ${balance.hp.toInt()} | SPD: ${String.format(Locale.US, "%.2f", balance.speed)} | ARMOR: ${balance.armor.toInt()} | +${balance.scrapValue}C",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (isEditingName) {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == enemyType.displayName) null else trimmed))
                            isEditingName = false
                        } else {
                            nameDraft = currentName
                            isEditingName = true
                            isExpanded = true
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isEditingName) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = "Rename Enemy",
                        tint = if (isEditingName) Color(0xFF00FF9D) else Color(0xFF94A3B8),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (isEditingName) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it },
                        singleLine = true,
                        placeholder = { Text(enemyType.displayName, fontSize = 10.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = enemyColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val trimmed = nameDraft.trim()
                            onBalanceChanged(balance.copy(customName = if (trimmed.isEmpty() || trimmed == enemyType.displayName) null else trimmed))
                            isEditingName = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = enemyColor, contentColor = Color.White),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("SAVE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BalanceSliderCard(
                        title = "Base Health Points",
                        subtitle = "Starting hit points before wave scaling",
                        value = balance.hp,
                        range = 25f..2000f,
                        steps = 78,
                        valueFormatter = { "${it.toInt()} HP" },
                        color = Color(0xFFFF2A55),
                        onValueChange = { onBalanceChanged(balance.copy(hp = it.roundToInt().toFloat())) }
                    )

                    BalanceSliderCard(
                        title = "Traversal Speed",
                        subtitle = "Lane navigation speed in grid cells/sec",
                        value = balance.speed,
                        range = 0.3f..3.0f,
                        steps = 26,
                        valueFormatter = { String.format(Locale.US, "%.2f cells/s", it) },
                        color = Color(0xFF00FF9D),
                        onValueChange = { onBalanceChanged(balance.copy(speed = (it * 100).roundToInt() / 100f)) }
                    )

                    BalanceSliderCard(
                        title = "Damage Reduction Armor",
                        subtitle = "Flat or percentage reduction applied against hits",
                        value = balance.armor,
                        range = 0f..50f,
                        steps = 50,
                        valueFormatter = { "${it.toInt()} Armor" },
                        color = Color(0xFF38BDF8),
                        onValueChange = { onBalanceChanged(balance.copy(armor = it.roundToInt().toFloat())) }
                    )

                    BalanceSliderCard(
                        title = "Scrap Bounty Yield",
                        subtitle = "Credits granted to player bank upon kill",
                        value = balance.scrapValue.toFloat(),
                        range = 2f..60f,
                        steps = 29,
                        valueFormatter = { "+${it.toInt()} Credits" },
                        color = Color(0xFFFFD700),
                        onValueChange = { onBalanceChanged(balance.copy(scrapValue = it.roundToInt())) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onBalanceChanged(
                                    EnemyUnitBalance(
                                        customName = null,
                                        hp = enemyType.baseHp,
                                        speed = enemyType.baseSpeed,
                                        armor = enemyType.baseArmor,
                                        scrapValue = enemyType.scrapValue
                                    )
                                )
                                isEditingName = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Reset ${enemyType.displayName} Defaults", color = Color(0xFF94A3B8), fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
