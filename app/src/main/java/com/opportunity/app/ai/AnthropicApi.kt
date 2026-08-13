package com.opportunity.app.ai

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

@Serializable
data class ClaudeMessage(val role: String, val content: String)

@Serializable
data class ClaudeMessagesRequest(
    val model: String,
    val max_tokens: Int = 1024,
    val system: String? = null,
    val messages: List<ClaudeMessage>,
    val temperature: Double = 0.2
)

@Serializable
data class ClaudeContentBlock(val type: String, val text: String? = null)

@Serializable
data class ClaudeMessagesResponse(
    val id: String? = null,
    val model: String? = null,
    val role: String? = null,
    val content: List<ClaudeContentBlock> = emptyList(),
    val stop_reason: String? = null
) {
    fun text(): String = content.firstOrNull { it.type == "text" }?.text.orEmpty()
}

interface AnthropicApi {
    @Headers("content-type: application/json")
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeMessagesRequest
    ): ClaudeMessagesResponse
}
