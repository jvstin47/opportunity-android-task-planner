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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed class TimelineBlock {
    abstract val startEpochMillis: Long
    abstract val endEpochMillis: Long

    data class Busy(val title: String, override val startEpochMillis: Long, override val endEpochMillis: Long) : TimelineBlock()
    data class Planned(val opportunity: OpportunityEntity, override val startEpochMillis: Long, override val endEpochMillis: Long) : TimelineBlock()
}

data class TimelineUiState(
    val blocks: List<TimelineBlock> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Merges device calendar events with time-blocked Opportunities into a single chronological view.
 */
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val opportunityRepository: OpportunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState(hasCalendarPermission = calendarRepository.hasPermission()))
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    fun onCalendarPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasCalendarPermission = granted) }
        if (granted) refresh()
    }

    fun refresh() {
        val permitted = calendarRepository.hasPermission()
        _uiState.update { it.copy(hasCalendarPermission = permitted) }
        if (!permitted) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
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

                (busyBlocks + plannedBlocks).sortedBy { it.startEpochMillis }
            }.onSuccess { sorted ->
                _uiState.update { it.copy(blocks = sorted, isLoading = false, error = null) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = "Couldn't load timeline: ${err.message}") }
            }
        }
    }
}
