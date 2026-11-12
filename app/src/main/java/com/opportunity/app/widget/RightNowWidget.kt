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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.ui.theme.emoji

/** Home Screen Widget: "What should I do next?" surfaced without opening the app. */
class RightNowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val recommendation = WidgetDataProvider.getRightNow(context)
        val best = recommendation?.best

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
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        "WHAT TO DO NEXT",
                        style = TextStyle(
                            color = WidgetColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        "+ Add",
                        style = TextStyle(
                            color = WidgetColors.TextMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(WidgetIntents.openCapture(ctx))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (best != null) {
                    val bucketLabel = when (best.priorityBucket) {
                        PriorityBucket.DO_SOON -> "Urgent · "
                        PriorityBucket.SHOULD_DO -> "Planned · "
                        PriorityBucket.GROW -> "Grow · "
                        PriorityBucket.EXPLORE -> "Idea · "
                        PriorityBucket.PERSONAL -> "Personal · "
                        PriorityBucket.SOMEDAY -> "Someday · "
                        null -> ""
                    }
                    val tagText = "${best.priorityBucket.emoji()} $bucketLabel${best.category ?: "Task"}"

                    Text(
                        tagText,
                        style = TextStyle(
                            color = if (best.priorityBucket == PriorityBucket.DO_SOON) WidgetColors.Urgent else WidgetColors.Warning,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        best.title,
                        style = TextStyle(
                            color = WidgetColors.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 2
                    )

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        best.estimatedDurationMinutes?.let {
                            Text(
                                "⏱ ~$it min",
                                style = TextStyle(color = WidgetColors.TextMuted, fontSize = 12.sp)
                            )
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        Row(
                            modifier = GlanceModifier
                                .background(WidgetColors.PrimaryContainer)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .clickable(
                                    actionStartActivity(WidgetIntents.openOpportunity(ctx, best.id))
                                ),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                "Open →",
                                style = TextStyle(
                                    color = WidgetColors.OnPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                } else {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        "All caught up! 🎉",
                        style = TextStyle(
                            color = WidgetColors.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        "No urgent tasks matching your available window right now.",
                        style = TextStyle(color = WidgetColors.TextMuted, fontSize = 12.sp),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
