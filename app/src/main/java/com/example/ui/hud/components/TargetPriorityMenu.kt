package com.example.ui.hud.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TargetPriority

/** Minimum / maximum width of the priority + sell pills so full words (STRONGEST, WEAKEST) never clip. */
internal val PRIORITY_PILL_MIN_WIDTH = 56.dp
internal val PRIORITY_PILL_MAX_WIDTH = 72.dp

/**
 * Compact Target Priority & Sell Flank Control used in FloatingSquircleUpgradeBar.
 * The priority pill stacks a compact icon above a single-line, untruncated uppercase label.
 */
@Composable
fun TargetPriorityPillButton(
    priority: TargetPriority,
    isRotated: Boolean,
    animatedRotation: Float,
    onChangePriority: (TargetPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(12.dp)
    val darkGlass = Color(0xFF0A101D).copy(alpha = 0.92f)
    Box(
        modifier = modifier
            .testTag("target_priority_button")
            .hudCapsuleContainer(isRotated, animatedRotation)
            .widthIn(min = PRIORITY_PILL_MIN_WIDTH, max = PRIORITY_PILL_MAX_WIDTH)
            .height(36.dp)
            .clip(pillShape)
            .background(darkGlass)
            .border(1.2.dp, Color(0xFF00F0FF).copy(alpha = 0.72f), pillShape)
            .clickable {
                val nextPrio = when (priority) {
                    TargetPriority.FIRST -> TargetPriority.CLOSEST
                    TargetPriority.CLOSEST -> TargetPriority.STRONGEST
                    TargetPriority.STRONGEST -> TargetPriority.WEAKEST
                    TargetPriority.WEAKEST -> TargetPriority.FIRST
                }
                onChangePriority(nextPrio)
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Target Priority",
                tint = Color(0xFF00F0FF),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = priority.label.uppercase(),
                color = Color(0xFF00F0FF),
                fontSize = 7.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SellTowerPillButton(
    sellValue: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    onSellTower: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(12.dp)
    val darkGlass = Color(0xFF0A101D).copy(alpha = 0.92f)
    Box(
        modifier = modifier
            .testTag("sell_tower_button")
            .hudCapsuleContainer(isRotated, animatedRotation)
            .widthIn(min = PRIORITY_PILL_MIN_WIDTH, max = PRIORITY_PILL_MAX_WIDTH)
            .height(36.dp)
            .clip(pillShape)
            .background(darkGlass)
            .border(1.2.dp, Color(0xFFFF2A55).copy(alpha = 0.72f), pillShape)
            .clickable { onSellTower() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Sell Tower",
                tint = Color(0xFFFF2A55),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "+$sellValue",
                color = Color(0xFFFFD700),
                fontSize = 7.5.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun TargetPriorityMenu(
    priority: TargetPriority,
    sellValue: Int,
    isRotated: Boolean,
    animatedRotation: Float,
    onChangePriority: (TargetPriority) -> Unit,
    onSellTower: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isRotated) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TargetPriorityPillButton(
                priority = priority,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onChangePriority = onChangePriority
            )
            SellTowerPillButton(
                sellValue = sellValue,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onSellTower = onSellTower
            )
        }
    } else {
        Column(
            modifier = modifier.height(76.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TargetPriorityPillButton(
                priority = priority,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onChangePriority = onChangePriority
            )
            SellTowerPillButton(
                sellValue = sellValue,
                isRotated = isRotated,
                animatedRotation = animatedRotation,
                onSellTower = onSellTower
            )
        }
    }
}
