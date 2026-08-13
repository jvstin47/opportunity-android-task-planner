package com.opportunity.app.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.opportunity.app.OpportunityApplication
import com.opportunity.app.R
import com.opportunity.app.domain.model.EscalationLevel
import com.opportunity.app.domain.usecase.ReminderContent
import com.opportunity.app.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun notify(opportunityId: String, content: ReminderContent) {
        val channel = if (content.level == EscalationLevel.URGENT) {
            OpportunityApplication.CHANNEL_URGENT
        } else {
            OpportunityApplication.CHANNEL_REMINDERS
        }

        val contentIntent = PendingIntent.getActivity(
            context, opportunityId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                data = android.net.Uri.parse("opportunity://opportunity/$opportunityId")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_tile_add)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(
                if (content.level == EscalationLevel.URGENT) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_start), actionIntent(opportunityId, NotificationActionReceiver.ACTION_START))
            .addAction(0, context.getString(R.string.action_snooze), actionIntent(opportunityId, NotificationActionReceiver.ACTION_SNOOZE))
            .addAction(0, context.getString(R.string.action_reschedule), actionIntent(opportunityId, NotificationActionReceiver.ACTION_RESCHEDULE))
            .build()

        NotificationManagerCompat.from(context).notify(opportunityId.hashCode(), notification)
    }

    private fun actionIntent(opportunityId: String, action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(NotificationActionReceiver.EXTRA_OPPORTUNITY_ID, opportunityId)
        }
        return PendingIntent.getBroadcast(
            context,
            (opportunityId + action).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
