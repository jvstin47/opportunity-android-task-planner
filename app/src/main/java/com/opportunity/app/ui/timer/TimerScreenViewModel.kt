package com.opportunity.app.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.timer.TimerController
import com.opportunity.app.timer.TimerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerScreenViewModel @Inject constructor(
    private val timerController: TimerController,
    private val repository: OpportunityRepository
) : ViewModel() {

    val timerState: StateFlow<TimerUiState?> = timerController.state

    private val _opportunityTitle = MutableStateFlow("")
    val opportunityTitle: StateFlow<String> = _opportunityTitle.asStateFlow()

    fun beginIfNeeded(opportunityId: String) {
        if (timerController.state.value?.opportunityId == opportunityId) return
        viewModelScope.launch {
            val opportunity = repository.getById(opportunityId) ?: return@launch
            _opportunityTitle.value = opportunity.title
            timerController.start(
                opportunityId = opportunity.id,
                opportunityTitle = opportunity.title,
                mode = opportunity.timerMode,
                targetMinutes = opportunity.estimatedDurationMinutes
            )
        }
    }

    fun pause() = timerController.pause()
    fun resume() = timerController.resume()
    fun finish() = timerController.stopAndLog()
}
