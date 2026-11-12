package com.opportunity.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Home screen widget: streak counter and consistency tracker. */
class StreakWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val streak = WidgetDataProvider.getStreakData(context)

        provideContent {
            val ctx = LocalContext.current

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.Background)
                    .cornerRadius(16.dp)
                    .padding(14.dp)
                    .clickable(actionStartActivity(WidgetIntents.launchApp(ctx))),
                verticalAlignment = Alignment.Vertical.Top
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "MOMENTUM",
                        style = TextStyle(
                            color = WidgetColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        "This week: ${streak.completedThisWeek}",
                        style = TextStyle(
                            color = WidgetColors.TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Streak Banner
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(WidgetColors.Surface)
                        .cornerRadius(10.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "🔥",
                        style = TextStyle(fontSize = 22.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    Column {
                        Text(
                            if (streak.streakDays == 1) "1 DAY" else "${streak.streakDays} DAYS",
                            style = TextStyle(
                                color = WidgetColors.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            "Consistency Streak",
                            style = TextStyle(
                                color = WidgetColors.TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Motivational footer
                val motivation = when {
                    streak.streakDays == 0 -> "Complete a task today to ignite your streak! ✨"
                    streak.streakDays in 1..2 -> "Great start! Keep the fire burning 🔥"
                    streak.streakDays in 3..6 -> "Solid rhythm! Momentum is building ⚡"
                    else -> "Unstoppable streak! Keep crushing it 🚀"
                }

                Text(
                    motivation,
                    style = TextStyle(
                        color = WidgetColors.Warning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
