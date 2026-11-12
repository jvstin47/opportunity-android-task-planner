package com.opportunity.app.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opportunity.app.data.local.recurrence
import com.opportunity.app.data.local.reminder
import com.opportunity.app.data.local.withReminder
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.RecurrenceFrequency
import com.opportunity.app.domain.usecase.AdaptiveReminderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.time.Instant

@HiltWorker
class ReminderSweepWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: OpportunityRepository,
    private val adaptiveReminder: AdaptiveReminderUseCase,
    private val notifier: ReminderNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val candidates = repository.getAllWithReminders()

            for (opportunity in candidates) {
                val reminderConfig = opportunity.reminder()
                if (!reminderConfig.enabled) continue
                val nextDue = reminderConfig.nextReminderEpochMillis ?: now
                if (nextDue > now) continue

                if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val content = adaptiveReminder.buildReminder(opportunity, Instant.ofEpochMilli(now))
                    notifier.notify(opportunity.id, content)
                }

                val recurrence = opportunity.recurrence()
                val stillRecurring = recurrence.frequency != RecurrenceFrequency.NONE && !recurrence.stopOnCompletion
                val nextInterval = reminderConfig.frequencyMinutes.takeIf { it > 0 } ?: (24 * 60)
                val updated = if (reminderConfig.frequencyMinutes > 0 || stillRecurring) {
                    opportunity.withReminder(
                        reminderConfig.copy(nextReminderEpochMillis = now + nextInterval * 60_000L)
                    )
                } else {
                    opportunity.withReminder(reminderConfig.copy(enabled = false))
                }
                repository.save(updated)
            }
            Result.success()
        } catch (c: CancellationException) {
            throw c
        } catch (_: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
