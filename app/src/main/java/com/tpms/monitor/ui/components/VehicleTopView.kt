package com.tpms.monitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tpms.monitor.ui.theme.TPMSMonitorTheme

/**
 * 科幻白色车辆俯视图组件 - 升级版
 * 更精致的细节，立体效果，科技感十足
 */
@Composable
fun VehicleTopView(
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF00E5FF),
    bodyColor: Color = Color(0xFFFFFFFF),
    showGlow: Boolean = true
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 绘制发光效果
            if (showGlow) {
                drawGlowEffect(glowColor)
            }

            // 绘制车辆主体
            drawVehicleBody(bodyColor, glowColor)

            // 绘制车辆细节
            drawVehicleDetails(glowColor)
        }
    }
}

private fun DrawScope.drawGlowEffect(glowColor: Color) {
    val centerX = size.width / 2
    val centerY = size.height / 2

    // 多层外发光效果
    val glowBrush1 = Brush.radialGradient(
        colors = listOf(
            glowColor.copy(alpha = 0.2f),
            glowColor.copy(alpha = 0.05f),
            Color.Transparent
        ),
        center = Offset(centerX, centerY),
        radius = size.width * 0.6f
    )

    drawCircle(
        brush = glowBrush1,
        radius = size.width * 0.55f,
        center = Offset(centerX, centerY)
    )
}

private fun DrawScope.drawVehicleBody(bodyColor: Color, accentColor: Color) {
    val width = size.width
    val height = size.height
    val centerX = width / 2

    // 车身主体渐变 - 更细腻的白色金属质感
    val bodyBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFFFF),  // 纯白
            Color(0xFFF8F8F8),  // 微灰白
            Color(0xFFFFFFFF),  // 纯白
            Color(0xFFF0F0F0),  // 浅灰
            Color(0xFFFAFAFA),  // 近白
            Color(0xFFFFFFFF)   // 纯白
        ),
        start = Offset(0f, 0f),
        end = Offset(width, height)
    )

    // 精细的车身轮廓
    val vehiclePath = Path().apply {
        // 几乎填满整个空间
        val margin = width * 0.015f
        val topY = margin
        val bottomY = height - margin

        // 流线型车头
        moveTo(centerX - width * 0.25f, topY)
        quadraticBezierTo(
            centerX, topY - width * 0.03f,
            centerX + width * 0.25f, topY
        )

        // 引擎盖曲线
        quadraticBezierTo(
            centerX + width * 0.42f, topY + height * 0.08f,
            centerX + width * 0.48f, topY + height * 0.18f
        )

        // 前翼子板到前门
        lineTo(centerX + width * 0.49f, topY + height * 0.30f)
        lineTo(centerX + width * 0.48f, topY + height * 0.48f)

        // 后门到后翼子板
        lineTo(centerX + width * 0.47f, topY + height * 0.68f)
        quadraticBezierTo(
            centerX + width * 0.45f, topY + height * 0.82f,
            centerX + width * 0.35f, bottomY
        )

        // 车尾
        quadraticBezierTo(
            centerX, bottomY + width * 0.02f,
            centerX - width * 0.35f, bottomY
        )

        // 左侧车身（镜像）
        quadraticBezierTo(
            centerX - width * 0.45f, topY + height * 0.82f,
            centerX - width * 0.47f, topY + height * 0.68f
        )
        lineTo(centerX - width * 0.48f, topY + height * 0.48f)
        lineTo(centerX - width * 0.49f, topY + height * 0.30f)
        lineTo(centerX - width * 0.48f, topY + height * 0.18f)
        quadraticBezierTo(
            centerX - width * 0.42f, topY + height * 0.08f,
            centerX - width * 0.25f, topY
        )

        close()
    }

    // 绘制车身阴影层
    drawPath(
        path = vehiclePath,
        color = Color(0xFF000000).copy(alpha = 0.15f),
        style = Stroke(width = 6f)
    )

    // 绘制车身填充
    drawPath(
        path = vehiclePath,
        brush = bodyBrush
    )

    // 绘制外发光边框
    drawPath(
        path = vehiclePath,
        color = accentColor.copy(alpha = 0.85f),
        style = Stroke(width = 3f)
    )

    // 绘制内高光边框
    drawPath(
        path = vehiclePath,
        color = Color(0xFFFFFFFF).copy(alpha = 0.6f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawVehicleDetails(accentColor: Color) {
    val width = size.width
    val height = size.height
    val centerX = width / 2

    // 前挡风玻璃
    drawWindshield(centerX, width, height, accentColor)

    // 后挡风玻璃
    drawRearWindow(centerX, width, height, accentColor)

    // 车顶天窗
    drawSunroof(centerX, width, height, accentColor)

    // 前大灯
    drawHeadlights(centerX, width, height, accentColor)

    // 后尾灯
    drawTaillights(centerX, width, height, accentColor)

    // 轮胎位置标记
    drawTireMarkers(centerX, width, height, accentColor)

    // 车身线条和细节
    drawBodyLines(centerX, width, height, accentColor)

    // 中央装饰线
    drawCenterLine(centerX, width, height, accentColor)
}

private fun DrawScope.drawWindshield(centerX: Float, width: Float, height: Float, accentColor: Color) {
    val windshieldPath = Path().apply {
        val topY = height * 0.18f
        val bottomY = height * 0.32f
        val topWidth = width * 0.38f
        val bottomWidth = width * 0.58f

        moveTo(centerX - topWidth / 2, topY)
        lineTo(centerX + topWidth / 2, topY)
        quadraticBezierTo(
            centerX + bottomWidth / 2, (topY + bottomY) / 2,
            centerX + bottomWidth / 2, bottomY
        )
        lineTo(centerX - bottomWidth / 2, bottomY)
        quadraticBezierTo(
            centerX - bottomWidth / 2, (topY + bottomY) / 2,
            centerX - topWidth / 2, topY
        )
        close()
    }

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D1B2A).copy(alpha = 0.95f),
            Color(0xFF1B263B).copy(alpha = 0.85f),
            Color(0xFF0D1B2A).copy(alpha = 0.9f)
        ),
        start = Offset(centerX, height * 0.18f),
        end = Offset(centerX, height * 0.32f)
    )

    drawPath(path = windshieldPath, brush = glassBrush)
    drawPath(
        path = windshieldPath,
        color = accentColor.copy(alpha = 0.7f),
        style = Stroke(width = 2f)
    )

    // 玻璃反光效果
    val reflectionPath = Path().apply {
        moveTo(centerX - width * 0.15f, height * 0.20f)
        lineTo(centerX + width * 0.10f, height * 0.20f)
        lineTo(centerX + width * 0.05f, height * 0.28f)
        lineTo(centerX - width * 0.20f, height * 0.28f)
        close()
    }
    drawPath(
        path = reflectionPath,
        color = Color(0xFFFFFFFF).copy(alpha = 0.15f)
    )
}

