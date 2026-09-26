package com.jzb.jichang.android

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BB4FF),
    onPrimary = Color(0xFF062553),
    primaryContainer = Color(0xFF173A70),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFB7C8E3),
    onSecondary = Color(0xFF233249),
    secondaryContainer = Color(0xFF2A3B55),
    onSecondaryContainer = Color(0xFFDCE7F8),
    tertiary = Color(0xFF8CC9DB),
    onTertiary = Color(0xFF003543),
    background = Color(0xFF101114),
    onBackground = Color(0xFFECEDEF),
    surface = Color(0xFF191A1D),
    onSurface = Color(0xFFECEDEF),
    surfaceVariant = Color(0xFF25272B),
    onSurfaceVariant = Color(0xFFB4B7BD),
    outline = Color(0xFF696C73),
    outlineVariant = Color(0xFF3C3E43),
    error = Color(0xFFFFB4AB),
)

@Composable
fun JichangTheme(darkTheme: Boolean = isSystemInDarkTheme(), glassOpacity: Float = 0.45f, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
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
                androidx.compose.material3.LocalContentColor provides if (darkTheme) DarkColors.onBackground else LightColors.onBackground,
                content = content,
            )
        },
    )
}
