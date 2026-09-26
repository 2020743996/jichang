package com.jzb.jichang.android

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val LocalGlassTransparency = compositionLocalOf { 0.45f }

/** GPU backdrop refraction applied to the app scene, masked to the measured chrome bounds. */
class LiquidGlassScene {
    val shader = RuntimeShader(
        """
        uniform shader backdrop;
        uniform float4 topRect;
        uniform float4 bottomRect;
        uniform float topRadius;
        uniform float bottomRadius;
        uniform float opacity;
        uniform float isDark;

        float roundedRectDistance(float2 p, float4 rect, float radius) {
            float2 center = (rect.xy + rect.zw) * 0.5;
            float2 halfSize = (rect.zw - rect.xy) * 0.5;
            float2 q = abs(p - center) - halfSize + radius;
            return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
        }

        half4 main(float2 p) {
            half4 original = backdrop.eval(p);
            float topD = roundedRectDistance(p, topRect, topRadius);
            float bottomD = roundedRectDistance(p, bottomRect, bottomRadius);
            float d = min(topD, bottomD);
            float inside = 1.0 - smoothstep(-1.0, 1.0, d);
            if (inside <= 0.001) return original;

            float edge = 1.0 - smoothstep(0.0, 13.0, abs(d));
            float radius = topD < bottomD ? topRadius : bottomRadius;
            float4 selectedRect = topD < bottomD ? topRect : bottomRect;
            float dx = roundedRectDistance(p + float2(1.0, 0.0), selectedRect, radius)
                     - roundedRectDistance(p - float2(1.0, 0.0), selectedRect, radius);
            float dy = roundedRectDistance(p + float2(0.0, 1.0), selectedRect, radius)
                     - roundedRectDistance(p - float2(0.0, 1.0), selectedRect, radius);
            float2 normal = normalize(float2(dx, dy) + float2(0.0001, 0.0001));
            float2 refractedAt = p + normal * edge * 2.1 + float2(0.0, -0.7) * (1.0 - edge);
            half4 refracted = backdrop.eval(refractedAt);
            half3 tint = isDark > 0.5 ? half3(0.12, 0.17, 0.25) : half3(0.96, 0.98, 1.0);
            float tintAlpha = (0.10 + (1.0 - opacity) * 0.19) * inside;
            float fresnel = pow(edge, 1.65) * (0.22 + (1.0 - opacity) * 0.30);
            float topLight = (1.0 - smoothstep(0.0, 4.0, abs(p.y - topRect.y))) * inside;
            half3 color = mix(refracted.rgb, tint, tintAlpha);
            color += half3(1.0) * fresnel * (isDark > 0.5 ? 0.48 : 0.7);
            color += half3(0.24, 0.43, 0.76) * edge * (isDark > 0.5 ? 0.13 : 0.075);
            color += half3(1.0) * topLight * (isDark > 0.5 ? 0.045 : 0.09);
            return half4(color, original.a);
        }
        """.trimIndent(),
    )

    val renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop").asComposeRenderEffect()
}

@Composable
fun Modifier.glassMaterial(shape: Shape, dark: Boolean, opacity: Float = LocalGlassTransparency.current): Modifier {
    val edge = if (dark) Color(0x665F8FD8) else Color(0xB3FFFFFF)
    val shadow = if (dark) Color(0x66050A12) else Color(0x18293648)
    val veil = (0.11f + (1f - opacity) * 0.24f).coerceIn(0.18f, 0.35f)
    return shadow(12.dp, shape, clip = false, ambientColor = shadow, spotColor = shadow)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = when (outline) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }
            clipPath(path) {
                drawRect(if (dark) Color(0xFF141B27).copy(alpha = veil + 0.12f) else Color(0xFFFFFFFF).copy(alpha = veil + 0.12f))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.12f else 0.52f), Color.Transparent), endY = size.height * 0.48f))
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, edge, Color.White.copy(alpha = if (dark) 0.34f else 0.95f), edge, Color.Transparent)),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                drawRoundRect(color = Color.Black.copy(alpha = if (dark) 0.20f else 0.045f), style = Stroke(width = 1.dp.toPx()))
            }
        }
}
