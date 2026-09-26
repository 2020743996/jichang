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
    primary = Color(0xFF256B55),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8EBDD),
    onPrimaryContainer = Color(0xFF103A2B),
    secondary = Color(0xFFB1741B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5E3BE),
    onSecondaryContainer = Color(0xFF3B2708),
    tertiary = Color(0xFF536B88),
    background = Color(0xFFF8F6F0),
    onBackground = Color(0xFF1B211D),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1B211D),
    surfaceVariant = Color(0xFFECEAE1),
    onSurfaceVariant = Color(0xFF464C46),
    outline = Color(0xFF767C75),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D3B7),
    onPrimary = Color(0xFF063827),
    primaryContainer = Color(0xFF1D513D),
    onPrimaryContainer = Color(0xFFD0EBD8),
    secondary = Color(0xFFE5BF79),
    onSecondary = Color(0xFF3E2B0A),
    secondaryContainer = Color(0xFF594116),
    onSecondaryContainer = Color(0xFFF5E2B8),
    tertiary = Color(0xFFB8CBE7),
    background = Color(0xFF111713),
    onBackground = Color(0xFFE1E6DF),
    surface = Color(0xFF171E19),
    onSurface = Color(0xFFE1E6DF),
    surfaceVariant = Color(0xFF29332C),
    onSurfaceVariant = Color(0xFFC0CAC1),
    outline = Color(0xFF89958B),
)

@Composable
fun JichangTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = androidx.compose.material3.Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
