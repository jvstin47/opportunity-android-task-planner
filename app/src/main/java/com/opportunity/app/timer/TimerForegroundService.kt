package com.opportunity.app.timer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.opportunity.app.OpportunityApplication
import com.opportunity.app.R
import com.opportunity.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps active focus timers alive via a silent low-importance foreground notification.
 * Updates only when title, pause status, or whole-minute elapsed text changes.
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var timerController: TimerController
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Timer running", null))
        job?.cancel()
        val manager = getSystemService(NotificationManager::class.java)
        job = CoroutineScope(Dispatchers.Default).launch {
            // On a process restart (OS killed the app, e.g. OEM battery managers), the
            // controller's in-memory state starts null even though a timer was still running --
            // wait for it to check persisted state before treating null as "nothing to do."
            timerController.ensureRehydrated()
            timerController.state
                .distinctUntilChangedBy { state ->
                    if (state == null) null
                    else Triple(state.opportunityTitle, state.elapsedMinutes, state.runState)
                }
                .onEach { state ->
                    if (state == null || state.runState == TimerRunState.COMPLETED) {
                        stopSelf()
                    } else {
                        val minutes = state.elapsedMinutes
                        val base = state.targetMinutes?.let { "$minutes / $it min" } ?: "$minutes min elapsed"
                        val body = if (state.runState == TimerRunState.PAUSED) "$base · Paused" else base
                        manager?.notify(NOTIFICATION_ID, buildNotification(state.opportunityTitle, body))
                    }
                }
                .launchIn(this)
        }
        return START_STICKY
    }

    private fun buildNotification(title: String, body: String?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OpportunityApplication.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_tile_add)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.opportunity.app.timer.SHOW"
        private const val NOTIFICATION_ID = 42
    }
}
