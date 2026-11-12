package com.opportunity.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.opportunity.app.data.local.reminder
import com.opportunity.app.data.local.withReminder
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles interactive notification actions (Complete, Snooze, Start, Reschedule). */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: OpportunityRepository

    override fun onReceive(context: Context, intent: Intent) {
        val opportunityId = intent.getStringExtra(EXTRA_OPPORTUNITY_ID) ?: return
        NotificationManagerCompat.from(context).cancel(opportunityId.hashCode())

        when (intent.action) {
            ACTION_COMPLETE -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        runCatching { repository.complete(opportunityId) }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        runCatching {
                            repository.postpone(opportunityId)
                            val opp = repository.getById(opportunityId) ?: return@runCatching
                            val reminder = opp.reminder()
                            repository.save(
                                opp.withReminder(
                                    reminder.copy(nextReminderEpochMillis = System.currentTimeMillis() + 60 * 60_000L)
                                )
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_RESCHEDULE -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = android.net.Uri.parse("opportunity://opportunity/$opportunityId?edit=deadline")
                }
                runCatching { context.startActivity(launch) }
            }
            ACTION_START -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = android.net.Uri.parse("opportunity://opportunity/$opportunityId?start=true")
                }
                runCatching { context.startActivity(launch) }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.opportunity.app.action.START"
        const val ACTION_SNOOZE = "com.opportunity.app.action.SNOOZE"
        const val ACTION_RESCHEDULE = "com.opportunity.app.action.RESCHEDULE"
        const val ACTION_COMPLETE = "com.opportunity.app.action.COMPLETE"
        const val EXTRA_OPPORTUNITY_ID = "opportunity_id"
    }
}
