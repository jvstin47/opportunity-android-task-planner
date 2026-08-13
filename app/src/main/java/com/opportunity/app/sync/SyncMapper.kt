package com.opportunity.app.sync

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.TimerMode

fun OpportunityEntity.toDto(): SyncOpportunityDto = SyncOpportunityDto(
    clientId = id,
    title = title,
    description = description,
    type = type.name,
    status = status.name,
    priorityBucket = priorityBucket?.name,
    userPriority = userPriority,
    importance = importance,
    urgency = urgency,
    deadlineEpochMillis = deadlineEpochMillis,
    preferredTimeMinuteOfDay = preferredTimeMinuteOfDay,
    estimatedDurationMinutes = estimatedDurationMinutes,
    actualDurationMinutes = actualDurationMinutes,
    timerMode = timerMode.name,
    sessionPlanJson = sessionPlanJson,
    reminderJson = reminderJson,
    recurrenceJson = recurrenceJson,
    parentProjectId = parentProjectId,
    goalId = goalId,
    category = category,
    locationHint = locationHint,
    completionPercent = completionPercent,
    postponementCount = postponementCount,
    lastPostponedEpochMillis = lastPostponedEpochMillis,
    sortOrderInParent = sortOrderInParent,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
    deletedLocally = deletedLocally
)

fun SyncOpportunityDto.toEntity(): OpportunityEntity = OpportunityEntity(
    id = clientId,
    title = title,
    description = description,
    type = runCatching { OpportunityType.valueOf(type) }.getOrDefault(OpportunityType.TASK),
    status = runCatching { OpportunityStatus.valueOf(status) }.getOrDefault(OpportunityStatus.INBOX),
    priorityBucket = priorityBucket?.let { runCatching { PriorityBucket.valueOf(it) }.getOrNull() },
    userPriority = userPriority,
    importance = importance,
    urgency = urgency,
    deadlineEpochMillis = deadlineEpochMillis,
    preferredTimeMinuteOfDay = preferredTimeMinuteOfDay,
    estimatedDurationMinutes = estimatedDurationMinutes,
    actualDurationMinutes = actualDurationMinutes,
    timerMode = runCatching { TimerMode.valueOf(timerMode) }.getOrDefault(TimerMode.COUNTDOWN),
    sessionPlanJson = sessionPlanJson,
    reminderJson = reminderJson,
    recurrenceJson = recurrenceJson,
    parentProjectId = parentProjectId,
    goalId = goalId,
    category = category,
    locationHint = locationHint,
    completionPercent = completionPercent,
    postponementCount = postponementCount,
    lastPostponedEpochMillis = lastPostponedEpochMillis,
    sortOrderInParent = sortOrderInParent,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    completedAtEpochMillis = completedAtEpochMillis,
    isDirty = false,
    deletedLocally = deletedLocally
)
