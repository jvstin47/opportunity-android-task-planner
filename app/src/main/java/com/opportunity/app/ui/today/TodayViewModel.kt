package com.opportunity.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.TodayPlan
import com.opportunity.app.domain.usecase.TodayPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: OpportunityRepository,
    private val todayPlanUseCase: TodayPlanUseCase
) : ViewModel() {

    private val _plan = MutableStateFlow<TodayPlan?>(null)
    val plan: StateFlow<TodayPlan?> = _plan.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _plan.value = todayPlanUseCase.buildPlan()
            _isLoading.value = false
        }
    }

    fun complete(id: String) = viewModelScope.launch {
        repository.complete(id)
        refresh()
    }
}
