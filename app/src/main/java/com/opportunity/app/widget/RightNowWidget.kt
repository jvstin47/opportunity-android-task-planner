package com.opportunity.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opportunity.app.domain.usecase.RightNowUseCase
import com.opportunity.app.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RightNowWidgetEntryPoint {
    fun rightNowUseCase(): RightNowUseCase
}

/** Section 13 "Home Screen Widget": "What should I do next?" surfaced without opening the app. */
class RightNowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, RightNowWidgetEntryPoint::class.java)
        val recommendation = runCatching { entryPoint.rightNowUseCase().recommend(availableMinutes = 60) }.getOrNull()

            val backgroundColor = ColorProvider(Color(0xFF0F131D))
            val textColor = ColorProvider(Color(0xFFDFE2F1))

            provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                Text(
                    "WHAT SHOULD I DO NEXT?",
                    style = TextStyle(color = textColor, fontWeight = FontWeight.Medium)
                )
                val best = recommendation?.best
                if (best != null) {
                    Text(
                        best.title,
                        style = TextStyle(color = textColor, fontWeight = FontWeight.Bold)
                    )
                    best.estimatedDurationMinutes?.let {
                        Text("~$it minutes", style = TextStyle(color = textColor))
                    }
                } else {
                    Text("Nothing urgent right now", style = TextStyle(color = textColor))
                }
            }
        }
    }
}
