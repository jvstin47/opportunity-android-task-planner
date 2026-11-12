package com.opportunity.app.domain.usecase

import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.EscalationLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AdaptiveReminderUseCaseTest {

    private val useCase = AdaptiveReminderUseCase()

    @Test
    fun `overdue task receives urgent escalation and overdue message`() {
        val now = Instant.now()
        val overdueTask = OpportunityEntity(
            title = "File taxes",
            deadlineEpochMillis = now.minusSeconds(7200).toEpochMilli(),
            estimatedDurationMinutes = 45
        )

        val reminder = useCase.buildReminder(overdueTask, now)

        assertEquals(EscalationLevel.URGENT, reminder.level)
        assertTrue("Body should mention overdue", reminder.body.contains("overdue", ignoreCase = true))
    }

    @Test
    fun `task due today receives urgent escalation`() {
        val now = Instant.now()
        val dueTodayTask = OpportunityEntity(
            title = "Team meeting slides",
            deadlineEpochMillis = now.plusSeconds(3600 * 4).toEpochMilli(),
            estimatedDurationMinutes = 30
        )

        val reminder = useCase.buildReminder(dueTodayTask, now)

        assertEquals(EscalationLevel.URGENT, reminder.level)
        assertTrue("Body should mention today", reminder.body.contains("today", ignoreCase = true))
    }

    @Test
    fun `unpostponed task without immediate deadline receives gentle reminder`() {
        val now = Instant.now()
        val task = OpportunityEntity(
            title = "Read chapter 4",
            postponementCount = 0
        )

        val reminder = useCase.buildReminder(task, now)

        assertEquals(EscalationLevel.GENTLE, reminder.level)
    }

    @Test
    fun `frequently postponed task receives concerned reminder`() {
        val now = Instant.now()
        val task = OpportunityEntity(
            title = "Doctor appointment",
            postponementCount = 4
        )

        val reminder = useCase.buildReminder(task, now)

        assertEquals(EscalationLevel.CONCERNED, reminder.level)
        assertTrue(reminder.body.contains("4 times"))
    }
}
