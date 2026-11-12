package com.opportunity.app.domain.usecase

import com.opportunity.app.calendar.CalendarRepository
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class TodayPlan(
    val mustHappen: List<OpportunityEntity>,
    val recommended: List<OpportunityEntity>,
    val ifYouHaveTime: List<OpportunityEntity>,
    val plannedMinutes: Int,
    val availableMinutes: Int?, // null when calendar permission isn't granted -- no capacity check without it
    val suggestedToMove: List<OpportunityEntity>
) {
    val overCapacityMinutes: Int
        get() = availableMinutes?.let { (plannedMinutes - it).coerceAtLeast(0) } ?: 0
}

/**
 * Section 17.1, reimagined per the phase-1 roadmap: instead of a flat "due today" list, Today
 * buckets the day's actionable set into what's non-negotiable, what's a good use of time, and
 * what's a bonus if there's room -- then checks that plan against real free time from the
 * calendar (section 10 "Smart Scheduling": don't pretend everything will get done).
 */
@Singleton
class TodayPlanUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val scorer: OpportunityScorer,
    private val calendarRepository: CalendarRepository
) {
    suspend fun buildPlan(zone: ZoneId = ZoneId.systemDefault()): TodayPlan {
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

        val candidates = repository.observeAllActionable().first()
            .filter { it.status == OpportunityStatus.ACTIVE || it.status == OpportunityStatus.IN_PROGRESS }

        val (dueOrOverdue, rest) = candidates.partition {
            it.deadlineEpochMillis != null &&
                it.deadlineEpochMillis < endOfDay.toEpochMilli()
        }

        val mustHappen = dueOrOverdue.sortedBy { it.deadlineEpochMillis }

        val scoredRest = rest
            .map { it to scorer.score(it, availableMinutes = null, now = now) }
            .sortedByDescending { it.second }

        val recommended = scoredRest.take(MAX_RECOMMENDED).map { it.first }
        val ifYouHaveTime = scoredRest.drop(MAX_RECOMMENDED).take(MAX_IF_YOU_HAVE_TIME).map { it.first }

        val shown = mustHappen + recommended + ifYouHaveTime
        val plannedMinutes = shown.sumOf { it.estimatedDurationMinutes ?: 0 }

        val availableMinutes = if (calendarRepository.hasPermission()) {
            calendarRepository.freeWindowsBetween(startOfDay.toEpochMilli(), endOfDay.toEpochMilli())
                .sumOf { it.minutes }
        } else null

        val suggestedToMove = if (availableMinutes != null && plannedMinutes > availableMinutes) {
            val gap = plannedMinutes - availableMinutes
            val movable = (ifYouHaveTime + recommended) // lowest-priority first
            var reclaimed = 0
            movable.takeWhile { item ->
                if (reclaimed >= gap) return@takeWhile false
                reclaimed += item.estimatedDurationMinutes ?: 0
                true
            }
        } else emptyList()

        return TodayPlan(mustHappen, recommended, ifYouHaveTime, plannedMinutes, availableMinutes, suggestedToMove)
    }

    companion object {
        private const val MAX_RECOMMENDED = 4
        private const val MAX_IF_YOU_HAVE_TIME = 4
    }
}
