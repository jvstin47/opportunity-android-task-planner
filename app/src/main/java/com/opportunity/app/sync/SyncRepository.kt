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
    suspend fun ensureLoggedIn(email: String) {
        if (tokenStore.token() != null) return
        val response = api.devLogin(DevLoginRequest(email))
        tokenStore.saveToken(response.token)
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
            opportunityDao.upsertAll(pullResponse.opportunities.map { it.toEntity() })
            tokenStore.saveLastSync(pullResponse.serverTimeEpochMillis)
        }
    }
}
