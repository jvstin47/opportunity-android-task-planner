package com.opportunity.wear

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class RightNowWearUiState(
    val opportunityId: String? = null,
    val title: String? = null,
    val estimatedMinutes: Int? = null
)

@Serializable
private data class WearRightNow(val id: String? = null, val title: String? = null, val estimatedMinutes: Int? = null)

/**
 * Section 26 Wear OS support: asks the paired phone for the current Right Now recommendation
 * over the Wearable MessageClient (see PhoneWearableListenerService on the app side) rather
 * than maintaining its own database -- a watch companion doesn't need offline capture, only a
 * quick glance at "what's next" and the ability to mark it done.
 */
class RightNowWearViewModel(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(RightNowWearUiState())
    val uiState: StateFlow<RightNowWearUiState> = _uiState.asStateFlow()

    private val messageListener = com.google.android.gms.wearable.MessageClient.OnMessageReceivedListener { event: MessageEvent ->
        if (event.path == "/right_now_response") {
            runCatching { json.decodeFromString<WearRightNow>(String(event.data)) }.getOrNull()?.let {
                _uiState.value = RightNowWearUiState(it.id, it.title, it.estimatedMinutes)
            }
        }
    }

    init {
        Wearable.getMessageClient(context).addListener(messageListener)
    }

    fun refresh() {
        scope.launch {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, "/request_right_now", ByteArray(0))
            }
        }
    }

    fun complete() {
        val id = _uiState.value.opportunityId ?: return
        scope.launch {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, "/complete_opportunity", id.toByteArray())
            }
        }
        _uiState.value = RightNowWearUiState()
    }
}
