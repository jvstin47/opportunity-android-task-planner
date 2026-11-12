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
import com.opportunity.app.domain.model.PriorityBucket

/** Home screen widget: today's most important pending tasks, at a glance, with quick actions. */
class TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val plan = WidgetDataProvider.getTodayPlan(context)
        val mustHappen = plan?.mustHappen.orEmpty()
        val recommended = plan?.recommended.orEmpty()
        val topTasks = (mustHappen + recommended).take(6)

        provideContent {
            val ctx = LocalContext.current

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetColors.Background)
                    .cornerRadius(16.dp)
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "TODAY'S TASKS",
                        style = TextStyle(
                            color = WidgetColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    if (topTasks.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            "(${topTasks.size})",
                            style = TextStyle(
                                color = WidgetColors.TextMuted,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Quick Add button
                    Row(
                        modifier = GlanceModifier
                            .background(WidgetColors.SurfaceHigh)
                            .cornerRadius(6.dp)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clickable(actionStartActivity(WidgetIntents.openCapture(ctx))),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            "+ Add",
                            style = TextStyle(
                                color = WidgetColors.Text,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (topTasks.isEmpty()) {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .clickable(actionStartActivity(WidgetIntents.openCapture(ctx))),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                    ) {
                        Text(
                            "All clear for today! ☀️",
                            style = TextStyle(
                                color = WidgetColors.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            "Tap to add a new task",
                            style = TextStyle(
                                color = WidgetColors.TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                        items(topTasks, itemId = { it.id.hashCode().toLong() }) { task ->
                            TaskRow(task = task)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TaskRow(task: OpportunityEntity) {
    val context = LocalContext.current
    val bulletColor = when (task.priorityBucket) {
        PriorityBucket.DO_SOON -> WidgetColors.Urgent
        PriorityBucket.SHOULD_DO -> WidgetColors.Warning
        else -> WidgetColors.TextMuted
    }
    val bulletSymbol = when (task.priorityBucket) {
        PriorityBucket.DO_SOON -> "●"
        PriorityBucket.SHOULD_DO -> "▲"
        else -> "○"
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
            bulletSymbol,
            style = TextStyle(
                color = bulletColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            task.title,
            style = TextStyle(
                color = WidgetColors.Text,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        task.estimatedDurationMinutes?.let { minutes ->
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                "${minutes}m",
                style = TextStyle(
                    color = WidgetColors.TextMuted,
                    fontSize = 11.sp
                )
            )
        }
    }
}
