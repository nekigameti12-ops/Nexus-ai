package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexusBlue
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusNeonViolet

@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    amplitude: Float = 0f,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    height: Dp = 32.dp,
    barWidth: Dp = 3.dp,
    activeColor: Color = NexusCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animDuration = 400 + (i % 4) * 120
            val animatedHeightFraction by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            val effectiveHeightFraction = if (isActive) {
                (animatedHeightFraction * 0.4f + amplitude * 0.6f).coerceIn(0.15f, 1f)
            } else {
                0.15f
            }

            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxHeight(effectiveHeightFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isActive) {
                                listOf(NexusCyan, NexusBlue, NexusNeonViolet)
                            } else {
                                listOf(Color(0xFF334155), Color(0xFF1E293B))
                            }
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
