package com.opportunity.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.timer.TimerController
import com.opportunity.app.timer.TimerRunState
import com.opportunity.app.timer.TimerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(
    repository: OpportunityRepository,
    private val timerController: TimerController
) : ViewModel() {

    val inboxCount: StateFlow<Int> = repository.observeInboxCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeTimer: StateFlow<TimerUiState?> = timerController.state

    /** Toggles pause or resume for the currently active focus timer. */
    fun toggleTimerPauseResume() {
        val state = timerController.state.value ?: return
        if (state.runState == TimerRunState.PAUSED) timerController.resume() else timerController.pause()
    }
}
