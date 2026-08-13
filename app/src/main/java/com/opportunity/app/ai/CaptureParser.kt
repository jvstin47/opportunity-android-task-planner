package com.opportunity.app.ai

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ParsedOpportunity(
    val title: String,
    val description: String = "",
    val type: String = "TASK", // OpportunityType name
    val priorityBucket: String? = null, // PriorityBucket name
    val deadlineIsoDate: String? = null, // yyyy-MM-dd, local
    val estimatedDurationMinutes: Int? = null,
    val reminderFrequencyMinutes: Int? = null,
    val reminderUntilCompleted: Boolean = false,
    val recurrenceFrequency: String? = null, // RecurrenceFrequency name
    val confidence: Double = 0.5
)

/**
 * Section 3.2 / 16: turns a free-text capture like
 * "Finish my DS assignment before Wednesday, probably takes an hour, remind me every evening
 * until I finish it" into structured fields the user can review/edit before saving.
 */
@Singleton
class CaptureParser @Inject constructor(
    private val claude: ClaudeClient
) {
    suspend fun parse(rawInput: String, now: LocalDate = LocalDate.now(ZoneId.systemDefault())): AiResult<ParsedOpportunity> {
        val system = """
            You convert a short, informally-written personal task capture into structured JSON matching this schema:
            {
              "title": string (concise, action-oriented),
              "description": string (empty if nothing extra),
              "type": one of TASK, PROJECT, GOAL, HABIT, IDEA, SOMEDAY,
              "priorityBucket": one of DO_SOON, SHOULD_DO, GROW, EXPLORE, PERSONAL, SOMEDAY, or null,
              "deadlineIsoDate": "yyyy-MM-dd" or null if no deadline stated,
              "estimatedDurationMinutes": integer minutes or null,
              "reminderFrequencyMinutes": integer minutes between reminders, or null,
              "reminderUntilCompleted": boolean, true if the user said things like "until I finish it",
              "recurrenceFrequency": one of NONE, DAILY, WEEKLY, CUSTOM_INTERVAL_DAYS, EVENING_UNTIL_COMPLETED, or null,
              "confidence": 0.0-1.0 how confident you are in this extraction
            }
            Today's date is $now. Resolve relative dates ("Wednesday", "tomorrow", "next week") against it.
        """.trimIndent()

        return when (val result = claude.completeJson(system, rawInput)) {
            is AiResult.Success -> runCatching { claude.parse<ParsedOpportunity>(result.value) }
                .fold(
                    onSuccess = { AiResult.Success(it) },
                    onFailure = { AiResult.Failure("Could not parse model output: ${it.message}") }
                )
            is AiResult.Failure -> result
        }
    }
}
