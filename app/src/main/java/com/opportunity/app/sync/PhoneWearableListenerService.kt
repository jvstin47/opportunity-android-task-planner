package com.opportunity.app.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.RightNowUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class WearRightNow(val id: String?, val title: String?, val estimatedMinutes: Int?)

/**
 * Section 26 "Wear OS support": the watch has no local database of its own -- it asks the phone
 * for the current Right Now recommendation over the Wearable MessageClient, and can send back a
 * "complete" action. Request/response is simpler and more robust for this use case than trying
 * to keep a full DataLayer replica in sync.
 */
@AndroidEntryPoint
class PhoneWearableListenerService : WearableListenerService() {

    @Inject lateinit var rightNowUseCase: RightNowUseCase
    @Inject lateinit var repository: OpportunityRepository

    private val json = Json { encodeDefaults = true }
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            PATH_REQUEST_RIGHT_NOW -> scope.launch {
                val recommendation = rightNowUseCase.recommend(availableMinutes = 60)
                val payload = WearRightNow(
                    id = recommendation.best?.id,
                    title = recommendation.best?.title,
                    estimatedMinutes = recommendation.best?.estimatedDurationMinutes
                )
                Wearable.getMessageClient(this@PhoneWearableListenerService)
                    .sendMessage(event.sourceNodeId, PATH_RIGHT_NOW_RESPONSE, json.encodeToString(payload).toByteArray())
            }
            PATH_COMPLETE_OPPORTUNITY -> scope.launch {
                repository.complete(String(event.data))
            }
        }
    }

    companion object {
        const val PATH_REQUEST_RIGHT_NOW = "/request_right_now"
        const val PATH_RIGHT_NOW_RESPONSE = "/right_now_response"
        const val PATH_COMPLETE_OPPORTUNITY = "/complete_opportunity"
    }
}
