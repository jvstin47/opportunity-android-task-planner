package com.opportunity.app.ai

import com.opportunity.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class AiResult<out T> {
    data class Success<T>(val value: T) : AiResult<T>()
    data class Failure(val reason: String, val cause: Throwable? = null) : AiResult<Nothing>()
}

/**
 * Thin wrapper over the Anthropic Messages API used as Opportunity's "intelligence layer"
 * (section 16): NLP capture parsing, task decomposition, prioritization, and reflection.
 *
 * Requires ANTHROPIC_API_KEY in local.properties (see local.properties.example) or the
 * ANTHROPIC_API_KEY env var at build time -- never hardcode a key here.
 */
@Singleton
class ClaudeClient @Inject constructor(
    private val api: AnthropicApi
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val model = "claude-sonnet-5"

    val isConfigured: Boolean get() = BuildConfig.ANTHROPIC_API_KEY.isNotBlank()

    suspend fun completeJson(systemPrompt: String, userPrompt: String, maxTokens: Int = 1024): AiResult<String> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext AiResult.Failure(
                    "ANTHROPIC_API_KEY is not set. Add it to local.properties to enable AI features."
                )
            }
            try {
                val response = api.createMessage(
                    apiKey = BuildConfig.ANTHROPIC_API_KEY,
                    request = ClaudeMessagesRequest(
                        model = model,
                        max_tokens = maxTokens,
                        system = "$systemPrompt\nRespond with ONLY valid JSON, no markdown fences, no commentary.",
                        messages = listOf(ClaudeMessage(role = "user", content = userPrompt))
                    )
                )
                val text = response.text().trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                if (text.isBlank()) AiResult.Failure("Empty response from model") else AiResult.Success(text)
            } catch (t: Throwable) {
                AiResult.Failure("Claude API call failed: ${t.message}", t)
            }
        }

    suspend fun completeText(systemPrompt: String, userPrompt: String, maxTokens: Int = 512): AiResult<String> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) {
                return@withContext AiResult.Failure(
                    "ANTHROPIC_API_KEY is not set. Add it to local.properties to enable AI features."
                )
            }
            try {
                val response = api.createMessage(
                    apiKey = BuildConfig.ANTHROPIC_API_KEY,
                    request = ClaudeMessagesRequest(
                        model = model,
                        max_tokens = maxTokens,
                        system = systemPrompt,
                        messages = listOf(ClaudeMessage(role = "user", content = userPrompt))
                    )
                )
                AiResult.Success(response.text().trim())
            } catch (t: Throwable) {
                AiResult.Failure("Claude API call failed: ${t.message}", t)
            }
        }

    internal inline fun <reified T> parse(jsonText: String): T = json.decodeFromString(jsonText)
}
