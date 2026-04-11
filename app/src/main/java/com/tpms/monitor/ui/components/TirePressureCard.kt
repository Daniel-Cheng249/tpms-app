package com.tpms.monitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpms.monitor.data.PressureStatus
import com.tpms.monitor.data.SignalStatus
import com.tpms.monitor.data.TirePosition
import com.tpms.monitor.data.TirePressureData
import com.tpms.monitor.ui.theme.*

/**
 * 单个轮胎压力卡片
 */
@Composable
fun TirePressureCard(
    data: TirePressureData,
    modifier: Modifier = Modifier
) {
    val hasData = data.pressure > 0f

    val borderColor = when {
        !hasData -> Color(0xFF3D3D5C)  // 无数据时深灰色边框
        data.pressure < 1.8f -> Color(0xFFEF5350)  // 低压红色
        data.pressure > 3.5f -> Color(0xFFFFB74D)  // 高压橙色
        else -> Color(0xFF66BB6A)  // 正常绿色
    }

    val pressureColor = when {
        !hasData -> Color(0xFFB0B0B0)  // 无数据时灰色
        data.pressure < 1.8f -> Color(0xFFEF5350)  // 低压红色
        data.pressure > 3.5f -> Color(0xFFFFB74D)  // 高压橙色
        else -> Color.White  // 正常白色
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E3F))
    ) {
        Box(
            modifier = Modifier
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 轮胎位置名称
                Text(
                    text = data.position.displayName,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "(${data.position.tireName})",
                    fontSize = 12.sp,
                    color = Color(0xFF888899),
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 轮毂图标
                WheelIcon(
                    modifier = Modifier.size(64.dp),
                    hasData = hasData
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 胎压值
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format("%.1f", data.pressure),
                        fontSize = 48.sp,
                        color = pressureColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "bar",
                        fontSize = 14.sp,
                        color = Color(0xFF888899),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 温度和电量
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 温度
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🌡️",
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasData && data.temperature > 0) "${data.temperature.toInt()}°C" else "--°C",
                            fontSize = 13.sp,
                            color = Color(0xFFBBBBCC),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 电量
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔋",
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasData && data.batteryLevel > 0) "${data.batteryLevel}%" else "--%",
                            fontSize = 13.sp,
                            color = Color(0xFFBBBBCC),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // RSSI 信号强度
                SignalStrengthIndicator(
                    rssi = data.rssi,
                    hasData = hasData
                )
            }
        }
    }
}

/**
 * 信号强度指示器
 */
@Composable
private fun SignalStrengthIndicator(
    rssi: Int,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when {
        !hasData || rssi == 0 -> Triple("📡", Color(0xFF666688), "无信号")
        rssi > -50 -> Triple("📶", Color(0xFF66BB6A), "${rssi} dBm")  // 强信号 - 绿色
        rssi > -70 -> Triple("📶", Color(0xFFFFB74D), "${rssi} dBm")  // 中等 - 黄色
        else -> Triple("📶", Color(0xFFEF5350), "${rssi} dBm")        // 弱信号 - 红色
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 14.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 轮毂图标
 */
@Composable
private fun WheelIcon(
    modifier: Modifier = Modifier,
    hasData: Boolean = false
) {
    val wheelColor = if (hasData) Color(0xFFE8A576) else Color(0xFF5A6E80)
    val bgColor = Color(0xFF2A2A4A)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.minDimension / 2f

            // 绘制轮毂辐条（8 根）
            for (i in 0 until 8) {
                val angle = (i * 45).toFloat()
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val innerRadius = radius * 0.25f
                val outerRadius = radius * 0.85f

                val startX = centerX + innerRadius * kotlin.math.cos(rad)
                val startY = centerY + innerRadius * kotlin.math.sin(rad)
                val endX = centerX + outerRadius * kotlin.math.cos(rad)
                val endY = centerY + outerRadius * kotlin.math.sin(rad)

                drawLine(
                    color = wheelColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 中心圆
            drawCircle(
                color = wheelColor,
                radius = radius * 0.2f,
                center = Offset(centerX, centerY)
            )

            // 外圈
            drawCircle(
                color = wheelColor.copy(alpha = 0.5f),
                radius = radius * 0.85f,
                center = Offset(centerX, centerY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A3E)
@Composable
fun TirePressureCardPreview() {
    TirePressureCard(
        data = TirePressureData(
            position = TirePosition.FRONT_LEFT,
            pressure = 2.5f,
            temperature = 25f,
            batteryLevel = 85,
            rssi = -45
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A3E)
@Composable
fun TirePressureCardLowPreview() {
    TirePressureCard(
        data = TirePressureData(
            position = TirePosition.REAR_RIGHT,
            pressure = 1.6f,
            temperature = 28f,
            batteryLevel = 45,
            rssi = -65
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A3E)
@Composable
fun TirePressureCardWeakSignalPreview() {
    TirePressureCard(
        data = TirePressureData(
            position = TirePosition.REAR_LEFT,
            pressure = 2.3f,
            temperature = 30f,
            batteryLevel = 30,
            rssi = -78
        )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A3E)
@Composable
fun TirePressureCardNoDataPreview() {
    TirePressureCard(
        data = TirePressureData(
            position = TirePosition.FRONT_RIGHT,
            pressure = 0f,
            temperature = 0f,
            batteryLevel = 0,
            rssi = 0
        )
    )
}
