package com.jzb.jichang.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/** Tinted translucent material for navigation and floating controls, never content cards. */
fun Modifier.glassMaterial(shape: Shape, dark: Boolean): Modifier {
    // Keep enough backdrop visible to read as glass while retaining a calm tint.
    val surface = if (dark) Color(0xB51B2536) else Color(0xBDFBFDFF)
    val tint = if (dark) Color(0x3D3A78E8) else Color(0x2B5C91F5)
    val edge = if (dark) Color(0x6659A0FF) else Color(0xA8FFFFFF)
    val shadow = if (dark) Color(0x77050B16) else Color(0x1E24354A)
    return shadow(16.dp, shape, clip = false, ambientColor = shadow, spotColor = shadow)
        .background(
            Brush.verticalGradient(listOf(surface.copy(alpha = 0.75f), tint, surface.copy(alpha = 0.84f))),
            shape,
        )
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = when (outline) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }
            clipPath(path) {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.12f else 0.54f), Color.Transparent), endY = size.height * 0.42f))
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, edge, Color.White.copy(alpha = if (dark) 0.2f else 0.92f), edge, Color.Transparent)),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                drawRoundRect(color = Color.Black.copy(alpha = if (dark) 0.2f else 0.045f), style = Stroke(width = 1.dp.toPx()))
            }
        }
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(edge, tint.copy(alpha = 0.55f), Color.White.copy(alpha = if (dark) 0.17f else 0.88f))),
            shape = shape,
        )
}
