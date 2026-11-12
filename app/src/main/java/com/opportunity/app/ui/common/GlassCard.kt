package com.opportunity.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Smoked-glass Atelier card with warm surface gradient, optional smooth radial ambient glow,
 * soft elevation shadow, and subtle top-left glass sheen.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    ambientGlow: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val high = MaterialTheme.colorScheme.surfaceContainerHigh
    val low = MaterialTheme.colorScheme.surfaceContainerLow
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.50f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        high.copy(alpha = 0.90f),
                        low.copy(alpha = 0.96f)
                    )
                )
            )
            .then(
                if (ambientGlow) {
                    Modifier.drawBehind {
                        val glowCenter = Offset(size.width * 0.90f, size.height * 0.10f)
                        val glowRadius = (size.maxDimension * 0.65f).coerceAtLeast(180f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.14f),
                                    accent.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = glowCenter,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = glowCenter
                        )
                    }
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF4EC).copy(alpha = 0.12f),
                        Color(0xFFFFF4EC).copy(alpha = 0.02f)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}
