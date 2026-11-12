package com.opportunity.app.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
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

/** Home screen widget: visual daily goal and focus progress tracker. */
class ProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val progress = WidgetDataProvider.getDailyProgress(context)

        provideContent {
            val ctx = LocalContext.current
            val percentage = (progress.progressFraction * 100).toInt()

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
                        "TODAY'S PROGRESS",
                        style = TextStyle(
                            color = WidgetColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        "$percentage%",
                        style = TextStyle(
                            color = WidgetColors.Text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = progress.progressFraction,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .cornerRadius(3.dp),
                    color = WidgetColors.Primary,
                    backgroundColor = WidgetColors.SurfaceHigh
                )

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Stats row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            "${progress.completedCount} of ${progress.plannedCount}",
                            style = TextStyle(
                                color = WidgetColors.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            "tasks completed",
                            style = TextStyle(
                                color = WidgetColors.TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.Horizontal.End) {
                        Text(
                            if (progress.focusedMinutes > 0) "${progress.focusedMinutes}m" else "0m",
                            style = TextStyle(
                                color = WidgetColors.Warning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            "time focused",
                            style = TextStyle(
                                color = WidgetColors.TextMuted,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
