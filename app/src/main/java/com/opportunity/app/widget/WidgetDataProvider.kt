package com.opportunity.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import com.opportunity.app.data.local.dao.OpportunityDao
import com.opportunity.app.data.local.dao.SessionDao
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.repository.OpportunityRepository
import com.opportunity.app.domain.usecase.RightNowRecommendation
import com.opportunity.app.domain.usecase.RightNowUseCase
import com.opportunity.app.domain.usecase.TodayPlan
import com.opportunity.app.domain.usecase.TodayPlanUseCase
import com.opportunity.app.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object WidgetColors {
    val Background = ColorProvider(Color(0xFF17130E))
    val Surface = ColorProvider(Color(0xFF201B16))
    val SurfaceHigh = ColorProvider(Color(0xFF2F2924))
    val Primary = ColorProvider(Color(0xFFFFC665))
    val PrimaryContainer = ColorProvider(Color(0xFFE5A93C))
    val OnPrimary = ColorProvider(Color(0xFF432C00))
    val OnPrimaryContainer = ColorProvider(Color(0xFF5E4000))
    val Text = ColorProvider(Color(0xFFEBE1D8))
    val TextMuted = ColorProvider(Color(0xFFD4C4B0))
    val Urgent = ColorProvider(Color(0xFFFFB4AB))
    val Warning = ColorProvider(Color(0xFFFFB4A1))
    val Success = ColorProvider(Color(0xFFA8DAC0))
    val Outline = ColorProvider(Color(0xFF504535))
}

data class DailyProgressData(
    val completedCount: Int,
    val plannedCount: Int,
    val focusedMinutes: Int,
    val progressFraction: Float
)

data class StreakData(
    val streakDays: Int,
    val completedThisWeek: Int
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun rightNowUseCase(): RightNowUseCase
    fun todayPlanUseCase(): TodayPlanUseCase
    fun opportunityRepository(): OpportunityRepository
    fun opportunityDao(): OpportunityDao
    fun sessionDao(): SessionDao
}

object WidgetIntents {
    fun launchApp(context: Context): Intent =
        Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun openCapture(context: Context): Intent =
        launchApp(context).apply {
            putExtra(MainActivity.EXTRA_OPEN_CAPTURE, true)
        }

    fun openOpportunity(context: Context, opportunityId: String): Intent =
        launchApp(context).apply {
            data = Uri.parse("opportunity://opportunity/$opportunityId")
        }
}

object WidgetDataProvider {

    fun getEntryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

    suspend fun getRightNow(context: Context): RightNowRecommendation? = runCatching {
        getEntryPoint(context).rightNowUseCase().recommend(availableMinutes = 60)
    }.getOrNull()

    suspend fun getTodayPlan(context: Context): TodayPlan? = runCatching {
        getEntryPoint(context).todayPlanUseCase().buildPlan()
    }.getOrNull()

    suspend fun getInboxCount(context: Context): Int = runCatching {
        getEntryPoint(context).opportunityRepository().getInboxCount()
    }.getOrDefault(0)

    suspend fun getUpcomingDeadlines(context: Context, limit: Int = 4): List<OpportunityEntity> = runCatching {
        getEntryPoint(context).opportunityRepository().getUpcomingDeadlines(limit)
    }.getOrDefault(emptyList())

    suspend fun getDailyProgress(context: Context): DailyProgressData = runCatching {
        val entry = getEntryPoint(context)
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val completedToday = entry.opportunityDao().getCompletedBetween(todayStart, todayEnd)
        val plan = entry.todayPlanUseCase().buildPlan(zone)
        val plannedTasks = plan.mustHappen + plan.recommended + plan.ifYouHaveTime
        val totalPlanned = (plannedTasks.size + completedToday.size).coerceAtLeast(1)

        val sessionsToday = entry.sessionDao().getBetween(todayStart, todayEnd)
        val focusedMinutes = sessionsToday.sumOf { it.durationMinutes }

        val fraction = (completedToday.size.toFloat() / totalPlanned).coerceIn(0f, 1f)
        DailyProgressData(
            completedCount = completedToday.size,
            plannedCount = plannedTasks.size + completedToday.size,
            focusedMinutes = focusedMinutes,
            progressFraction = fraction
        )
    }.getOrDefault(DailyProgressData(0, 0, 0, 0f))

    suspend fun getStreakData(context: Context): StreakData = runCatching {
        val entry = getEntryPoint(context)
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS).toEpochMilli()
        val weekAgo = now.minus(7, ChronoUnit.DAYS).toEpochMilli()

        val completed30Days = entry.opportunityDao().getCompletedBetween(thirtyDaysAgo, now.toEpochMilli())
        val completedThisWeek = completed30Days.count { (it.completedAtEpochMillis ?: 0L) >= weekAgo }

        val completionDates = completed30Days.mapNotNull { it.completedAtEpochMillis }
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSet()

        val today = LocalDate.now(zone)
        var streak = 0
        var checkDate = if (completionDates.contains(today)) today else today.minusDays(1)

        while (completionDates.contains(checkDate)) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        StreakData(
            streakDays = streak,
            completedThisWeek = completedThisWeek
        )
    }.getOrDefault(StreakData(0, 0))
}
