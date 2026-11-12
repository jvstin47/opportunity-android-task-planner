package com.opportunity.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared formatting helpers for durations, deadlines, and timestamps across UI and widgets.
 * Keeps date/calendar logic out of Compose UI components and eliminates duplicated formatters.
 */
object DateFormatting {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")

    /** Formats [minutes] as `"Xh Ym"` or `"Ym"` (e.g. `"1h 30m"` or `"45m"`). */
    fun formatDuration(minutes: Int, showZeroHours: Boolean = false): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 || showZeroHours -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    /** Formats a deadline epoch millis into a short relative badge (`"OVERDUE"`, `"TODAY"`, `"TOMORROW"`, or `"MMM d"`). */
    fun formatDeadlineBadge(
        deadlineEpochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<String, DeadlineUrgency> {
        val deadlineDate = Instant.ofEpochMilli(deadlineEpochMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when {
            deadlineEpochMillis < nowMillis -> "OVERDUE" to DeadlineUrgency.OVERDUE
            deadlineDate == today -> "TODAY" to DeadlineUrgency.TODAY
            deadlineDate == today.plusDays(1) -> "TOMORROW" to DeadlineUrgency.TOMORROW
            else -> shortDateFormatter.format(deadlineDate) to DeadlineUrgency.UPCOMING
        }
    }

    /** Formats a deadline epoch millis for task cards (`"Overdue"`, `"Due today"`, `"Due tomorrow"`, or `"Due MMM d"`). */
    fun formatDeadlineLabel(
        deadlineEpochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val (badge, urgency) = formatDeadlineBadge(deadlineEpochMillis, nowMillis, zone)
        return when (urgency) {
            DeadlineUrgency.OVERDUE -> "Overdue"
            DeadlineUrgency.TODAY -> "Due today"
            DeadlineUrgency.TOMORROW -> "Due tomorrow"
            DeadlineUrgency.UPCOMING -> "Due $badge"
        }
    }

    fun formatShortDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        shortDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}

enum class DeadlineUrgency { OVERDUE, TODAY, TOMORROW, UPCOMING }
