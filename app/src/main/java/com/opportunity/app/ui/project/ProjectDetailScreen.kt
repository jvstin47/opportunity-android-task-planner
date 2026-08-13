package com.opportunity.app.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.common.OpportunityCard

@Composable
fun ProjectDetailScreen(
    onStartTimer: (String) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val opportunity = state.opportunity ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text(opportunity.title) }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (opportunity.description.isNotBlank()) {
                Text(opportunity.description, style = MaterialTheme.typography.bodyLarge)
            }

            if (opportunity.status == OpportunityStatus.INBOX) {
                TriageSection(viewModel)
            }

            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                Button(onClick = { onStartTimer(opportunity.id) }) { Text("Start") }
                OutlinedButton(onClick = viewModel::complete, modifier = Modifier.padding(start = 8.dp)) { Text("Complete") }
                OutlinedButton(onClick = viewModel::postpone, modifier = Modifier.padding(start = 8.dp)) { Text("Postpone") }
            }

            if (opportunity.type == OpportunityType.PROJECT || state.children.isNotEmpty()) {
                val total = state.children.size
                val done = state.children.count { it.status == com.opportunity.app.domain.model.OpportunityStatus.COMPLETED }
                if (total > 0) {
                    Text("$done / $total subtasks", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { if (total > 0) done.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
                LazyColumn {
                    items(state.children, key = { it.id }) { child ->
                        OpportunityCard(opportunity = child, onClick = {}, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            } else {
                OutlinedButton(onClick = viewModel::decomposeWithAi, enabled = !state.isDecomposing) {
                    if (state.isDecomposing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text("Break this down with AI")
                }
                state.decomposeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    state.postponementAdvice?.let { advice ->
        AlertDialog(
            onDismissRequest = viewModel::dismissPostponementPrompt,
            title = { Text("Postponed again") },
            text = { Text(advice.message ?: "") },
            confirmButton = {
                TextButton(onClick = viewModel::moveToTomorrow) { Text("Move to tomorrow") }
            },
            dismissButton = {
                Row {
                    if (advice.offerRecurring) {
                        TextButton(onClick = viewModel::makeRecurring) { Text("Make it recurring") }
                    }
                    if (advice.offerArchive) {
                        TextButton(onClick = viewModel::archiveItem) { Text("Archive") }
                    }
                    TextButton(onClick = viewModel::dismissPostponementPrompt) { Text("Keep as is") }
                }
            }
        )
    }
}

/**
 * Section 2.5 "Triage actions from Inbox": the manual path for getting an item onto Today without
 * AI enrichment -- estimate, priority, then either a hard "due today" deadline or just activate it
 * as a candidate for Right Now / Today's Recommended and If You Have Time buckets.
 */
@Composable
private fun TriageSection(viewModel: ProjectDetailViewModel) {
    var minutes by remember { mutableIntStateOf(30) }
    var priority by remember { mutableIntStateOf(3) }

    GlassCard(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Still in your Inbox", style = MaterialTheme.typography.titleLarge)
            Text(
                "Estimate it, set a priority, then add it to Today.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Estimated time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(15, 30, 45, 60).forEach { option ->
                    FilterChip(
                        selected = minutes == option,
                        onClick = { minutes = option },
                        label = { Text("${option}m") }
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(1 to "High", 3 to "Medium", 5 to "Low").forEach { (value, label) ->
                    FilterChip(
                        selected = priority == value,
                        onClick = { priority = value },
                        label = { Text(label) }
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { viewModel.addToToday(minutes, priority, dueToday = true) }) { Text("Due today") }
                OutlinedButton(
                    onClick = { viewModel.addToToday(minutes, priority, dueToday = false) },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Add, no deadline") }
            }
        }
    }
}
