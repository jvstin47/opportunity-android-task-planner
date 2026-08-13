package com.opportunity.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.opportunity.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE opportunityId = :opportunityId ORDER BY startedAtEpochMillis DESC")
    fun observeForOpportunity(opportunityId: String): Flow<List<SessionEntity>>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM sessions WHERE opportunityId = :opportunityId")
    suspend fun totalMinutesFor(opportunityId: String): Int

    @Query("SELECT * FROM sessions WHERE startedAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis")
    suspend fun getBetween(startEpochMillis: Long, endEpochMillis: Long): List<SessionEntity>
}
