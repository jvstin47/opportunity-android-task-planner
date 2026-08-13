package com.opportunity.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.domain.model.Place
import com.opportunity.app.location.LocationRepository
import com.opportunity.app.location.PlaceStore
import com.opportunity.app.sync.SyncRepository
import com.opportunity.app.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val syncEnabled: Boolean = false,
    val error: String? = null,
    val isAddingPlace: Boolean = false,
    val placeError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val syncScheduler: SyncScheduler,
    private val placeStore: PlaceStore,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val places: StateFlow<List<Place>> = placeStore.places
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPlaceFromCurrentLocation(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingPlace = true, placeError = null)
            val location = locationRepository.currentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isAddingPlace = false,
                    placeError = "Couldn't get your current location. Check location permission and try again."
                )
                return@launch
            }
            placeStore.addPlace(name, location.latitude, location.longitude)
            _uiState.value = _uiState.value.copy(isAddingPlace = false)
        }
    }

    fun removePlace(id: String) = viewModelScope.launch { placeStore.removePlace(id) }

    fun enableSync(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            runCatching {
                syncRepository.ensureLoggedIn(email)
                syncRepository.syncNow().getOrThrow()
            }.onSuccess {
                syncScheduler.ensureScheduled()
                _uiState.value = SettingsUiState(syncEnabled = true)
            }.onFailure {
                _uiState.value = SettingsUiState(error = "Couldn't reach the sync service: ${it.message}")
            }
        }
    }
}
