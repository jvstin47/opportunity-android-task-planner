package com.opportunity.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triggers non-blocking updates across all 6 Glance home screen widgets whenever local
 * task or session data mutates, so widgets never stay stale between periodic alarms.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshAll() {
        scope.launch {
            runCatching { RightNowWidget().updateAll(context) }
            runCatching { TaskListWidget().updateAll(context) }
            runCatching { QuickAddWidget().updateAll(context) }
            runCatching { ProgressWidget().updateAll(context) }
            runCatching { StreakWidget().updateAll(context) }
            runCatching { UpcomingWidget().updateAll(context) }
        }
    }
}
