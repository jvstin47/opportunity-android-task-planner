package com.opportunity.app.capture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opportunity.app.di.ApplicationScope
import com.opportunity.app.domain.usecase.CaptureOpportunityUseCase
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.theme.OpportunityTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Section 13 "Share Sheet" + "Text Selection": lets the user share a link/text/image, or
 * long-press-select text anywhere on the device, and add it to Opportunity's Inbox without
 * ever opening the full app.
 */
@AndroidEntryPoint
class QuickCaptureActivity : ComponentActivity() {

    @Inject lateinit var captureUseCase: CaptureOpportunityUseCase
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = extractSharedText()

        setContent {
            OpportunityTheme {
                var saved by remember { mutableStateOf(false) }
                var displayTitle by remember { mutableStateOf(rawText.take(80)) }

                LaunchedEffect(rawText) {
                    val draft = captureUseCase.captureRaw(rawText)
                    displayTitle = draft.title.take(80)
                    saved = true
                    applicationScope.launch { captureUseCase.enrichWithAi(draft.id, rawText) }
                    delay(1200)
                    finish()
                }

                Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        GlassCard(cornerRadius = 20.dp) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (saved) "Added to Opportunity Inbox" else "Adding…",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                if (saved) {
                                    Text(displayTitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractSharedText(): String {
        return when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { "Shared image: $it" }
                    ?: "Shared item"
            }
            Intent.ACTION_PROCESS_TEXT -> intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT) ?: "Selected text"
            else -> "New Opportunity"
        }
    }
}
