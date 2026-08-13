package com.opportunity.app.timer

import android.app.Notification
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var timerController: TimerController
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Timer running", null))
        job?.cancel()
        job = timerController.state.onEach { state ->
            if (state == null || state.runState == TimerRunState.COMPLETED) {
                stopSelf()
            } else {
                val minutes = state.elapsedMinutes
                val body = state.targetMinutes?.let { "$minutes / $it min" } ?: "$minutes min elapsed"
                startForeground(NOTIFICATION_ID, buildNotification(state.opportunityTitle, body))
            }
        }.launchIn(CoroutineScope(Dispatchers.Default))
        return START_STICKY
    }

    private fun buildNotification(title: String, body: String?): Notification {
        val contentIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OpportunityApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_tile_add)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
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
