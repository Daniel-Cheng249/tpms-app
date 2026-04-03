package com.tpms.monitor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpms.monitor.ui.theme.TextSecondary

/**
 * 状态指示器组件
 * 显示扫描和连接状态
 */
@Composable
fun StatusIndicators(
    isScanning: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(
            label = "扫描中",
            isActive = isScanning,
            activeColor = Color(0xFF42A5F5),
            inactiveLabel = "未连接",
            scale = if (isScanning) scale else 1f
        )

        Spacer(modifier = Modifier.width(32.dp))

        StatusDot(
            label = "已连接",
            isActive = isConnected,
            activeColor = Color(0xFF66BB6A),
            inactiveLabel = "未连接",
            scale = 1f
        )
    }
}

@Composable
private fun StatusDot(
    label: String,
    isActive: Boolean,
    inactiveLabel: String,
    activeColor: Color,
    scale: Float = 1f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(scale)
                .background(
                    if (isActive) activeColor else Color.Gray.copy(alpha = 0.5f),
                    CircleShape
                )
        )

        Text(
            text = if (isActive) label else inactiveLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = if (isActive) activeColor else TextSecondary
        )
    }
}
