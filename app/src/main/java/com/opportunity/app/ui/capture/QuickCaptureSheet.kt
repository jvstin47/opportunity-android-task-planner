package com.opportunity.app.ui.capture

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import java.util.Locale

/** Quick Capture bottom sheet: fast input + inline inbox triage, matching the redesign mockup. */
@Composable
fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val inboxItems by viewModel.inboxItems.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Quick Capture",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            CaptureInputBar(isParsing = isParsing, onSubmit = viewModel::capture)

            Text(
                "UNPROCESSED INBOX · ${inboxItems.size} ITEMS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (inboxItems.isEmpty()) {
                EmptyState(
                    title = "Nothing waiting for you",
                    body = "Drop anything above — you don't need to organize it right now.",
                    modifier = Modifier.padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(inboxItems, key = { it.id }) { item ->
                        TriageCard(
                            item = item,
                            onSetType = { type -> viewModel.setTypeAndActivate(item.id, type) },
                            onAdjustDuration = { delta -> viewModel.adjustDuration(item.id, delta) },
                            onComplete = { viewModel.complete(item.id) }
                        )
                    }
                    item { Spacer(Modifier.size(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CaptureInputBar(
    isParsing: Boolean,
    onSubmit: (text: String, dueToday: Boolean, highPriority: Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var dueToday by remember { mutableStateOf(false) }
    var highPriority by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (spoken != null) onSubmit(spoken, dueToday, highPriority)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("I need to…") },
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }
                runCatching { voiceLauncher.launch(intent) }
                    .onFailure {
                        android.widget.Toast.makeText(context, "Voice capture isn't available on this device", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice capture")
            }
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    onSubmit(text, dueToday, highPriority)
                    text = ""
                    dueToday = false
                    highPriority = false
                }
            }) {
                if (isParsing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Send, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = { dueToday = !dueToday }) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = if (dueToday) "Due today (on)" else "Set due today",
                    tint = if (dueToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = { highPriority = !highPriority }) {
                Icon(
                    Icons.Filled.Flag,
                    contentDescription = if (highPriority) "High priority (on)" else "Mark high priority",
                    tint = if (highPriority) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                )
            }
            if (dueToday || highPriority) {
                Text(
                    listOfNotNull(
                        "due today".takeIf { dueToday },
                        "high priority".takeIf { highPriority }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).align(androidx.compose.ui.Alignment.CenterVertically)
                )
            }
        }
    }
}

private val QUICK_TYPES = listOf(
    OpportunityType.TASK to "Task",
    OpportunityType.PROJECT to "Project",
    OpportunityType.GOAL to "Goal",
    OpportunityType.IDEA to "Idea"
)

@Composable
private fun TriageCard(
    item: OpportunityEntity,
    onSetType: (OpportunityType) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onComplete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp, end = 12.dp)
                        .size(20.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(onClick = onComplete)
                )
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                QUICK_TYPES.forEach { (type, label) ->
                    FilterChip(
                        selected = item.type == type && item.status != com.opportunity.app.domain.model.OpportunityStatus.INBOX,
                        onClick = { onSetType(type) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "Est. Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = { onAdjustDuration(-15) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "${item.estimatedDurationMinutes ?: 30}m",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(onClick = { onAdjustDuration(15) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
