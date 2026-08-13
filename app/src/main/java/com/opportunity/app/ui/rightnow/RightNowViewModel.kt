package com.opportunity.app.ui.rightnow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.domain.usecase.NearbyPlaceMatch
import com.opportunity.app.domain.usecase.NearbyPlaceUseCase
import com.opportunity.app.domain.usecase.RightNowRecommendation
import com.opportunity.app.domain.usecase.RightNowUseCase
import com.opportunity.app.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RightNowViewModel @Inject constructor(
    private val rightNowUseCase: RightNowUseCase,
    private val nearbyPlaceUseCase: NearbyPlaceUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _availableMinutes = MutableStateFlow(30)
    val availableMinutes: StateFlow<Int> = _availableMinutes.asStateFlow()

    private val _recommendation = MutableStateFlow<RightNowRecommendation?>(null)
    val recommendation: StateFlow<RightNowRecommendation?> = _recommendation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _nearbyPlace = MutableStateFlow<NearbyPlaceMatch?>(null)
    val nearbyPlace: StateFlow<NearbyPlaceMatch?> = _nearbyPlace.asStateFlow()

    fun setAvailableMinutes(minutes: Int) {
        _availableMinutes.value = minutes
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _recommendation.value = rightNowUseCase.recommend(_availableMinutes.value)
            _isLoading.value = false
            if (locationRepository.hasPermission()) {
                _nearbyPlace.value = nearbyPlaceUseCase.currentPlaceMatch()
            }
        }
    }
}
