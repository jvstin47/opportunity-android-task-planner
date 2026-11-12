package com.opportunity.app.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.RightNowUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class WearRightNow(val id: String?, val title: String?, val estimatedMinutes: Int?)

/**
 * Wear OS companion bridge over Google Play Services Wearable MessageClient.
 */
@AndroidEntryPoint
class PhoneWearableListenerService : WearableListenerService() {

    @Inject lateinit var rightNowUseCase: RightNowUseCase
    @Inject lateinit var repository: OpportunityRepository

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PATH_REQUEST_RIGHT_NOW -> scope.launch {
                runCatching {
                    val recommendation = rightNowUseCase.recommend(availableMinutes = 60)
                    val payload = WearRightNow(
                        id = recommendation.best?.id,
                        title = recommendation.best?.title,
                        estimatedMinutes = recommendation.best?.estimatedDurationMinutes
                    )
                    Wearable.getMessageClient(this@PhoneWearableListenerService)
                        .sendMessage(
                            event.sourceNodeId,
                            PATH_RIGHT_NOW_RESPONSE,
                            json.encodeToString(payload).toByteArray()
                        )
                        .await()
                }
            }
            PATH_COMPLETE_OPPORTUNITY -> scope.launch {
                val id = String(event.data).trim()
                if (id.isNotBlank()) {
                    runCatching { repository.complete(id) }
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val PATH_REQUEST_RIGHT_NOW = "/request_right_now"
        const val PATH_RIGHT_NOW_RESPONSE = "/right_now_response"
        const val PATH_COMPLETE_OPPORTUNITY = "/complete_opportunity"
    }
}
