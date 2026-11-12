package com.opportunity.app.ui.capture

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.OpportunityDateHelpers
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.theme.opportunityFilterChipBorder
import com.opportunity.app.ui.theme.opportunityFilterChipColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Quick Capture bottom sheet: fast input + inline inbox triage. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    viewModel: QuickCaptureViewModel = hiltViewModel()
) {
    val inboxItems by viewModel.inboxItems.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().imePadding()) {
            Text(
                "Quick Capture",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            CaptureInputBar(isParsing = isParsing, onSubmit = viewModel::capture)

            Text(
                "UNPROCESSED INBOX · ${inboxItems.size} ${if (inboxItems.size == 1) "ITEM" else "ITEMS"}",
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
                            onComplete = { viewModel.complete(item.id) },
                            onDelete = { viewModel.delete(item.id) },
                            onSetPriority = { bucket -> viewModel.setPriority(item.id, bucket) },
                            onSetDeadline = { millis -> viewModel.setDeadline(item.id, millis) }
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
    val haptics = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) onSubmit(spoken, dueToday, highPriority)
    }

    fun submitCurrent() {
        if (text.isNotBlank()) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSubmit(text, dueToday, highPriority)
            text = ""
            dueToday = false
            highPriority = false
            keyboardController?.hide()
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("I need to…") },
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCurrent() })
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
            IconButton(onClick = ::submitCurrent, enabled = text.isNotBlank() && !isParsing) {
                if (isParsing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.padding(start = 4.dp)
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

private val URGENCY_OPTIONS = listOf(
    PriorityBucket.DO_SOON to "Do Soon",
    PriorityBucket.SHOULD_DO to "Should Do",
    PriorityBucket.SOMEDAY to "Someday"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TriageCard(
    item: OpportunityEntity,
    onSetType: (OpportunityType) -> Unit,
    onAdjustDuration: (Int) -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onSetPriority: (PriorityBucket) -> Unit,
    onSetDeadline: (Long?) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(28.dp)
                        .clickable(onClick = onComplete),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                QUICK_TYPES.forEach { (type, label) ->
                    FilterChip(
                        selected = item.type == type && item.status != OpportunityStatus.INBOX,
                        onClick = { onSetType(type) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Text(
                "How urgent" + if (item.priorityBucket == null) " · not set" else "",
                style = MaterialTheme.typography.labelLarge,
                color = if (item.priorityBucket == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                URGENCY_OPTIONS.forEach { (bucket, label) ->
                    FilterChip(
                        selected = item.priorityBucket == bucket,
                        onClick = { onSetPriority(bucket) },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            val deadlineDate = item.deadlineEpochMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }
            val today = LocalDate.now()
            Text(
                "When" + if (deadlineDate == null) " · not set" else "",
                style = MaterialTheme.typography.labelLarge,
                color = if (deadlineDate == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                FilterChip(
                    selected = deadlineDate == today,
                    onClick = { onSetDeadline(OpportunityDateHelpers.endOfTodayEpochMillis()) },
                    label = { Text("Today") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadlineDate == today.plusDays(1),
                    onClick = { onSetDeadline(OpportunityDateHelpers.endOfTomorrowEpochMillis()) },
                    label = { Text("Tomorrow") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadlineDate != null && deadlineDate > today.plusDays(1) && deadlineDate <= today.plusDays(6),
                    onClick = { onSetDeadline(OpportunityDateHelpers.endOfThisWeekEpochMillis()) },
                    label = { Text("This week") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadlineDate == null,
                    onClick = { onSetDeadline(null) },
                    label = { Text("No date") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Est. Time",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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
