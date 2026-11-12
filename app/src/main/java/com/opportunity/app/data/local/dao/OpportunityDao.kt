package com.opportunity.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OpportunityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<OpportunityEntity>)

    @Update
    suspend fun update(entity: OpportunityEntity)

    @Query("SELECT * FROM opportunities WHERE id = :id")
    suspend fun getById(id: String): OpportunityEntity?

    @Query("SELECT * FROM opportunities WHERE id = :id")
    fun observeById(id: String): Flow<OpportunityEntity?>

    @Query("SELECT * FROM opportunities WHERE status = :status AND deletedLocally = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeByStatus(status: OpportunityStatus): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE status = 'INBOX' AND deletedLocally = 0 ORDER BY createdAtEpochMillis DESC")
    fun observeInbox(): Flow<List<OpportunityEntity>>

    @Query(
        """SELECT * FROM opportunities
           WHERE deletedLocally = 0
             AND status IN ('ACTIVE', 'IN_PROGRESS')
             AND deadlineEpochMillis IS NOT NULL
             AND deadlineEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis
           ORDER BY deadlineEpochMillis ASC"""
    )
    fun observeDueBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<OpportunityEntity>>

    @Query(
        """SELECT * FROM opportunities
           WHERE deletedLocally = 0 AND status IN ('ACTIVE', 'IN_PROGRESS')
           ORDER BY deadlineEpochMillis ASC"""
    )
    fun observeAllActionable(): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE type = :type AND deletedLocally = 0 ORDER BY updatedAtEpochMillis DESC")
    fun observeByType(type: OpportunityType): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE parentProjectId = :projectId AND deletedLocally = 0 ORDER BY sortOrderInParent ASC")
    fun observeChildren(projectId: String): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE status = 'COMPLETED' AND deletedLocally = 0 AND completedAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis ORDER BY completedAtEpochMillis DESC")
    suspend fun getCompletedBetween(startEpochMillis: Long, endEpochMillis: Long): List<OpportunityEntity>

    @Query("SELECT * FROM opportunities WHERE status = 'COMPLETED' AND deletedLocally = 0 AND completedAtEpochMillis BETWEEN :startEpochMillis AND :endEpochMillis ORDER BY completedAtEpochMillis DESC")
    fun observeCompletedBetween(startEpochMillis: Long, endEpochMillis: Long): Flow<List<OpportunityEntity>>

    @Query("SELECT * FROM opportunities WHERE postponementCount >= :threshold AND deletedLocally = 0 ORDER BY postponementCount DESC")
    fun observeFrequentlyPostponed(threshold: Int = 3): Flow<List<OpportunityEntity>>

    @Query("UPDATE opportunities SET postponementCount = postponementCount + 1, lastPostponedEpochMillis = :nowEpochMillis, isDirty = 1 WHERE id = :id")
    suspend fun incrementPostponement(id: String, nowEpochMillis: Long)

    @Query(
        """UPDATE opportunities SET status = 'COMPLETED', completionPercent = 100,
           completedAtEpochMillis = :nowEpochMillis, updatedAtEpochMillis = :nowEpochMillis, isDirty = 1
           WHERE id = :id"""
    )
    suspend fun markCompleted(id: String, nowEpochMillis: Long)

    @Query(
        """UPDATE opportunities SET status = 'ACTIVE', completionPercent = 0,
           completedAtEpochMillis = NULL, updatedAtEpochMillis = :nowEpochMillis, isDirty = 1
           WHERE id = :id"""
    )
    suspend fun markReopened(id: String, nowEpochMillis: Long)

    @Query("UPDATE opportunities SET deletedLocally = 1, isDirty = 1, updatedAtEpochMillis = :nowEpochMillis WHERE id = :id")
    suspend fun softDelete(id: String, nowEpochMillis: Long)

    @Query("SELECT * FROM opportunities WHERE isDirty = 1")
    suspend fun getDirty(): List<OpportunityEntity>

    @Query("UPDATE opportunities SET isDirty = 0, remoteId = :remoteId WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String)

    @Query("SELECT * FROM opportunities WHERE reminderJson IS NOT NULL AND deletedLocally = 0 AND status NOT IN ('COMPLETED', 'ARCHIVED')")
    suspend fun getAllWithReminders(): List<OpportunityEntity>

    @Query("SELECT COUNT(*) FROM opportunities WHERE status = 'INBOX' AND deletedLocally = 0")
    fun observeInboxCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM opportunities WHERE status = 'INBOX' AND deletedLocally = 0")
    suspend fun getInboxCount(): Int

    @Query(
        """SELECT * FROM opportunities
           WHERE deletedLocally = 0
             AND status IN ('ACTIVE', 'IN_PROGRESS')
             AND deadlineEpochMillis IS NOT NULL
           ORDER BY deadlineEpochMillis ASC
           LIMIT :limit"""
    )
    suspend fun getUpcomingDeadlines(limit: Int = 5): List<OpportunityEntity>
}
