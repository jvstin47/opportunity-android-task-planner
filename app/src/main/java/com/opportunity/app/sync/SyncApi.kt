package com.opportunity.app.sync

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class SyncOpportunityDto(
    val clientId: String,
    val title: String,
    val description: String = "",
    val type: String,
    val status: String,
    val priorityBucket: String? = null,
    val userPriority: Int? = null,
    val importance: Int = 0,
    val urgency: Int = 0,
    val deadlineEpochMillis: Long? = null,
    val preferredTimeMinuteOfDay: Int? = null,
    val estimatedDurationMinutes: Int? = null,
    val actualDurationMinutes: Int = 0,
    val timerMode: String = "COUNTDOWN",
    val sessionPlanJson: String? = null,
    val reminderJson: String? = null,
    val recurrenceJson: String? = null,
    val parentProjectId: String? = null,
    val goalId: String? = null,
    val category: String? = null,
    val locationHint: String? = null,
    val completionPercent: Int = 0,
    val postponementCount: Int = 0,
    val lastPostponedEpochMillis: Long? = null,
    val sortOrderInParent: Int = 0,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
    val deletedLocally: Boolean = false
)

@Serializable
data class PushRequest(val opportunities: List<SyncOpportunityDto>)

@Serializable
data class PushResponse(val synced: Map<String, String>)

@Serializable
data class PullResponse(val serverTimeEpochMillis: Long, val opportunities: List<SyncOpportunityDto>)

@Serializable
data class DevLoginRequest(val email: String)

@Serializable
data class DevLoginResponse(val token: String, val userId: String)

interface SyncApi {
    @POST("auth/dev-login")
    suspend fun devLogin(@Body request: DevLoginRequest): DevLoginResponse

    @POST("sync/push")
    suspend fun push(@Header("Authorization") bearer: String, @Body request: PushRequest): PushResponse

    @GET("sync/pull")
    suspend fun pull(@Header("Authorization") bearer: String, @Query("since") since: Long): PullResponse
}
