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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TargetPriority

/**
 * Compact Target Priority & Sell Flank Control used in FloatingSquircleUpgradeBar.
 */
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
    val pillShape = RoundedCornerShape(12.dp)
    val darkGlass = Color(0xFF0A101D).copy(alpha = 0.92f)

    Column(
        modifier = modifier.height(76.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (isRotated) 40.dp else 46.dp, 36.dp)
                .graphicsLayer { rotationZ = animatedRotation }
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
                .padding(horizontal = 3.dp)
                .testTag("target_priority_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isRotated) {
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
                        text = priority.label.take(4).uppercase(),
                        color = Color(0xFF00F0FF),
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "Target Priority",
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = priority.label.take(4).uppercase(),
                        color = Color(0xFF00F0FF),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(if (isRotated) 40.dp else 46.dp, 36.dp)
                .graphicsLayer { rotationZ = animatedRotation }
                .clip(pillShape)
                .background(darkGlass)
                .border(1.2.dp, Color(0xFFFF2A55).copy(alpha = 0.72f), pillShape)
                .clickable { onSellTower() }
                .padding(horizontal = 3.dp)
                .testTag("sell_tower_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isRotated) {
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
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Sell Tower",
                        tint = Color(0xFFFF2A55),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "+$sellValue",
                        color = Color(0xFFFFD700),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}
