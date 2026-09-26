package com.jzb.jichang.android

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3478F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8F0FF),
    onPrimaryContainer = Color(0xFF142B50),
    secondary = Color(0xFF536174),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EDF5),
    onSecondaryContainer = Color(0xFF1C2A40),
    tertiary = Color(0xFF6B829E),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F6F8),
    onBackground = Color(0xFF17191D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191D),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF60656D),
    outline = Color(0xFFBBC0C8),
    outlineVariant = Color(0xFFE1E3E7),
    error = Color(0xFFBA1A1A),
)

@Composable
fun JichangTheme(glassOpacity: Float = 0.45f, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(22.dp),
        ),
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalGlassTransparency provides glassOpacity,
                androidx.compose.material3.LocalContentColor provides LightColors.onBackground,
                content = content,
            )
        },
    )
}
