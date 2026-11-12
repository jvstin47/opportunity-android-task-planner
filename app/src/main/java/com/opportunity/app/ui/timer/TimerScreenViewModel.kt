package com.opportunity.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.timer.TimerController
import com.opportunity.app.timer.TimerUiState
import com.opportunity.app.util.DateFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for the Focus Session Timer screen. Coordinates timer lifecycle controls,
 * opportunity metadata, daily deep-work telemetry, and optional session jotter notes.
 */
@HiltViewModel
class TimerScreenViewModel @Inject constructor(
    private val timerController: TimerController,
    private val repository: OpportunityRepository
) : ViewModel() {

    val timerState: StateFlow<TimerUiState?> = timerController.state

    private val _opportunityTitle = MutableStateFlow("")
    val opportunityTitle: StateFlow<String> = _opportunityTitle.asStateFlow()

    private val _opportunityCategory = MutableStateFlow<String?>(null)
    val opportunityCategory: StateFlow<String?> = _opportunityCategory.asStateFlow()

    private val _todayFocusedDuration = MutableStateFlow("0m")
    val todayFocusedDuration: StateFlow<String> = _todayFocusedDuration.asStateFlow()

    private val _completedTodayCount = MutableStateFlow(0)
    val completedTodayCount: StateFlow<Int> = _completedTodayCount.asStateFlow()

    init {
        refreshDailyStats()
    }

    fun beginIfNeeded(opportunityId: String) {
        val existing = timerController.state.value
        if (existing?.opportunityId == opportunityId) {
            _opportunityTitle.value = existing.opportunityTitle
            viewModelScope.launch {
                _opportunityCategory.value = repository.getById(opportunityId)?.category
            }
            return
        }
        viewModelScope.launch {
            val opportunity = repository.getById(opportunityId) ?: return@launch
            _opportunityTitle.value = opportunity.title
            _opportunityCategory.value = opportunity.category
            timerController.start(
                opportunityId = opportunity.id,
                opportunityTitle = opportunity.title,
                mode = opportunity.timerMode,
                targetMinutes = opportunity.estimatedDurationMinutes
            )
        }
    }

    private fun refreshDailyStats() {
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val endOfDay = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sessions = runCatching { repository.sessionsBetween(startOfDay, endOfDay) }.getOrDefault(emptyList())
            val totalMinutes = sessions.sumOf { it.durationMinutes }
            _todayFocusedDuration.value = DateFormatting.formatDuration(totalMinutes)

            val completed = runCatching { repository.observeCompletedBetween(startOfDay, endOfDay).first() }
                .getOrDefault(emptyList())
            _completedTodayCount.value = completed.size
        }
    }

    fun pause() = timerController.pause()
    fun resume() = timerController.resume()

    /** Completes the sprint and logs an optional session jotter note. */
    fun finish(note: String? = null) {
        timerController.stopAndLog(note?.trim()?.takeIf { it.isNotEmpty() })
    }

    fun discard() = timerController.discard()
}
