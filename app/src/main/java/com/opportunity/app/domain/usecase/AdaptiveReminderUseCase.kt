package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.EscalationLevel
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderContent(val title: String, val body: String, val level: EscalationLevel)

/**
 * Section 8 "Adaptive Reminder System": reminder copy changes tone based on how many times an
 * item has been ignored/postponed and how close its deadline is -- not a flat repeating alert.
 */
@Singleton
class AdaptiveReminderUseCase @Inject constructor() {

    fun buildReminder(opportunity: OpportunityEntity, now: Instant = Instant.now()): ReminderContent {
        val hoursToDeadline = opportunity.deadlineEpochMillis?.let {
            Duration.between(now, Instant.ofEpochMilli(it)).toHours()
        }

        if (hoursToDeadline != null && hoursToDeadline <= 36) {
            val remaining = opportunity.estimatedDurationMinutes
            val remainingText = remaining?.let { "~$it minutes estimated remaining" } ?: "deadline approaching"
            return ReminderContent(
                title = "⚠️ ${opportunity.title}",
                body = "Due ${if (hoursToDeadline < 0) "overdue" else if (hoursToDeadline <= 24) "today" else "tomorrow"} · $remainingText",
                level = EscalationLevel.URGENT
            )
        }

        return when (opportunity.postponementCount) {
            0 -> ReminderContent(
                title = opportunity.title,
                body = "You planned to work on this today.",
                level = EscalationLevel.GENTLE
            )
            in 1..2 -> ReminderContent(
                title = opportunity.title,
                body = "Still want to do this tonight?",
                level = EscalationLevel.FOLLOW_UP
            )
            else -> ReminderContent(
                title = opportunity.title,
                body = "You've postponed this ${opportunity.postponementCount} times. Move it to tomorrow?",
                level = EscalationLevel.CONCERNED
            )
        }
    }
}
