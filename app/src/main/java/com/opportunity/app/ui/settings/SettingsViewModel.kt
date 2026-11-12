package com.opportunity.app.ui.settings

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.domain.model.Place
import com.opportunity.app.location.LocationRepository
import com.opportunity.app.location.PlaceStore
import com.opportunity.app.sync.SyncRepository
import com.opportunity.app.sync.SyncScheduler
import com.opportunity.app.util.DateFormatting
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val syncEnabled: Boolean = false,
    val syncEmail: String? = null,
    val formattedLastSync: String? = null,
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

    init {
        viewModelScope.launch {
            runCatching {
                val connected = syncRepository.isConnected()
                val email = syncRepository.connectedEmail()
                val lastSync = syncRepository.lastSyncEpochMillis()
                _uiState.update {
                    it.copy(
                        syncEnabled = connected,
                        syncEmail = email,
                        formattedLastSync = lastSync.takeIf { ts -> ts > 0L }?.let(DateFormatting::formatDateTime)
                    )
                }
            }
        }
    }

    fun addPlaceFromCurrentLocation(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingPlace = true, placeError = null) }
            runCatching {
                val location = locationRepository.currentLocation()
                if (location == null) {
                    _uiState.update {
                        it.copy(
                            isAddingPlace = false,
                            placeError = "Couldn't get your current location. Check location permission and try again."
                        )
                    }
                    return@launch
                }
                placeStore.addPlace(cleanName, location.latitude, location.longitude)
            }.onSuccess {
                _uiState.update { it.copy(isAddingPlace = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isAddingPlace = false, placeError = "Failed to save place: ${err.message}") }
            }
        }
    }

    fun removePlace(id: String) = viewModelScope.launch {
        runCatching { placeStore.removePlace(id) }
            .onFailure { err -> _uiState.update { it.copy(placeError = "Failed to remove place: ${err.message}") } }
    }

    fun enableSync(email: String) {
        val cleanEmail = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.update { it.copy(error = "Please enter a valid email address.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            runCatching {
                syncRepository.ensureLoggedIn(cleanEmail)
                syncRepository.syncNow().getOrThrow()
            }.onSuccess {
                syncScheduler.ensureScheduled()
                val lastSync = syncRepository.lastSyncEpochMillis()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncEnabled = true,
                        syncEmail = cleanEmail,
                        formattedLastSync = lastSync.takeIf { ts -> ts > 0L }?.let(DateFormatting::formatDateTime),
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Couldn't reach the sync service: ${err.message}"
                    )
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }
            runCatching {
                syncRepository.syncNow().getOrThrow()
            }.onSuccess {
                val lastSync = syncRepository.lastSyncEpochMillis()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        formattedLastSync = lastSync.takeIf { ts -> ts > 0L }?.let(DateFormatting::formatDateTime),
                        error = null
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${err.message}"
                    )
                }
            }
        }
    }

    fun disconnectSync() {
        viewModelScope.launch {
            runCatching { syncRepository.disconnect() }
            _uiState.update {
                it.copy(
                    syncEnabled = false,
                    syncEmail = null,
                    formattedLastSync = null,
                    error = null
                )
            }
        }
    }
}
