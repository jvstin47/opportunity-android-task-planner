package com.opportunity.app.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 8: rather than scheduling one exact alarm per Opportunity (which gets unwieldy once
 * reminder cadence becomes adaptive), a periodic sweep checks all reminder-enabled Opportunities
 * every 15 minutes (WorkManager's minimum periodic interval) and fires any that are due. This is
 * the right tradeoff for "remind me every evening"-style cadences; sub-15-minute precision isn't
 * needed for a personal task reminder.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<ReminderSweepWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME = "reminder_sweep"
    }
}
