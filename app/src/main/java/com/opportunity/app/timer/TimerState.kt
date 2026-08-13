package com.opportunity.app.timer

import com.opportunity.app.domain.model.TimerMode

enum class TimerRunState { IDLE, RUNNING, PAUSED, COMPLETED }

data class TimerUiState(
    val opportunityId: String,
    val opportunityTitle: String,
    val mode: TimerMode,
    val targetMinutes: Int?, // null for pure stopwatch
    val elapsedMillis: Long,
    val runState: TimerRunState
) {
    val elapsedMinutes: Int get() = (elapsedMillis / 60_000).toInt()
    val progressFraction: Float get() = targetMinutes?.takeIf { it > 0 }
        ?.let { (elapsedMillis / (it * 60_000.0)).coerceIn(0.0, 1.0).toFloat() } ?: 0f
    val isOverTarget: Boolean get() = targetMinutes != null && elapsedMinutes >= targetMinutes
}
