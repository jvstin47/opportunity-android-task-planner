package com.opportunity.app.domain.usecase

import com.opportunity.app.ai.AiResult
import com.opportunity.app.ai.CaptureParser
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.withReminder
import com.opportunity.app.data.local.withRecurrence
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.RecurrenceFrequency
import com.opportunity.app.domain.model.RecurrenceRule
import com.opportunity.app.domain.model.ReminderConfig
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 3.1/21: capture must never block on the network. We save a raw Opportunity into the
 * Inbox immediately (offline-first), then enrich it with AI-parsed structure in the background
 * if a Claude API key is configured. The user always sees and can edit the final result --
 * this only pre-fills fields, per section 3.2 ("user can edit the generated information
 * afterward").
 */
@Singleton
class CaptureOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val captureParser: CaptureParser
) {
    suspend fun captureRaw(
        rawInput: String,
        dueToday: Boolean = false,
        highPriority: Boolean = false
    ): OpportunityEntity {
        val draft = OpportunityEntity(
            title = rawInput,
            status = if (dueToday || highPriority) OpportunityStatus.ACTIVE else OpportunityStatus.INBOX,
            deadlineEpochMillis = if (dueToday) endOfTodayEpochMillis() else null,
            priorityBucket = if (highPriority) PriorityBucket.DO_SOON else null,
            userPriority = if (highPriority) 1 else null
        )
        repository.save(draft)
        return draft
    }

    private fun endOfTodayEpochMillis(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * AI enrichment fills gaps, it never clobbers a value the user already set explicitly (e.g.
     * via the Quick Capture due-today/high-priority toggles) -- otherwise a capture where AI
     * found no deadline would silently erase the user's own "due today" choice.
     */
    suspend fun enrichWithAi(opportunityId: String, rawInput: String): AiResult<OpportunityEntity> {
        return when (val result = captureParser.parse(rawInput, LocalDate.now(ZoneId.systemDefault()))) {
            is AiResult.Success -> {
                val existing = repository.getById(opportunityId) ?: return AiResult.Failure("Opportunity no longer exists")
                val parsed = result.value
                val deadlineMillis = parsed.deadlineIsoDate?.let {
                    runCatching {
                        LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
                }
                var updated = existing.copy(
                    title = parsed.title.ifBlank { existing.title },
                    description = parsed.description,
                    type = runCatching { OpportunityType.valueOf(parsed.type) }.getOrDefault(existing.type),
                    priorityBucket = existing.priorityBucket
                        ?: parsed.priorityBucket?.let { runCatching { PriorityBucket.valueOf(it) }.getOrNull() },
                    deadlineEpochMillis = existing.deadlineEpochMillis ?: deadlineMillis,
                    estimatedDurationMinutes = existing.estimatedDurationMinutes ?: parsed.estimatedDurationMinutes,
                    status = OpportunityStatus.ACTIVE
                )
                if (parsed.reminderFrequencyMinutes != null || parsed.reminderUntilCompleted) {
                    updated = updated.withReminder(
                        ReminderConfig(
                            enabled = true,
                            frequencyMinutes = parsed.reminderFrequencyMinutes ?: (24 * 60),
                            nextReminderEpochMillis = System.currentTimeMillis()
                        )
                    )
                }
                parsed.recurrenceFrequency?.let { freq ->
                    runCatching { RecurrenceFrequency.valueOf(freq) }.getOrNull()?.let {
                        updated = updated.withRecurrence(RecurrenceRule(frequency = it))
                    }
                }
                repository.save(updated)
                AiResult.Success(updated)
            }
            is AiResult.Failure -> result
        }
    }
}
