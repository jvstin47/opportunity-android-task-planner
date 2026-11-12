package com.opportunity.app.ui.rightnow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.NearbyPlaceMatch
import com.opportunity.app.domain.usecase.NearbyPlaceUseCase
import com.opportunity.app.domain.usecase.RightNowRecommendation
import com.opportunity.app.domain.usecase.RightNowUseCase
import com.opportunity.app.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable UI state for the Focus ("Right Now") screen. */
data class RightNowUiState(
    val availableMinutes: Int = 30,
    val recommendation: RightNowRecommendation? = null,
    val nearbyPlace: NearbyPlaceMatch? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class RightNowViewModel @Inject constructor(
    private val rightNowUseCase: RightNowUseCase,
    private val nearbyPlaceUseCase: NearbyPlaceUseCase,
    private val locationRepository: LocationRepository,
    private val repository: OpportunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RightNowUiState())
    val uiState: StateFlow<RightNowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllActionable()
                .collect { loadRecommendation(showLoading = false) }
        }
    }

    fun setAvailableMinutes(minutes: Int) {
        _uiState.update { it.copy(availableMinutes = minutes) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { loadRecommendation(showLoading = true) }
    }

    private suspend fun loadRecommendation(showLoading: Boolean) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
        val minutes = _uiState.value.availableMinutes
        runCatching { rightNowUseCase.recommend(minutes) }
            .onSuccess { rec ->
                _uiState.update { it.copy(recommendation = rec, isLoading = false, error = null) }
            }
            .onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = "Couldn't compute focus recommendation: ${err.message}") }
            }
        if (locationRepository.hasPermission()) {
            val match = runCatching { nearbyPlaceUseCase.currentPlaceMatch() }.getOrNull()
            _uiState.update { it.copy(nearbyPlace = match) }
        }
    }
}
