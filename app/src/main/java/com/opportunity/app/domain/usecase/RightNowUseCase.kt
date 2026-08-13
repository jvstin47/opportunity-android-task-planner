package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.model.OpportunityStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class RightNowRecommendation(
    val best: OpportunityEntity?,
    val alternatives: List<OpportunityEntity>
)

/**
 * Section 9 "What Can I Do Right Now?". Ranks actionable opportunities by OpportunityScorer
 * against the user's stated available time and returns a single best match plus a short list
 * of alternatives -- deliberately not the full sorted backlog (section 9: "not... hundreds of
 * tasks").
 */
@Singleton
class RightNowUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val scorer: OpportunityScorer
) {
    suspend fun recommend(availableMinutes: Int, maxAlternatives: Int = 3): RightNowRecommendation {
        val candidates = repository.observeAllActionable().first()
            .filter { it.status == OpportunityStatus.ACTIVE || it.status == OpportunityStatus.IN_PROGRESS }

        val ranked = candidates
            .map { it to scorer.score(it, availableMinutes = availableMinutes) }
            .sortedByDescending { it.second }
            .map { it.first }

        val fitting = ranked.filter { fits(it, availableMinutes) }
        val best = fitting.firstOrNull()
        val alternatives = (if (best != null) fitting.drop(1) else ranked)
            .take(maxAlternatives)

        return RightNowRecommendation(best, alternatives)
    }

    private fun fits(opportunity: OpportunityEntity, availableMinutes: Int): Boolean {
        val estimate = opportunity.estimatedDurationMinutes ?: return true // unknown duration: don't exclude
        return estimate <= availableMinutes
    }
}
