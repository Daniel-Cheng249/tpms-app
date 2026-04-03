package com.tpms.monitor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFB74D),
    background = BackgroundDark,
    surface = BackgroundCard,
    surfaceVariant = SurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFF57C00),
    background = Color(0xFFFFFBFE),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5)
)

@Composable
fun TPMSMonitorTheme(
    darkTheme: Boolean = true,  // 默认深色主题
    dynamicColor: Boolean = false,  // 暂不启用动态颜色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            activity.window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
