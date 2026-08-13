package com.opportunity.app.data.local

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.ReminderConfig
import com.opportunity.app.domain.model.RecurrenceRule
import com.opportunity.app.domain.model.SessionPlan
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun OpportunityEntity.reminder(): ReminderConfig =
    reminderJson?.let { runCatching { json.decodeFromString<ReminderConfig>(it) }.getOrNull() } ?: ReminderConfig()

fun OpportunityEntity.withReminder(config: ReminderConfig): OpportunityEntity =
    copy(reminderJson = json.encodeToString(config))

fun OpportunityEntity.recurrence(): RecurrenceRule =
    recurrenceJson?.let { runCatching { json.decodeFromString<RecurrenceRule>(it) }.getOrNull() } ?: RecurrenceRule()

fun OpportunityEntity.withRecurrence(rule: RecurrenceRule): OpportunityEntity =
    copy(recurrenceJson = json.encodeToString(rule))

fun OpportunityEntity.sessionPlan(): SessionPlan? =
    sessionPlanJson?.let { runCatching { json.decodeFromString<SessionPlan>(it) }.getOrNull() }

fun OpportunityEntity.withSessionPlan(plan: SessionPlan): OpportunityEntity =
    copy(sessionPlanJson = json.encodeToString(plan))
