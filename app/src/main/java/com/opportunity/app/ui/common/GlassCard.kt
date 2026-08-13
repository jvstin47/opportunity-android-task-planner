package com.opportunity.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Approximates the mockups' `.glass-card` (translucent surface + soft border + blur). Real
 * backdrop-blur (blurring whatever sits behind the card) isn't available in Compose without a
 * third-party library (e.g. Haze) -- this uses a semi-transparent surface-container-high plus a
 * subtle white border instead, which reads as "glass" against the app's dark background even
 * without true blur.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        content = { content() }
    )
}

fun Modifier.glassBorder(cornerRadius: Dp = 24.dp) = this.background(
    color = Color.White.copy(alpha = 0.03f),
    shape = RoundedCornerShape(cornerRadius)
)
