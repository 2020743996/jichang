package com.jzb.jichang.android

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.unit.dp

val LocalGlassTransparency = compositionLocalOf { 0.45f }

/** GPU backdrop refraction, restricted to the measured top and bottom chrome regions. */
class LiquidGlassScene {
    val shader = RuntimeShader(
        """
        uniform shader backdrop;
        uniform float4 topRect;
        uniform float4 bottomRect;
        uniform float topRadius;
        uniform float bottomRadius;
        uniform float opacity;

        float roundedRectDistance(float2 p, float4 rect, float radius) {
            float2 center = (rect.xy + rect.zw) * 0.5;
            float2 halfSize = (rect.zw - rect.xy) * 0.5;
            float2 q = abs(p - center) - halfSize + radius;
            return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
        }

        half4 glassSample(float2 p, float radius) {
            float2 diagonal = float2(radius * 0.70710678, radius * 0.70710678);
            half4 color = backdrop.eval(p) * 0.24;
            color += backdrop.eval(p + float2(radius, 0.0)) * 0.12;
            color += backdrop.eval(p - float2(radius, 0.0)) * 0.12;
            color += backdrop.eval(p + float2(0.0, radius)) * 0.12;
            color += backdrop.eval(p - float2(0.0, radius)) * 0.12;
            color += backdrop.eval(p + diagonal) * 0.07;
            color += backdrop.eval(p - diagonal) * 0.07;
            color += backdrop.eval(p + float2(diagonal.x, -diagonal.y)) * 0.07;
            color += backdrop.eval(p + float2(-diagonal.x, diagonal.y)) * 0.07;
            return color;
        }

        half4 main(float2 p) {
            half4 original = backdrop.eval(p);
            float topD = roundedRectDistance(p, topRect, topRadius);
            float bottomD = roundedRectDistance(p, bottomRect, bottomRadius);
            float d = min(topD, bottomD);
            float inside = 1.0 - smoothstep(-1.0, 1.0, d);
            if (inside <= 0.001) return original;

            float radius = topD < bottomD ? topRadius : bottomRadius;
            float4 selectedRect = topD < bottomD ? topRect : bottomRect;
            float dx = roundedRectDistance(p + float2(1.0, 0.0), selectedRect, radius)
                     - roundedRectDistance(p - float2(1.0, 0.0), selectedRect, radius);
            float dy = roundedRectDistance(p + float2(0.0, 1.0), selectedRect, radius)
                     - roundedRectDistance(p - float2(0.0, 1.0), selectedRect, radius);
            float2 normal = normalize(float2(dx, dy) + float2(0.0001, 0.0001));
            float edge = 1.0 - smoothstep(0.0, 20.0, abs(d));
            float strength = mix(0.78, 1.35, opacity);
            float wave = sin(p.x * 0.025 + p.y * 0.012) * 0.85;
            float2 refractedAt = p + normal * edge * (8.0 * strength + wave) + float2(0.0, -1.1 * strength);
            float blurRadius = mix(5.0, 2.8, opacity);
            half4 refracted = glassSample(refractedAt, blurRadius);

            float dispersion = edge * mix(1.1, 3.1, opacity);
            half red = backdrop.eval(refractedAt + normal * dispersion).r;
            half blue = backdrop.eval(refractedAt - normal * dispersion).b;
            half3 color = refracted.rgb;
            color.r = mix(color.r, red, edge * 0.68);
            color.b = mix(color.b, blue, edge * 0.68);

            half3 clearTint = half3(0.88, 0.94, 1.0);
            float tintAlpha = mix(0.15, 0.035, opacity) * inside;
            color = mix(color, clearTint, tintAlpha);

            float fresnel = pow(edge, 1.45) * mix(0.24, 0.55, opacity);
            float topLight = (1.0 - smoothstep(0.0, 5.0, abs(p.y - topRect.y))) * inside;
            color += half3(1.0) * fresnel * 0.72;
            color += half3(0.24, 0.48, 0.9) * edge * 0.10;
            color += half3(1.0) * topLight * 0.13;
            return half4(color, original.a);
        }
        """.trimIndent(),
    )

    // The shader instance and RenderEffect stay cached; only its uniforms change as the UI moves.
    val renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "backdrop").asComposeRenderEffect()
}

@Composable
fun Modifier.glassMaterial(shape: Shape, opacity: Float = LocalGlassTransparency.current): Modifier {
    val edge = Color(0xA6FFFFFF)
    val shadow = Color(0x18293648)
    val isSystemBarSurface = shape === androidx.compose.ui.graphics.RectangleShape
    return shadow(if (isSystemBarSurface) 0.dp else 12.dp, shape, clip = false, ambientColor = shadow, spotColor = shadow)
        .drawBehind {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = when (outline) {
                is Outline.Generic -> outline.path
                is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
                is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            }
            clipPath(path) {
                // Keep the base veil nearly clear so the backdrop shader supplies the material.
                drawRect(Color(0xFFEAF2FF).copy(alpha = 0.025f + (1f - opacity) * 0.035f))
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.015f), Color.Transparent),
                        endY = size.height * 0.58f,
                    ),
                )
                drawLine(
                    brush = if (isSystemBarSurface) {
                        Brush.horizontalGradient(listOf(edge, edge.copy(alpha = 0.45f), edge))
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, edge, Color.White.copy(alpha = 0.95f), edge, Color.Transparent))
                    },
                    start = androidx.compose.ui.geometry.Offset(if (isSystemBarSurface) 0f else size.width * 0.08f, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(if (isSystemBarSurface) size.width else size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                if (!isSystemBarSurface) drawRoundRect(color = Color.Black.copy(alpha = 0.045f), style = Stroke(width = 1.dp.toPx()))
            }
        }
}
