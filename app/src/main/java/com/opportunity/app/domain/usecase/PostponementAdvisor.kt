package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import javax.inject.Inject
import javax.inject.Singleton

enum class PostponementAction { NONE, GENTLE_CHECK_IN, OFFER_CHOICES }

/**
 * Section 11 "Postponement Intelligence". Postponement count crosses thresholds where the app
 * should stop silently rescheduling and instead surface a choice: keep it, schedule it properly,
 * turn it into a recurring goal, or archive it. Deliberately simple/threshold-based (no ML) --
 * this is the "meaningful data" hook the spec describes, not a prediction model.
 */
@Singleton
class PostponementAdvisor @Inject constructor() {

    data class Advice(
        val action: PostponementAction,
        val message: String? = null,
        val offerArchive: Boolean = false,
        val offerRecurring: Boolean = false
    )

    fun evaluate(opportunity: OpportunityEntity): Advice = when {
        opportunity.postponementCount in 1..2 -> Advice(
            action = PostponementAction.NONE
        )
        opportunity.postponementCount in 3..5 -> Advice(
            action = PostponementAction.GENTLE_CHECK_IN,
            message = "You've postponed \"${opportunity.title}\" ${opportunity.postponementCount} times. Move it to tomorrow?"
        )
        opportunity.postponementCount > 5 -> Advice(
            action = PostponementAction.OFFER_CHOICES,
            message = "You've postponed \"${opportunity.title}\" ${opportunity.postponementCount} times. " +
                "Want to keep it, schedule it properly, turn it into a recurring goal, or archive it?",
            offerArchive = true,
            offerRecurring = true
        )
        else -> Advice(PostponementAction.NONE)
    }
}
