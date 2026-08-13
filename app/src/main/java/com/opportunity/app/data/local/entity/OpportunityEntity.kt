package com.opportunity.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.ReminderConfig
import com.opportunity.app.domain.model.RecurrenceRule
import com.opportunity.app.domain.model.SessionPlan
import com.opportunity.app.domain.model.TimerMode
import java.util.UUID

/**
 * Section 22 data model. Entity doubles as the domain model for this app's size --
 * a separate mapping layer would be pure overhead here.
 */
@Entity(tableName = "opportunities")
data class OpportunityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: OpportunityType = OpportunityType.TASK,
    val status: OpportunityStatus = OpportunityStatus.INBOX,

    val priorityBucket: PriorityBucket? = null,
    val userPriority: Int? = null, // 1 (highest) - 5, user override
    val importance: Int = 0, // 0-100, AI or user assigned
    val urgency: Int = 0, // 0-100

    val deadlineEpochMillis: Long? = null,
    val preferredTimeMinuteOfDay: Int? = null,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int = 0,

    val timerMode: TimerMode = TimerMode.COUNTDOWN,
    val sessionPlanJson: String? = null, // SessionPlan, for SESSION_BASED / TOTAL_PROGRESS

    val reminderJson: String? = null, // ReminderConfig
    val recurrenceJson: String? = null, // RecurrenceRule

    val parentProjectId: String? = null, // self-referential: Project = Opportunity with children
    val goalId: String? = null,
    val category: String? = null,
    val locationHint: String? = null,

    val completionPercent: Int = 0,
    val postponementCount: Int = 0,
    val lastPostponedEpochMillis: Long? = null,

    val sortOrderInParent: Int = 0,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val completedAtEpochMillis: Long? = null,

    // Sync bookkeeping (V3 cross-device sync, task #15)
    val isDirty: Boolean = true,
    val remoteId: String? = null,
    val deletedLocally: Boolean = false
)
