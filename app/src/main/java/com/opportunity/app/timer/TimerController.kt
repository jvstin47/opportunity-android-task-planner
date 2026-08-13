package com.opportunity.app.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.opportunity.app.data.local.entity.SessionEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.TimerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 7 "Timers": supports countdown, stopwatch, session-based and total-progress modes.
 * Runs a bound-less foreground service purely to keep a persistent notification (and the
 * process alive) while a timer is active -- state itself lives here as a StateFlow so both the
 * Compose UI and the service's notification renderer read the same source of truth.
 */
@Singleton
class TimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OpportunityRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var baseElapsedRealtime: Long = 0L
    private var accumulatedMillisBeforePause: Long = 0L

    private val _state = MutableStateFlow<TimerUiState?>(null)
    val state: StateFlow<TimerUiState?> = _state

    fun start(opportunityId: String, opportunityTitle: String, mode: TimerMode, targetMinutes: Int?) {
        baseElapsedRealtime = SystemClock.elapsedRealtime()
        accumulatedMillisBeforePause = 0L
        _state.value = TimerUiState(
            opportunityId = opportunityId,
            opportunityTitle = opportunityTitle,
            mode = mode,
            targetMinutes = targetMinutes,
            elapsedMillis = 0L,
            runState = TimerRunState.RUNNING
        )
        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_SHOW)
        )
        startTicking()
    }

    fun pause() {
        val current = _state.value ?: return
        if (current.runState != TimerRunState.RUNNING) return
        accumulatedMillisBeforePause = current.elapsedMillis
        tickJob?.cancel()
        _state.update { it?.copy(runState = TimerRunState.PAUSED) }
    }

    fun resume() {
        val current = _state.value ?: return
        if (current.runState != TimerRunState.PAUSED) return
        baseElapsedRealtime = SystemClock.elapsedRealtime()
        _state.update { it?.copy(runState = TimerRunState.RUNNING) }
        startTicking()
    }

    fun stopAndLog(note: String? = null) {
        val current = _state.value ?: return
        tickJob?.cancel()
        scope.launch {
            repository.logSession(
                SessionEntity(
                    opportunityId = current.opportunityId,
                    startedAtEpochMillis = System.currentTimeMillis() - current.elapsedMillis,
                    durationMinutes = current.elapsedMinutes.coerceAtLeast(1),
                    mode = current.mode.name,
                    note = note
                )
            )
        }
        _state.value = current.copy(runState = TimerRunState.COMPLETED)
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }

    fun discard() {
        tickJob?.cancel()
        _state.value = null
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                val elapsed = accumulatedMillisBeforePause + (SystemClock.elapsedRealtime() - baseElapsedRealtime)
                _state.update { it?.copy(elapsedMillis = elapsed) }
                delay(1000)
            }
        }
    }
}
