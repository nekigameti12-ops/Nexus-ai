package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBlue
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusNeonViolet
import com.example.ui.theme.NexusRed
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

@Composable
fun NexusOrb(
    orbState: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    amplitude: Float = 0f,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (orbState == OrbState.THINKING) 2000 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation1"
    )

    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (orbState == OrbState.THINKING) 3000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation2"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (orbState) {
                    OrbState.LISTENING -> 600
                    OrbState.THINKING -> 800
                    OrbState.SPEAKING -> 500
                    OrbState.ERROR -> 400
                    OrbState.IDLE -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = when (orbState) {
        OrbState.IDLE -> NexusCyan
        OrbState.LISTENING -> NexusAmber
        OrbState.THINKING -> NexusNeonViolet
        OrbState.SPEAKING -> NexusGreen
        OrbState.ERROR -> NexusRed
    }

    val secondaryColor = when (orbState) {
        OrbState.IDLE -> NexusBlue
        OrbState.LISTENING -> Color(0xFFFF6F00)
        OrbState.THINKING -> NexusCyan
        OrbState.SPEAKING -> NexusCyan
        OrbState.ERROR -> Color(0xFFB71C1C)
    }

    val effectivePulse = pulse + (amplitude * 0.15f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 3.4f * effectivePulse

            // 1. Outer Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.6f
                ),
                radius = baseRadius * 1.6f,
                center = center
            )

            // 2. Orbital Ring 1 (Clockwise with Nodes)
            drawOrbitalRing(
                center = center,
                radius = baseRadius * 1.28f,
                angle = rotation1,
                color = primaryColor,
                numNodes = 4
            )

            // 3. Orbital Ring 2 (Counter-Clockwise with Nodes)
            drawOrbitalRing(
                center = center,
                radius = baseRadius * 1.45f,
                angle = rotation2,
                color = secondaryColor,
                numNodes = 3,
                dashed = true
            )

            // 4. Core Pulsing Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.9f),
                        Color(0xFF030712)
                    ),
                    center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.15f),
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 5. Glowing Outer Core Border
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }
    }
}

private fun DrawScope.drawOrbitalRing(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color,
    numNodes: Int,
    dashed: Boolean = false
) {
    // Draw ring
    drawCircle(
        color = color.copy(alpha = if (dashed) 0.35f else 0.5f),
        radius = radius,
        center = center,
        style = Stroke(width = if (dashed) 1.5f else 2f)
    )

    // Draw orbiting nodes
    val step = (2 * Math.PI / numNodes).toFloat()
    val radAngle = Math.toRadians(angle.toDouble()).toFloat()

    for (i in 0 until numNodes) {
        val currentAngle = radAngle + (i * step)
        val x = center.x + radius * cos(currentAngle)
        val y = center.y + radius * sin(currentAngle)

        drawCircle(
            color = Color.White,
            radius = 4f,
            center = Offset(x, y)
        )
        drawCircle(
            color = color.copy(alpha = 0.6f),
            radius = 7f,
            center = Offset(x, y)
        )
    }
}
