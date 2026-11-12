package com.opportunity.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "sessions",
    foreignKeys = [ForeignKey(
        entity = OpportunityEntity::class,
        parentColumns = ["id"],
        childColumns = ["opportunityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("opportunityId")]
)
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val opportunityId: String,
    val startedAtEpochMillis: Long,
    val durationMinutes: Int,
    val mode: String, // TimerMode name
    val note: String? = null
)
