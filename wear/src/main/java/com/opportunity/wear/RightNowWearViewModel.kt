package com.opportunity.wear

import android.content.Context
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class RightNowWearUiState(
    val opportunityId: String? = null,
    val title: String? = null,
    val estimatedMinutes: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@Serializable
private data class WearRightNow(val id: String? = null, val title: String? = null, val estimatedMinutes: Int? = null)

/**
 * Wear OS state holder that requests the paired phone's current Right Now recommendation
 * over Wearable MessageClient and supports marking the item complete with graceful error handling.
 */
class RightNowWearViewModel(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(RightNowWearUiState())
    val uiState: StateFlow<RightNowWearUiState> = _uiState.asStateFlow()

    private val messageListener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
        if (event.path == "/right_now_response") {
            val decoded = runCatching { json.decodeFromString<WearRightNow>(String(event.data)) }.getOrNull()
            if (decoded != null) {
                _uiState.value = RightNowWearUiState(
                    opportunityId = decoded.id,
                    title = decoded.title,
                    estimatedMinutes = decoded.estimatedMinutes,
                    isLoading = false,
                    error = null
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid response from phone")
            }
        }
    }

    init {
        Wearable.getMessageClient(context).addListener(messageListener)
    }

    fun refresh() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                if (nodes.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Phone not connected")
                    return@launch
                }
                nodes.forEach { node ->
                    Wearable.getMessageClient(context).sendMessage(node.id, "/request_right_now", ByteArray(0)).await()
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to reach phone")
            }
        }
    }

    fun complete() {
        val id = _uiState.value.opportunityId ?: return
        scope.launch {
            runCatching {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                nodes.forEach { node ->
                    Wearable.getMessageClient(context).sendMessage(node.id, "/complete_opportunity", id.toByteArray()).await()
                }
            }.onSuccess {
                _uiState.value = RightNowWearUiState()
                refresh()
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = "Couldn't complete on phone")
            }
        }
    }

    fun dispose() {
        Wearable.getMessageClient(context).removeListener(messageListener)
        scope.cancel()
    }
}
