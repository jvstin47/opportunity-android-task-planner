package com.opportunity.app.data.local

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object OpportunityDateHelpers {
    /** Used by "Move to tomorrow" -- a reasonable default deadline, not tied to any specific reminder time. */
    fun tomorrowMorningEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).plusDays(1).atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()

    fun endOfTodayEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()

    fun endOfTomorrowEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).plusDays(1).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()

    fun endOfThisWeekEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).plusDays(6).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
}
