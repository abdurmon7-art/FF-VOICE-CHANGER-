package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink

@Composable
fun AudioEqualizerVisualizer(
    bars: List<Float>,
    modifier: Modifier = Modifier,
    activeColorStart: Color = VibrantLavender,
    activeColorEnd: Color = VibrantRosePink,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    isActive: Boolean = true,
    barSpacing: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = bars.size
            if (totalBars == 0) return@Canvas

            val availableWidth = size.width
            val height = size.height
            val spacingPx = barSpacing.toPx()
            val singleBarWidth = ((availableWidth - (spacingPx * (totalBars - 1))) / totalBars).coerceIn(4f, 22f)

            val gradientBrush = Brush.verticalGradient(
                colors = if (isActive) listOf(activeColorEnd, activeColorStart)
                else listOf(inactiveColor, inactiveColor.copy(alpha = 0.4f))
            )

            var currentX = (availableWidth - (totalBars * singleBarWidth + (totalBars - 1) * spacingPx)) / 2f

            for (i in 0 until totalBars) {
                val value = if (isActive) bars[i].coerceIn(0.1f, 1.0f) else 0.1f
                val barHeight = height * value
                val topY = (height - barHeight) / 2f

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(currentX, topY),
                    size = Size(singleBarWidth, barHeight),
                    cornerRadius = CornerRadius(singleBarWidth / 2f, singleBarWidth / 2f),
                    alpha = if (isActive) (0.4f + 0.6f * value).coerceIn(0.4f, 1f) else 0.35f
                )

                currentX += singleBarWidth + spacingPx
            }
        }
    }
}
