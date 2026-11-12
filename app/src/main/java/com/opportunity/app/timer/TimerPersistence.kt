package com.opportunity.app.timer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.opportunity.app.domain.model.TimerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "opportunity_timer")

data class PersistedTimer(
    val opportunityId: String,
    val opportunityTitle: String,
    val mode: TimerMode,
    val targetMinutes: Int?,
    val baseElapsedRealtime: Long,
    val accumulatedMillisBeforePause: Long,
    val isPaused: Boolean
)

/**
 * Survives the process being killed and restarted while a timer is running -- e.g. MIUI/other
 * OEM battery managers killing the app in the background despite the foreground service. Without
 * this, TimerController (a Hilt singleton) comes back up with a blank in-memory state on restart,
 * the foreground service sees null and immediately stops itself, and the user's timer silently
 * vanishes with no way to recover the elapsed time.
 */
@Singleton
class TimerPersistence @Inject constructor(@ApplicationContext private val context: Context) {

    private val opportunityIdKey = stringPreferencesKey("opportunity_id")
    private val opportunityTitleKey = stringPreferencesKey("opportunity_title")
    private val modeKey = stringPreferencesKey("mode")
    private val targetMinutesKey = intPreferencesKey("target_minutes")
    private val baseElapsedRealtimeKey = longPreferencesKey("base_elapsed_realtime")
    private val accumulatedKey = longPreferencesKey("accumulated_millis")
    private val pausedKey = booleanPreferencesKey("is_paused")

    suspend fun save(state: PersistedTimer) {
        context.timerDataStore.edit { prefs ->
            prefs[opportunityIdKey] = state.opportunityId
            prefs[opportunityTitleKey] = state.opportunityTitle
            prefs[modeKey] = state.mode.name
            if (state.targetMinutes != null) prefs[targetMinutesKey] = state.targetMinutes else prefs.remove(targetMinutesKey)
            prefs[baseElapsedRealtimeKey] = state.baseElapsedRealtime
            prefs[accumulatedKey] = state.accumulatedMillisBeforePause
            prefs[pausedKey] = state.isPaused
        }
    }

    suspend fun load(): PersistedTimer? {
        val prefs = context.timerDataStore.data.first()
        val id = prefs[opportunityIdKey] ?: return null
        val title = prefs[opportunityTitleKey] ?: return null
        val mode = prefs[modeKey]?.let { runCatching { TimerMode.valueOf(it) }.getOrNull() } ?: return null
        val base = prefs[baseElapsedRealtimeKey] ?: return null
        return PersistedTimer(
            opportunityId = id,
            opportunityTitle = title,
            mode = mode,
            targetMinutes = prefs[targetMinutesKey],
            baseElapsedRealtime = base,
            accumulatedMillisBeforePause = prefs[accumulatedKey] ?: 0L,
            isPaused = prefs[pausedKey] ?: false
        )
    }

    suspend fun clear() {
        context.timerDataStore.edit { it.clear() }
    }
}
