package com.opportunity.app.ui.timeline

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.util.DateFormatting

/** Timeline screen: renders [TimelineUiState] from [TimelineViewModel]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onOpenOpportunity: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Timeline",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Calendar events & scheduled tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            !state.hasCalendarPermission -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("See your real day", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Give Opportunity calendar access so scheduling respects your actual free time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Allow calendar access")
                    }
                }
            }
            state.isLoading && state.blocks.isEmpty() -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.error != null && state.blocks.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    title = "Couldn't load timeline",
                    body = state.error ?: "Please try again."
                )
            }
            state.blocks.isEmpty() -> {
                EmptyState(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    title = "Your day is open",
                    body = "No calendar events or time-blocked tasks for today yet."
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.blocks) { block ->
                        val startTime = DateFormatting.formatTime(block.startEpochMillis)
                        val endTime = DateFormatting.formatTime(block.endEpochMillis)
                        val presentation = when (block) {
                            is TimelineBlock.Busy -> TimelinePresentation(
                                label = block.title,
                                subtitle = "Calendar · $startTime – $endTime",
                                accent = MaterialTheme.colorScheme.outline,
                                opportunityId = null
                            )
                            is TimelineBlock.Planned -> TimelinePresentation(
                                label = block.opportunity.title,
                                subtitle = "Planned · $startTime – $endTime",
                                accent = MaterialTheme.colorScheme.primary,
                                opportunityId = block.opportunity.id
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                startTime,
                                modifier = Modifier.width(76.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            GlassCard(
                                cornerRadius = 18.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .let { base ->
                                        val id = presentation.opportunityId
                                        if (id != null) base.clickable { onOpenOpportunity(id) } else base
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(28.dp)
                                            .background(presentation.accent, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            presentation.label,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            presentation.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TimelinePresentation(
    val label: String,
    val subtitle: String,
    val accent: Color,
    val opportunityId: String?
)
