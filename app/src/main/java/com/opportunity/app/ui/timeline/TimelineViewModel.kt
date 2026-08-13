package com.opportunity.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.calendar.CalendarRepository
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed class TimelineBlock {
    abstract val startEpochMillis: Long
    abstract val endEpochMillis: Long

    data class Busy(val title: String, override val startEpochMillis: Long, override val endEpochMillis: Long) : TimelineBlock()
    data class Planned(val opportunity: OpportunityEntity, override val startEpochMillis: Long, override val endEpochMillis: Long) : TimelineBlock()
    data class Free(override val startEpochMillis: Long, override val endEpochMillis: Long) : TimelineBlock()
}

/**
 * Section 14/17.5: merges calendar events with planned Opportunities into one chronological
 * view, so scheduling isn't blind to the user's actual day.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val opportunityRepository: OpportunityRepository
) : ViewModel() {

    private val _blocks = MutableStateFlow<List<TimelineBlock>>(emptyList())
    val blocks: StateFlow<List<TimelineBlock>> = _blocks.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(calendarRepository.hasPermission())
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    fun onCalendarPermissionResult(granted: Boolean) {
        _hasCalendarPermission.value = granted
        if (granted) refresh()
    }

    fun refresh() {
        _hasCalendarPermission.value = calendarRepository.hasPermission()
        if (!_hasCalendarPermission.value) return
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val busyBlocks = calendarRepository.eventsBetween(startOfDay, endOfDay)
                .map { TimelineBlock.Busy(it.title, it.startEpochMillis, it.endEpochMillis) }

            val plannedBlocks = opportunityRepository.observeDueBetween(startOfDay, endOfDay).first()
                .mapNotNull { opp ->
                    val start = opp.preferredTimeMinuteOfDay?.let { startOfDay + it * 60_000L } ?: return@mapNotNull null
                    val duration = (opp.estimatedDurationMinutes ?: 30) * 60_000L
                    TimelineBlock.Planned(opp, start, start + duration)
                }

            _blocks.value = (busyBlocks + plannedBlocks).sortedBy { it.startEpochMillis }
        }
    }
}
