package com.opportunity.app.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.opportunity.app.data.local.entity.SessionEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.TimerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
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
 *
 * Every state change is mirrored to TimerPersistence. This singleton's in-memory state does NOT
 * survive the process being killed (common on OEM battery managers, e.g. MIUI, even with a
 * foreground service running) -- without persistence, a restarted process comes back with
 * state=null, the foreground service sees that and immediately stops itself, and the timer just
 * vanishes. ensureRehydrated() lets the service check persisted state before deciding to give up.
 */
@Singleton
class TimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OpportunityRepository,
    private val persistence: TimerPersistence
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var baseElapsedRealtime: Long = 0L
    private var accumulatedMillisBeforePause: Long = 0L
    private val rehydrationDone = CompletableDeferred<Unit>()

    private val _state = MutableStateFlow<TimerUiState?>(null)
    val state: StateFlow<TimerUiState?> = _state

    init {
        scope.launch {
            rehydrate()
            rehydrationDone.complete(Unit)
        }
    }

    /** Awaited by the foreground service before it treats a null state as "nothing to show." */
    suspend fun ensureRehydrated() = rehydrationDone.await()

    private suspend fun rehydrate() {
        val saved = persistence.load() ?: return
        val now = SystemClock.elapsedRealtime()
        if (saved.baseElapsedRealtime > now) {
            // elapsedRealtime is since-boot; a saved value in the "future" means the device
            // rebooted since this was written, so the clock basis is no longer valid.
            persistence.clear()
            return
        }
        if (_state.value != null) return // a fresh start() already happened while we were loading

        baseElapsedRealtime = saved.baseElapsedRealtime
        accumulatedMillisBeforePause = saved.accumulatedMillisBeforePause
        val elapsed = if (saved.isPaused) {
            accumulatedMillisBeforePause
        } else {
            accumulatedMillisBeforePause + (now - baseElapsedRealtime)
        }
        _state.value = TimerUiState(
            opportunityId = saved.opportunityId,
            opportunityTitle = saved.opportunityTitle,
            mode = saved.mode,
            targetMinutes = saved.targetMinutes,
            elapsedMillis = elapsed,
            runState = if (saved.isPaused) TimerRunState.PAUSED else TimerRunState.RUNNING
        )
        if (!saved.isPaused) startTicking()
        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_SHOW)
        )
    }

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
        persist(opportunityId, opportunityTitle, mode, targetMinutes, isPaused = false)
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
        persist(current.opportunityId, current.opportunityTitle, current.mode, current.targetMinutes, isPaused = true)
    }

    fun resume() {
        val current = _state.value ?: return
        if (current.runState != TimerRunState.PAUSED) return
        baseElapsedRealtime = SystemClock.elapsedRealtime()
        _state.update { it?.copy(runState = TimerRunState.RUNNING) }
        persist(current.opportunityId, current.opportunityTitle, current.mode, current.targetMinutes, isPaused = false)
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
            persistence.clear()
        }
        _state.value = current.copy(runState = TimerRunState.COMPLETED)
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }

    fun discard() {
        tickJob?.cancel()
        _state.value = null
        scope.launch { persistence.clear() }
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }

    private fun persist(id: String, title: String, mode: TimerMode, targetMinutes: Int?, isPaused: Boolean) {
        scope.launch {
            persistence.save(
                PersistedTimer(
                    opportunityId = id,
                    opportunityTitle = title,
                    mode = mode,
                    targetMinutes = targetMinutes,
                    baseElapsedRealtime = baseElapsedRealtime,
                    accumulatedMillisBeforePause = accumulatedMillisBeforePause,
                    isPaused = isPaused
                )
            )
        }
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
