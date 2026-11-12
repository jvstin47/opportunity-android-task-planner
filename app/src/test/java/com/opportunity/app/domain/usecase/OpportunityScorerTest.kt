package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OpportunityScorerTest {

    private val scorer = OpportunityScorer()

    @Test
    fun `zero duration does not produce NaN or Infinity`() {
        val opp = OpportunityEntity(
            title = "Zero duration task",
            estimatedDurationMinutes = 0,
            urgency = 50,
            importance = 50
        )

        val score = scorer.score(opp, availableMinutes = 30, now = Instant.now())
        assertFalse("Score must not be NaN", score.isNaN())
        assertFalse("Score must not be Infinite", score.isInfinite())
        assertTrue("Score must be a valid positive number", score > 0)
    }

    @Test
    fun `null duration handled cleanly`() {
        val opp = OpportunityEntity(
            title = "Unknown duration task",
            estimatedDurationMinutes = null,
            urgency = 80,
            importance = 70
        )

        val score = scorer.score(opp, availableMinutes = 60, now = Instant.now())
        assertFalse(score.isNaN())
        assertTrue(score > 0)
    }

    @Test
    fun `overdue task gets high deadline proximity score`() {
        val now = Instant.now()
        val pastDeadline = now.minusSeconds(3600).toEpochMilli()

        val oppOverdue = OpportunityEntity(
            title = "Past deadline",
            deadlineEpochMillis = pastDeadline,
            urgency = 50,
            importance = 50
        )

        val oppFuture = OpportunityEntity(
            title = "Future deadline",
            deadlineEpochMillis = now.plusSeconds(86400 * 5).toEpochMilli(),
            urgency = 50,
            importance = 50
        )

        val scoreOverdue = scorer.score(oppOverdue, availableMinutes = 60, now = now)
        val scoreFuture = scorer.score(oppFuture, availableMinutes = 60, now = now)

        assertTrue("Overdue tasks must score higher than tasks due next week", scoreOverdue > scoreFuture)
    }
}
