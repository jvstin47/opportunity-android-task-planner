package com.opportunity.app.ai

import com.opportunity.app.data.local.entity.OpportunityEntity
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class PrioritySuggestion(
    val priorityBucket: String, // PriorityBucket name
    val importance: Int, // 0-100
    val urgency: Int, // 0-100
    val rationale: String
)

/**
 * Section 12/16: suggests a priority bucket + importance/urgency for an Opportunity the user
 * hasn't classified yet. This is a suggestion only -- the user can always override
 * (OpportunityEntity.userPriority takes precedence in the scoring engine, see domain/usecase).
 */
@Singleton
class PrioritizationAdvisor @Inject constructor(
    private val claude: ClaudeClient
) {
    suspend fun suggest(opportunity: OpportunityEntity): AiResult<PrioritySuggestion> {
        val system = """
            You triage a personal task/goal/idea into one of these buckets:
            DO_SOON (urgent responsibility), SHOULD_DO (important but flexible), GROW (long-term improvement),
            EXPLORE (idea/experiment), PERSONAL (personal responsibility), SOMEDAY (worth remembering, not urgent).
            Respond with JSON: { "priorityBucket": string, "importance": 0-100, "urgency": 0-100, "rationale": short string }.
        """.trimIndent()
        val user = buildString {
            append("Title: ${opportunity.title}\n")
            append("Type: ${opportunity.type}\n")
            if (opportunity.description.isNotBlank()) append("Description: ${opportunity.description}\n")
            opportunity.deadlineEpochMillis?.let { append("Has a deadline: yes\n") }
            append("Times postponed: ${opportunity.postponementCount}")
        }
        return when (val result = claude.completeJson(system, user, maxTokens = 400)) {
            is AiResult.Success -> runCatching { claude.parse<PrioritySuggestion>(result.value) }
                .fold(
                    onSuccess = { AiResult.Success(it) },
                    onFailure = { AiResult.Failure("Could not parse priority suggestion: ${it.message}") }
                )
            is AiResult.Failure -> result
        }
    }
}
