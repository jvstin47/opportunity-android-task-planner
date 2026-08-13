package com.opportunity.app.ai

import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DecomposedSubtask(
    val title: String,
    val estimatedDurationMinutes: Int? = null,
    val order: Int
)

@Serializable
data class DecompositionResult(val subtasks: List<DecomposedSubtask>)

/**
 * Section 16 "Task decomposition": turns a Project-type Opportunity like
 * "Build my portfolio" into an ordered list of child tasks.
 */
@Singleton
class TaskDecomposer @Inject constructor(
    private val claude: ClaudeClient
) {
    suspend fun decompose(projectTitle: String, projectDescription: String = ""): AiResult<DecompositionResult> {
        val system = """
            You break a personal project into a realistic, ordered checklist of concrete subtasks.
            Respond with JSON: { "subtasks": [ { "title": string, "estimatedDurationMinutes": integer or null, "order": integer starting at 0 } ] }
            Keep it to 4-10 subtasks. Titles should be concrete actions, not vague phases.
        """.trimIndent()
        val user = buildString {
            append("Project: $projectTitle")
            if (projectDescription.isNotBlank()) append("\nDetails: $projectDescription")
        }
        return when (val result = claude.completeJson(system, user, maxTokens = 1200)) {
            is AiResult.Success -> runCatching { claude.parse<DecompositionResult>(result.value) }
                .fold(
                    onSuccess = { AiResult.Success(it) },
                    onFailure = { AiResult.Failure("Could not parse decomposition: ${it.message}") }
                )
            is AiResult.Failure -> result
        }
    }
}
