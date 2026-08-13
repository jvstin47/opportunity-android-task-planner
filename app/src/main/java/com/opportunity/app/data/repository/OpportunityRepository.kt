package com.opportunity.app.data.repository

import com.opportunity.app.data.local.dao.OpportunityDao
import com.opportunity.app.data.local.dao.SessionDao
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.entity.SessionEntity
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpportunityRepository @Inject constructor(
    private val opportunityDao: OpportunityDao,
    private val sessionDao: SessionDao
) {
    fun observeInbox(): Flow<List<OpportunityEntity>> = opportunityDao.observeInbox()

    fun observeInboxCount(): Flow<Int> = opportunityDao.observeInboxCount()

    fun observeByStatus(status: OpportunityStatus): Flow<List<OpportunityEntity>> =
        opportunityDao.observeByStatus(status)

    fun observeAllActionable(): Flow<List<OpportunityEntity>> = opportunityDao.observeAllActionable()

    fun observeDueBetween(startEpochMillis: Long, endEpochMillis: Long) =
        opportunityDao.observeDueBetween(startEpochMillis, endEpochMillis)

    fun observeByType(type: OpportunityType): Flow<List<OpportunityEntity>> =
        opportunityDao.observeByType(type)

    fun observeChildren(projectId: String): Flow<List<OpportunityEntity>> =
        opportunityDao.observeChildren(projectId)

    fun observeFrequentlyPostponed(threshold: Int = 3) =
        opportunityDao.observeFrequentlyPostponed(threshold)

    fun observeById(id: String): Flow<OpportunityEntity?> = opportunityDao.observeById(id)

    suspend fun getById(id: String): OpportunityEntity? = opportunityDao.getById(id)

    suspend fun save(entity: OpportunityEntity) {
        opportunityDao.upsert(entity.copy(updatedAtEpochMillis = System.currentTimeMillis(), isDirty = true))
    }

    suspend fun saveAll(entities: List<OpportunityEntity>) {
        val now = System.currentTimeMillis()
        opportunityDao.upsertAll(entities.map { it.copy(updatedAtEpochMillis = now, isDirty = true) })
    }

    suspend fun complete(id: String) = opportunityDao.markCompleted(id, System.currentTimeMillis())

    suspend fun postpone(id: String) = opportunityDao.incrementPostponement(id, System.currentTimeMillis())

    suspend fun delete(id: String) = opportunityDao.softDelete(id, System.currentTimeMillis())

    suspend fun getAllWithReminders(): List<OpportunityEntity> = opportunityDao.getAllWithReminders()

    suspend fun getCompletedBetween(startEpochMillis: Long, endEpochMillis: Long) =
        opportunityDao.getCompletedBetween(startEpochMillis, endEpochMillis)

    suspend fun getDirty(): List<OpportunityEntity> = opportunityDao.getDirty()

    suspend fun markSynced(id: String, remoteId: String) = opportunityDao.markSynced(id, remoteId)

    fun observeSessions(opportunityId: String): Flow<List<SessionEntity>> =
        sessionDao.observeForOpportunity(opportunityId)

    suspend fun logSession(session: SessionEntity) {
        sessionDao.insert(session)
        val opp = opportunityDao.getById(session.opportunityId) ?: return
        val totalMinutes = sessionDao.totalMinutesFor(session.opportunityId)
        opportunityDao.update(
            opp.copy(
                actualDurationMinutes = totalMinutes,
                updatedAtEpochMillis = System.currentTimeMillis(),
                isDirty = true
            )
        )
    }

    suspend fun sessionsBetween(startEpochMillis: Long, endEpochMillis: Long) =
        sessionDao.getBetween(startEpochMillis, endEpochMillis)
}