private fun DrawScope.drawRearWindow(centerX: Float, width: Float, height: Float, accentColor: Color) {
    val rearWindowPath = Path().apply {
        val topY = height * 0.70f
        val bottomY = height * 0.86f
        val topWidth = width * 0.55f
        val bottomWidth = width * 0.35f

        moveTo(centerX - topWidth / 2, topY)
        lineTo(centerX + topWidth / 2, topY)
        quadraticBezierTo(
            centerX + bottomWidth / 2, (topY + bottomY) / 2,
            centerX + bottomWidth / 2, bottomY
        )
        lineTo(centerX - bottomWidth / 2, bottomY)
        quadraticBezierTo(
            centerX - bottomWidth / 2, (topY + bottomY) / 2,
            centerX - topWidth / 2, topY
        )
        close()
    }

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1B263B).copy(alpha = 0.85f),
            Color(0xFF0D1B2A).copy(alpha = 0.95f)
        ),
        start = Offset(centerX, height * 0.70f),
        end = Offset(centerX, height * 0.86f)
    )

    drawPath(path = rearWindowPath, brush = glassBrush)
    drawPath(
        path = rearWindowPath,
        color = accentColor.copy(alpha = 0.6f),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawSunroof(centerX: Float, width: Float, height: Float, accentColor: Color) {
    val sunroofPath = Path().apply {
        val topY = height * 0.36f
        val bottomY = height * 0.66f
        val roofWidth = width * 0.32f

        moveTo(centerX - roofWidth / 2, topY)
        lineTo(centerX + roofWidth / 2, topY)
        lineTo(centerX + roofWidth / 2, bottomY)
        lineTo(centerX - roofWidth / 2, bottomY)
        close()
    }

    drawPath(
        path = sunroofPath,
        color = Color(0xFF2C3E50).copy(alpha = 0.75f)
    )

    // 天窗边框
    drawPath(
        path = sunroofPath,
        color = accentColor.copy(alpha = 0.5f),
        style = Stroke(width = 2f)
    )

    // 天窗内部线条
    drawLine(
        color = accentColor.copy(alpha = 0.3f),
        start = Offset(centerX - width * 0.12f, height * 0.42f),
        end = Offset(centerX + width * 0.12f, height * 0.42f),
        strokeWidth = 1f
    )
    drawLine(
        color = accentColor.copy(alpha = 0.3f),
        start = Offset(centerX - width * 0.12f, height * 0.58f),
        end = Offset(centerX + width * 0.12f, height * 0.58f),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawHeadlights(centerX: Float, width: Float, height: Float, accentColor: Color) {
    // 左前大灯 - 更有立体感
    val leftHeadlightPath = Path().apply {
        moveTo(centerX - width * 0.32f, height * 0.10f)
        lineTo(centerX - width * 0.18f, height * 0.06f)
        quadraticBezierTo(
            centerX - width * 0.12f, height * 0.08f,
            centerX - width * 0.15f, height * 0.14f
        )
        lineTo(centerX - width * 0.28f, height * 0.16f)
        close()
    }

    val headlightBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF9C4).copy(alpha = 0.95f),
            Color(0xFFFFEB3B).copy(alpha = 0.85f)
        ),
        start = Offset(centerX - width * 0.30f, height * 0.08f),
        end = Offset(centerX - width * 0.18f, height * 0.14f)
    )

    drawPath(path = leftHeadlightPath, brush = headlightBrush)
    drawPath(
        path = leftHeadlightPath,
        color = accentColor.copy(alpha = 0.9f),
        style = Stroke(width = 1.5f)
    )

    // 右前大灯
    val rightHeadlightPath = Path().apply {
        moveTo(centerX + width * 0.32f, height * 0.10f)
        lineTo(centerX + width * 0.18f, height * 0.06f)
        quadraticBezierTo(
            centerX + width * 0.12f, height * 0.08f,
            centerX + width * 0.15f, height * 0.14f
        )
        lineTo(centerX + width * 0.28f, height * 0.16f)
        close()
    }

    drawPath(path = rightHeadlightPath, brush = headlightBrush)
    drawPath(
        path = rightHeadlightPath,
        color = accentColor.copy(alpha = 0.9f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawTaillights(centerX: Float, width: Float, height: Float, accentColor: Color) {
    // 左后尾灯 - LED效果
    val leftTaillightPath = Path().apply {
        moveTo(centerX - width * 0.38f, height * 0.80f)
        lineTo(centerX - width * 0.22f, height * 0.80f)
        quadraticBezierTo(
            centerX - width * 0.20f, height * 0.84f,
            centerX - width * 0.22f, height * 0.88f
        )
        lineTo(centerX - width * 0.35f, height * 0.88f)
        close()
    }

    val taillightBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF5252).copy(alpha = 0.9f),
            Color(0xFFD32F2F).copy(alpha = 0.85f)
        ),
        start = Offset(centerX - width * 0.35f, height * 0.80f),
        end = Offset(centerX - width * 0.25f, height * 0.88f)
    )

    drawPath(path = leftTaillightPath, brush = taillightBrush)
    drawPath(
        path = leftTaillightPath,
        color = accentColor.copy(alpha = 0.7f),
        style = Stroke(width = 1.5f)
    )

    // 右后尾灯
    val rightTaillightPath = Path().apply {
        moveTo(centerX + width * 0.38f, height * 0.80f)
        lineTo(centerX + width * 0.22f, height * 0.80f)
        quadraticBezierTo(
            centerX + width * 0.20f, height * 0.84f,
            centerX + width * 0.22f, height * 0.88f
        )
        lineTo(centerX + width * 0.35f, height * 0.88f)
        close()
    }

    drawPath(path = rightTaillightPath, brush = taillightBrush)
    drawPath(
        path = rightTaillightPath,
        color = accentColor.copy(alpha = 0.7f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawTireMarkers(centerX: Float, width: Float, height: Float, accentColor: Color) {
    val tirePositions = listOf(
        Pair(centerX - width * 0.40f, height * 0.28f),  // 左前
        Pair(centerX + width * 0.40f, height * 0.28f),  // 右前
        Pair(centerX - width * 0.40f, height * 0.72f),  // 左后
        Pair(centerX + width * 0.40f, height * 0.72f)   // 右后
    )

    tirePositions.forEach { (x, y) ->
        // 外圈光环
        drawCircle(
            color = accentColor.copy(alpha = 0.4f),
            radius = width * 0.065f,
            center = Offset(x, y),
            style = Stroke(width = 2f)
        )

        // 中间圆环
        drawCircle(
            color = accentColor.copy(alpha = 0.7f),
            radius = width * 0.050f,
            center = Offset(x, y),
            style = Stroke(width = 2.5f)
        )

        // 内部填充
        drawCircle(
            color = Color(0xFFE0E0E0).copy(alpha = 0.8f),
            radius = width * 0.035f,
            center = Offset(x, y)
        )

        // 中心点
        drawCircle(
            color = accentColor.copy(alpha = 0.95f),
            radius = width * 0.018f,
            center = Offset(x, y)
        )

        // 十字标记
        drawLine(
            color = Color(0xFF666666).copy(alpha = 0.5f),
            start = Offset(x - width * 0.025f, y),
            end = Offset(x + width * 0.025f, y),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF666666).copy(alpha = 0.5f),
            start = Offset(x, y - width * 0.025f),
            end = Offset(x, y + width * 0.025f),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawBodyLines(centerX: Float, width: Float, height: Float, accentColor: Color) {
    // 引擎盖线条
    drawLine(
        color = Color(0xFFCCCCCC).copy(alpha = 0.5f),
        start = Offset(centerX - width * 0.20f, height * 0.08f),
        end = Offset(centerX - width * 0.15f, height * 0.16f),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFFCCCCCC).copy(alpha = 0.5f),
        start = Offset(centerX + width * 0.20f, height * 0.08f),
        end = Offset(centerX + width * 0.15f, height * 0.16f),
        strokeWidth = 1f
    )

    // 后备箱线条
    drawLine(
        color = Color(0xFFCCCCCC).copy(alpha = 0.5f),
        start = Offset(centerX - width * 0.25f, height * 0.84f),
        end = Offset(centerX - width * 0.20f, height * 0.90f),
        strokeWidth = 1f
    )
    drawLine(
        color = Color(0xFFCCCCCC).copy(alpha = 0.5f),
        start = Offset(centerX + width * 0.25f, height * 0.84f),
        end = Offset(centerX + width * 0.20f, height * 0.90f),
        strokeWidth = 1f
    )

    // 门分割线
    drawLine(
        color = Color(0xFFAAAAAA).copy(alpha = 0.4f),
        start = Offset(centerX - width * 0.48f, height * 0.50f),
        end = Offset(centerX + width * 0.48f, height * 0.50f),
        strokeWidth = 1f
    )

    // 门把手
    listOf(
        Pair(centerX - width * 0.30f, height * 0.52f),
        Pair(centerX + width * 0.30f, height * 0.52f),
        Pair(centerX - width * 0.32f, height * 0.62f),
        Pair(centerX + width * 0.32f, height * 0.62f)
    ).forEach { (x, y) ->
        drawCircle(
            color = Color(0xFF888888).copy(alpha = 0.7f),
            radius = width * 0.010f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawCenterLine(centerX: Float, width: Float, height: Float, accentColor: Color) {
    // 中央脊柱线
    drawLine(
        color = accentColor.copy(alpha = 0.4f),
        start = Offset(centerX, height * 0.15f),
        end = Offset(centerX, height * 0.85f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
    )

    // 中央发光点
    drawCircle(
        color = accentColor.copy(alpha = 0.6f),
        radius = width * 0.025f,
        center = Offset(centerX, height * 0.50f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = width * 0.012f,
        center = Offset(centerX, height * 0.50f)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun VehicleTopViewPreview() {
    TPMSMonitorTheme {
        VehicleTopView(
            modifier = Modifier.size(300.dp)
        )
    }
}
