package com.opportunity.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.TodayPlan
import com.opportunity.app.domain.usecase.TodayPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Immutable UI state for the Today Plan screen.
 * Keeps filtering and plan computation inside [TodayViewModel] rather than Compose UI.
 */
data class TodayUiState(
    val plan: TodayPlan? = null,
    val completedToday: List<OpportunityEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val hasAnyItems: Boolean
        get() = (plan != null && !plan.isEmpty()) || completedToday.isNotEmpty()
}

fun TodayPlan.isEmpty(): Boolean =
    mustHappen.isEmpty() && recommended.isEmpty() && ifYouHaveTime.isEmpty()

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: OpportunityRepository,
    private val todayPlanUseCase: TodayPlanUseCase
) : ViewModel() {

    private val _rawPlan = MutableStateFlow<TodayPlan?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    private val completedTodayFlow = run {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        repository.observeCompletedBetween(start, end)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        _rawPlan,
        completedTodayFlow,
        _searchQuery,
        _isLoading,
        _error
    ) { plan, completed, query, isLoading, error ->
        val normalized = query.trim().lowercase()
        val filteredPlan = if (plan == null || normalized.isBlank()) {
            plan
        } else {
            plan.copy(
                mustHappen = plan.mustHappen.filterByQuery(normalized),
                recommended = plan.recommended.filterByQuery(normalized),
                ifYouHaveTime = plan.ifYouHaveTime.filterByQuery(normalized)
            )
        }
        val filteredCompleted = if (normalized.isBlank()) {
            completed
        } else {
            completed.filterByQuery(normalized)
        }
        TodayUiState(
            plan = filteredPlan,
            completedToday = filteredCompleted,
            searchQuery = query,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    init {
        viewModelScope.launch {
            repository.observeAllActionable()
                .collect { loadPlan(showLoading = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch { loadPlan(showLoading = true) }
    }

    fun complete(id: String) = viewModelScope.launch {
        runCatching { repository.complete(id) }
            .onSuccess { loadPlan(showLoading = false) }
            .onFailure { _error.value = "Could not complete task: ${it.message}" }
    }

    fun reopen(id: String) = viewModelScope.launch {
        runCatching { repository.reopen(id) }
            .onSuccess { loadPlan(showLoading = false) }
            .onFailure { _error.value = "Could not reopen task: ${it.message}" }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun loadPlan(showLoading: Boolean) {
        if (showLoading) _isLoading.value = true
        runCatching { todayPlanUseCase.buildPlan() }
            .onSuccess {
                _rawPlan.value = it
                _error.value = null
            }
            .onFailure {
                _error.value = "Couldn't load today's plan: ${it.message}"
            }
        _isLoading.value = false
    }

    private fun List<OpportunityEntity>.filterByQuery(normalizedQuery: String): List<OpportunityEntity> =
        filter {
            it.title.lowercase().contains(normalizedQuery) ||
                it.category?.lowercase()?.contains(normalizedQuery) == true
        }
}
