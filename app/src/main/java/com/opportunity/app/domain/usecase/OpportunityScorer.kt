package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Section 23 "Core Engine": a concrete, tunable implementation of
 *   Score = Urgency + Importance + DeadlineProximity + TimeFit + Preference
 *           + HistoricalBehavior + Context - PostponementCost - RequiredEffort
 *
 * All sub-scores are normalized to 0-100 before weighting so the weights below are the single
 * place to tune behavior. This is intentionally simple (linear, hand-tuned weights) rather than
 * a learned model -- see PROJECT_PLAN.md V3 notes for where a real personalization model would
 * eventually replace this.
 */
@Singleton
class OpportunityScorer @Inject constructor() {

    data class Weights(
        val urgency: Double = 1.0,
        val importance: Double = 1.0,
        val deadlineProximity: Double = 1.2,
        val timeFit: Double = 0.8,
        val userPreference: Double = 0.6,
        val postponementPenalty: Double = 0.5,
        val effortPenalty: Double = 0.3
    )

    fun score(
        opportunity: OpportunityEntity,
        availableMinutes: Int? = null,
        now: Instant = Instant.now(),
        weights: Weights = Weights()
    ): Double {
        val urgency = opportunity.urgency.toDouble()
        val importance = opportunity.importance.toDouble()

        val deadlineProximity = opportunity.deadlineEpochMillis?.let { deadline ->
            val hoursRemaining = Duration.between(now, Instant.ofEpochMilli(deadline)).toHours()
            when {
                hoursRemaining <= 0 -> 100.0
                hoursRemaining < 24 -> 90.0
                hoursRemaining < 72 -> 65.0
                hoursRemaining < 24 * 7 -> 35.0
                else -> 10.0
            }
        } ?: 0.0

        val timeFit = if (availableMinutes != null && opportunity.estimatedDurationMinutes != null && opportunity.estimatedDurationMinutes > 0) {
            val est = opportunity.estimatedDurationMinutes
            when {
                est > availableMinutes -> 0.0 // doesn't fit at all
                est <= availableMinutes * 0.5 -> 70.0 // comfortably fits, leaves room
                else -> 100.0 // fits snugly, uses the window well
            }
        } else 50.0 // unknown duration: neutral

        // userPriority is 1 (highest) .. 5; map to 100..20
        val userPreference = opportunity.userPriority?.let { max(0, 120 - it * 20).toDouble() } ?: 50.0

        val postponementCost = min(opportunity.postponementCount * 8, 60).toDouble()
        val effort = (opportunity.estimatedDurationMinutes ?: 30).coerceIn(0, 480) / 4.8 // 0-100 scale

        return weights.urgency * urgency +
            weights.importance * importance +
            weights.deadlineProximity * deadlineProximity +
            weights.timeFit * timeFit +
            weights.userPreference * userPreference -
            weights.postponementPenalty * postponementCost -
            weights.effortPenalty * effort
    }
}
