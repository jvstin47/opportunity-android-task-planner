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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.util.DateFormatting
import com.opportunity.app.util.DeadlineUrgency

/** Home screen widget: chronological upcoming deadlines with urgency coloring. */
class UpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deadlines = WidgetDataProvider.getUpcomingDeadlines(context, limit = 5)

        provideContent {
            val ctx = LocalContext.current

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.Background)
                    .cornerRadius(16.dp)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "UPCOMING DEADLINES",
                        style = TextStyle(
                            color = WidgetColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        "${deadlines.size} upcoming",
                        style = TextStyle(color = WidgetColors.TextMuted, fontSize = 11.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (deadlines.isEmpty()) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionStartActivity(WidgetIntents.launchApp(ctx))),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Text(
                            "No upcoming deadlines 🎉",
                            style = TextStyle(
                                color = WidgetColors.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            "Relax or capture a new goal",
                            style = TextStyle(color = WidgetColors.TextMuted, fontSize = 12.sp)
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(deadlines, itemId = { it.id.hashCode().toLong() }) { task ->
                            UpcomingTaskRow(task = task)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun UpcomingTaskRow(task: OpportunityEntity) {
    val context = LocalContext.current
    val deadlineMillis = task.deadlineEpochMillis ?: return
    val (badgeText, urgency) = DateFormatting.formatDeadlineBadge(deadlineMillis)
    val badgeColor = when (urgency) {
        DeadlineUrgency.OVERDUE -> WidgetColors.Urgent
        DeadlineUrgency.TODAY, DeadlineUrgency.TOMORROW -> WidgetColors.Warning
        DeadlineUrgency.UPCOMING -> WidgetColors.TextMuted
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(WidgetColors.Surface)
            .cornerRadius(8.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clickable(actionStartActivity(WidgetIntents.openOpportunity(context, task.id))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            badgeText,
            style = TextStyle(color = badgeColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            task.title,
            style = TextStyle(color = WidgetColors.Text, fontWeight = FontWeight.Medium, fontSize = 13.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}
