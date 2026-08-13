package com.opportunity.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.opportunity.app.domain.model.PriorityBucket

/** Section 12 bucket colors, mapped onto the redesign palette (theme/Color.kt). */
fun PriorityBucket?.toColor(): Color = when (this) {
    PriorityBucket.DO_SOON -> ErrorColor
    PriorityBucket.SHOULD_DO -> Tertiary
    PriorityBucket.GROW -> Secondary
    PriorityBucket.EXPLORE -> Primary
    PriorityBucket.PERSONAL -> Color(0xFFEC4899)
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
