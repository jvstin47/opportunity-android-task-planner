package com.opportunity.app.ai

import javax.inject.Inject
import javax.inject.Singleton

data class WeeklyStats(
    val completedCount: Int,
    val postponedCount: Int,
    val focusedMinutes: Int,
    val mostProductiveHourRange: String?,
    val mostPostponedCategory: String?
)

/**
 * Section 16 "Reflection" / section 17.6 Insights: turns raw weekly stats into a short,
 * non-judgmental narrative ("You've completed most of your short tasks... but haven't made
 * progress on long-term projects") -- see section 18, no guilt/shame framing.
 */
@Singleton
class ReflectionGenerator @Inject constructor(
    private val claude: ClaudeClient
) {
    suspend fun generate(stats: WeeklyStats): AiResult<String> {
        val system = """
            You write a single short paragraph (2-4 sentences) reflecting on someone's personal
            productivity week, based on the stats given. Be observational and encouraging, never
            guilt-inducing or judgmental (no "you failed to", no streak-shaming). Point out one
            genuinely useful pattern and end with a gentle, optional suggestion, not a directive.
        """.trimIndent()
        val user = buildString {
            append("Completed: ${stats.completedCount} items\n")
            append("Postponed: ${stats.postponedCount} times\n")
            append("Focused work: ${stats.focusedMinutes} minutes\n")
            stats.mostProductiveHourRange?.let { append("Most productive window: $it\n") }
            stats.mostPostponedCategory?.let { append("Most postponed category: $it\n") }
        }
        return claude.completeText(system, user, maxTokens = 220)
    }
}
