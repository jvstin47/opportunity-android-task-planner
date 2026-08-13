package com.opportunity.app.ui.timeline

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.ui.common.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

/** Section 14/17.5 "Timeline": calendar events and planned Opportunities in one view. */
@Composable
fun TimelineScreen(
    onOpenOpportunity: (String) -> Unit,
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val blocks by viewModel.blocks.collectAsState()
    val hasCalendarPermission by viewModel.hasCalendarPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Timeline") }) }) { padding ->
        if (!hasCalendarPermission) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("See your real day", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Give Opportunity calendar access so scheduling respects your actual free time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                    Text("Allow calendar access")
                }
            }
            return@Scaffold
        }
        if (blocks.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                title = "Your day is open",
                body = "Nothing scheduled yet."
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(blocks) { block ->
                val zone = ZoneId.systemDefault()
                val time = timeFormatter.format(Instant.ofEpochMilli(block.startEpochMillis).atZone(zone))
                Row {
                    Text(time, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.bodyMedium)
                    Card(modifier = Modifier.padding(start = 8.dp)) {
                        val (label, clickable) = when (block) {
                            is TimelineBlock.Busy -> block.title to null
                            is TimelineBlock.Planned -> block.opportunity.title to block.opportunity.id
                            is TimelineBlock.Free -> "Free" to null
                        }
                        Text(
                            text = label,
                            modifier = Modifier.padding(12.dp).let { base ->
                                if (clickable != null) base.clickable { onOpenOpportunity(clickable) } else base
                            }
                        )
                    }
                }
            }
        }
    }
}
