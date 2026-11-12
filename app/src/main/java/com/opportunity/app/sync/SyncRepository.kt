package com.opportunity.app.sync

import com.opportunity.app.data.local.dao.OpportunityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 26 "cross-device synchronization": pushes locally-dirty Opportunities to the backend
 * (see backend/README.md), then pulls anything newer from the server. Entirely optional -- the
 * app works fully offline against Room without this ever running (section 21).
 */
@Singleton
class SyncRepository @Inject constructor(
    private val api: SyncApi,
    private val tokenStore: AuthTokenStore,
    private val opportunityDao: OpportunityDao
) {
    suspend fun isConnected(): Boolean = tokenStore.token() != null

    suspend fun connectedEmail(): String? = tokenStore.email()

    suspend fun lastSyncEpochMillis(): Long = tokenStore.lastSyncEpochMillis()

    suspend fun disconnect() = tokenStore.clear()

    suspend fun ensureLoggedIn(email: String) {
        if (tokenStore.token() != null) return
        val response = api.devLogin(DevLoginRequest(email))
        tokenStore.saveToken(response.token)
        tokenStore.saveEmail(email)
    }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val token = tokenStore.token() ?: return@runCatching
            val bearer = "Bearer $token"

            val dirty = opportunityDao.getDirty()
            if (dirty.isNotEmpty()) {
                val response = api.push(bearer, PushRequest(dirty.map { it.toDto() }))
                response.synced.keys.forEach { clientId ->
                    opportunityDao.markSynced(clientId, response.synced.getValue(clientId))
                }
            }

            val since = tokenStore.lastSyncEpochMillis()
            val pullResponse = api.pull(bearer, since)
            val remoteEntities = pullResponse.opportunities.map { it.toEntity() }
            val merged = remoteEntities.mapNotNull { remote ->
                val local = opportunityDao.getById(remote.id)
                when {
                    local == null -> remote // new from server
                    local.isDirty -> null // local has unsaved changes, don't overwrite
                    remote.updatedAtEpochMillis > local.updatedAtEpochMillis -> remote // server is newer
                    else -> null // local is same or newer, skip
                }
            }
            if (merged.isNotEmpty()) {
                opportunityDao.upsertAll(merged)
            }
            tokenStore.saveLastSync(pullResponse.serverTimeEpochMillis)
        }
    }
}
