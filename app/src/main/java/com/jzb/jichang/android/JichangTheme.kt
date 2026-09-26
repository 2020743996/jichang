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
    primary = Color(0xFF3979EE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5EEFF),
    onPrimaryContainer = Color(0xFF122E5F),
    secondary = Color(0xFF536A8D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8EDF5),
    onSecondaryContainer = Color(0xFF1C2A40),
    tertiary = Color(0xFF4C9DB7),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF171B23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171B23),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF555F70),
    outline = Color(0xFFB7C2D2),
    outlineVariant = Color(0xFFDCE3EE),
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
    background = Color(0xFF10151F),
    onBackground = Color(0xFFE4E9F2),
    surface = Color(0xFF171E2A),
    onSurface = Color(0xFFE4E9F2),
    surfaceVariant = Color(0xFF242E3D),
    onSurfaceVariant = Color(0xFFBEC9D9),
    outline = Color(0xFF6C7A8F),
    outlineVariant = Color(0xFF3B485A),
    error = Color(0xFFFFB4AB),
)

@Composable
fun JichangTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(15.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp),
        ),
        content = content,
    )
}
