package com.opportunity.app.ui.growth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.ai.AiResult
import com.opportunity.app.ai.ReflectionGenerator
import com.opportunity.app.ai.WeeklyStats
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.util.DateFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class GrowthListState(
    val goals: List<OpportunityEntity> = emptyList(),
    val ideas: List<OpportunityEntity> = emptyList(),
    val someday: List<OpportunityEntity> = emptyList(),
    val habits: List<OpportunityEntity> = emptyList()
)

data class GrowthStatsState(
    val completedCount: Int = 0,
    val postponedCount: Int = 0,
    val focusedMinutes: Int = 0,
    val formattedFocusDuration: String = "0m",
    val mostProductiveHourRange: String? = null,
    val reflection: String = DEFAULT_REFLECTION,
    val isLoadingReflection: Boolean = false,
    val error: String? = null
) {
    companion object {
        const val DEFAULT_REFLECTION =
            "Take a breath. You are building momentum one small step at a time. Every focused session counts."
    }
}

/** Growth tab: merged Explore (goals/ideas/someday) + Insights (weekly stats & AI reflection). */
@HiltViewModel
class GrowthViewModel @Inject constructor(
    private val repository: OpportunityRepository,
    private val reflectionGenerator: ReflectionGenerator
) : ViewModel() {

    val lists: StateFlow<GrowthListState> = combine(
        repository.observeByType(OpportunityType.GOAL),
        repository.observeByType(OpportunityType.IDEA),
        repository.observeByType(OpportunityType.SOMEDAY),
        repository.observeByType(OpportunityType.HABIT)
    ) { goals, ideas, someday, habits -> GrowthListState(goals, ideas, someday, habits) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GrowthListState())

    private val _stats = MutableStateFlow(GrowthStatsState())
    val stats: StateFlow<GrowthStatsState> = _stats.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            runCatching {
                val now = Instant.now()
                val weekAgo = now.minus(7, ChronoUnit.DAYS)
                val completed = repository.getCompletedBetween(weekAgo.toEpochMilli(), now.toEpochMilli())
                val sessions = repository.sessionsBetween(weekAgo.toEpochMilli(), now.toEpochMilli())
                val focusedMinutes = sessions.sumOf { it.durationMinutes }

                val hourCounts = sessions.groupingBy {
                    Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(ZoneId.systemDefault()).hour
                }.eachCount()
                val peakHour = hourCounts.maxByOrNull { it.value }?.key
                val productiveRange = peakHour?.let { "${it}:00 - ${it + 1}:00" }
                val postponedCount = repository.observeFrequentlyPostponed(1).first().sumOf { it.postponementCount }

                _stats.update {
                    it.copy(
                        completedCount = completed.size,
                        postponedCount = postponedCount,
                        focusedMinutes = focusedMinutes,
                        formattedFocusDuration = DateFormatting.formatDuration(focusedMinutes),
                        mostProductiveHourRange = productiveRange,
                        error = null
                    )
                }
            }.onSuccess {
                generateReflection()
            }.onFailure { err ->
                _stats.update { it.copy(error = "Couldn't load stats: ${err.message}") }
            }
        }
    }

    private fun generateReflection() {
        viewModelScope.launch {
            _stats.update { it.copy(isLoadingReflection = true) }
            val state = _stats.value
            val result = runCatching {
                reflectionGenerator.generate(
                    WeeklyStats(
                        completedCount = state.completedCount,
                        postponedCount = state.postponedCount,
                        focusedMinutes = state.focusedMinutes,
                        mostProductiveHourRange = state.mostProductiveHourRange,
                        mostPostponedCategory = null
                    )
                )
            }.getOrNull()

            _stats.update {
                it.copy(
                    isLoadingReflection = false,
                    reflection = (result as? AiResult.Success)?.value?.takeIf { text -> text.isNotBlank() }
                        ?: GrowthStatsState.DEFAULT_REFLECTION
                )
            }
        }
    }
}
