package com.example.kampus.ui.onboarding
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent

private val NeonPink   = Color(0xFFFF4D8D)
private val NeonOrange = Color(0xFFFFA94D)

@Composable
fun OnboardingIllustration(
    @DrawableRes imageRes : Int,
    modifier              : Modifier = Modifier
) {
    val inf = rememberInfiniteTransition(label = "illus")

    // Float up and down
    val floatY by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = -14f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Glow pulse
    val glowAlpha by inf.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.75f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Glow size pulse
    val glowScale by inf.animateFloat(
        initialValue  = 0.75f,
        targetValue   = 0.92f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
    ) {
        // ── Layered glow blobs ─────────────────────────────────────────────
        // Outer soft pink blob
        Box(
            modifier = Modifier
                .fillMaxSize(glowScale)
                .clip(CircleShape)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPink.copy(alpha = glowAlpha * 0.45f),
                            NeonOrange.copy(alpha = glowAlpha * 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Inner concentrated hot glow
        Box(
            modifier = Modifier
                .fillMaxSize(glowScale * 0.55f)
                .clip(CircleShape)
                .blur(20.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPink.copy(alpha = glowAlpha * 0.30f),
                            NeonOrange.copy(alpha = glowAlpha * 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── The actual image — Multiply removes white bg + neon shadow ────
        Image(
            painter            = painterResource(id = imageRes),
            contentDescription = null,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY        = floatY
                    compositingStrategy = CompositingStrategy.Offscreen
                    // Neon drop shadow — pink below, orange offset
                    shadowElevation    = 0f
                }
                .drawWithContent {
                    val capturedGlow = glowAlpha
                    drawIntoCanvas { canvas ->
                        // ── Shadow pass 1: pink glow below ────────────────
                        val shadowPaintPink = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias  = true
                                color        = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    60f,   // blur radius
                                    0f,    // dx
                                    18f,   // dy
                                    android.graphics.Color.argb(
                                        (capturedGlow * 200).toInt(),
                                        255, 77, 141   // NeonPink
                                    )
                                )
                            }
                        }
                        // ── Shadow pass 2: orange glow offset ─────────────
                        val shadowPaintOrange = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias  = true
                                color        = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    40f,
                                    12f,
                                    12f,
                                    android.graphics.Color.argb(
                                        (capturedGlow * 150).toInt(),
                                        255, 169, 77   // NeonOrange
                                    )
                                )
                            }
                        }

                        // Draw shadow layers first
                        canvas.saveLayer(
                            androidx.compose.ui.geometry.Rect(
                                -80f, -80f,
                                size.width + 80f,
                                size.height + 80f
                            ),
                            shadowPaintPink
                        )
                        drawContent()
                        canvas.restore()

                        canvas.saveLayer(
                            androidx.compose.ui.geometry.Rect(
                                -80f, -80f,
                                size.width + 80f,
                                size.height + 80f
                            ),
                            shadowPaintOrange
                        )
                        drawContent()
                        canvas.restore()

                        // ── Final image with Multiply to remove white bg ──
                        canvas.saveLayer(
                            androidx.compose.ui.geometry.Rect(
                                0f, 0f, size.width, size.height
                            ),
                            Paint().apply { blendMode = BlendMode.Multiply }
                        )
                        drawContent()
                        canvas.restore()
                    }
                }
        )
    }
}