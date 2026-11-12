package com.opportunity.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DateFormattingTest {

    @Test
    fun `formatDuration formats minutes and hours cleanly`() {
        assertEquals("0m", DateFormatting.formatDuration(0))
        assertEquals("45m", DateFormatting.formatDuration(45))
        assertEquals("1h 30m", DateFormatting.formatDuration(90))
        assertEquals("0h 45m", DateFormatting.formatDuration(45, showZeroHours = true))
        assertEquals("0m", DateFormatting.formatDuration(-10))
    }

    @Test
    fun `formatDeadlineBadge detects overdue, today, and tomorrow`() {
        val zone = ZoneId.of("UTC")
        val noonToday = LocalDate.now(zone).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val (overdueBadge, overdueUrgency) = DateFormatting.formatDeadlineBadge(
            deadlineEpochMillis = noonToday - 3_600_000L,
            nowMillis = noonToday,
            zone = zone
        )
        assertEquals("OVERDUE", overdueBadge)
        assertEquals(DeadlineUrgency.OVERDUE, overdueUrgency)

        val (todayBadge, todayUrgency) = DateFormatting.formatDeadlineBadge(
            deadlineEpochMillis = noonToday + 3_600_000L,
            nowMillis = noonToday,
            zone = zone
        )
        assertEquals("TODAY", todayBadge)
        assertEquals(DeadlineUrgency.TODAY, todayUrgency)

        val (tomorrowBadge, tomorrowUrgency) = DateFormatting.formatDeadlineBadge(
            deadlineEpochMillis = noonToday + 86_400_000L,
            nowMillis = noonToday,
            zone = zone
        )
        assertEquals("TOMORROW", tomorrowBadge)
        assertEquals(DeadlineUrgency.TOMORROW, tomorrowUrgency)
    }
}
