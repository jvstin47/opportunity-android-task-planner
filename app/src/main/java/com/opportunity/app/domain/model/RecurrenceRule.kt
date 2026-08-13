package com.opportunity.app.domain.model

import kotlinx.serialization.Serializable

enum class RecurrenceFrequency { NONE, DAILY, WEEKLY, CUSTOM_INTERVAL_DAYS, EVENING_UNTIL_COMPLETED }

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val intervalDays: Int = 1,
    val daysOfWeek: Set<Int> = emptySet(), // 1 = Monday .. 7 = Sunday
    val stopOnCompletion: Boolean = true
)

enum class EscalationLevel { GENTLE, FOLLOW_UP, CONCERNED, URGENT }

@Serializable
data class ReminderConfig(
    val enabled: Boolean = false,
    val frequencyMinutes: Int = 0,
    val windowStartMinuteOfDay: Int? = null,
    val windowEndMinuteOfDay: Int? = null,
    val nextReminderEpochMillis: Long? = null,
    val escalationLevel: EscalationLevel = EscalationLevel.GENTLE,
    val snoozeCount: Int = 0
)

@Serializable
data class SessionPlan(
    val sessionsPerWeek: Int = 0,
    val minutesPerSession: Int = 0,
    val targetTotalMinutes: Int = 0
)
