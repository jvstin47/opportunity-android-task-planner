package com.opportunity.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.opportunity.app.domain.model.PriorityBucket

/**
 * Section 12 bucket colors. Primary is reserved for brand actions (buttons, selected nav) --
 * these are a separate, muted categorical palette so priority dots don't compete with it.
 */
fun PriorityBucket?.toColor(): Color = when (this) {
    PriorityBucket.DO_SOON -> ErrorColor
    PriorityBucket.SHOULD_DO -> Tertiary
    PriorityBucket.GROW -> GrowGreen
    PriorityBucket.EXPLORE -> Secondary
    PriorityBucket.PERSONAL -> PersonalRose
    PriorityBucket.SOMEDAY, null -> Outline
}

fun PriorityBucket?.emoji(): String = when (this) {
    PriorityBucket.DO_SOON -> "🔥"
    PriorityBucket.SHOULD_DO -> "📌"
    PriorityBucket.GROW -> "🌱"
    PriorityBucket.EXPLORE -> "💡"
    PriorityBucket.PERSONAL -> "❤️"
    PriorityBucket.SOMEDAY, null -> "🗃"
}
