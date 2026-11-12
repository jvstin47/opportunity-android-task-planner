package com.opportunity.app.ui.project

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.OpportunityDateHelpers
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.recurrence
import com.opportunity.app.data.local.reminder
import com.opportunity.app.domain.model.OpportunityStatus
import com.opportunity.app.domain.model.OpportunityType
import com.opportunity.app.domain.model.PriorityBucket
import com.opportunity.app.domain.model.RecurrenceFrequency
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.common.OpportunityCard
import com.opportunity.app.ui.theme.opportunityFilterChipBorder
import com.opportunity.app.ui.theme.opportunityFilterChipColors
import com.opportunity.app.ui.theme.toColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onStartTimer: (String) -> Unit,
    onBack: () -> Unit,
    onOpenOpportunity: (String) -> Unit = {},
    initialOpenEdit: Boolean = false,
    viewModel: ProjectDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val opportunity = state.opportunity
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditSheet by remember(initialOpenEdit) { mutableStateOf(initialOpenEdit) }
    var newSubtaskTitle by remember { mutableStateOf("") }

    if (opportunity == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }

    val reminderConfig = remember(opportunity.reminderJson) { opportunity.reminder() }
    val recurrenceRule = remember(opportunity.recurrenceJson) { opportunity.recurrence() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        opportunity.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                opportunity.category?.takeIf { it.isNotBlank() }?.let { cat ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                opportunity.priorityBucket?.let { bucket ->
                                    val color = bucket.toColor()
                                    Box(
                                        modifier = Modifier
                                            .background(color.copy(alpha = 0.16f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            bucket.name.replace('_', ' '),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = color
                                        )
                                    }
                                }
                            }
                            if (opportunity.status == OpportunityStatus.COMPLETED) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Completed",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            opportunity.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        if (opportunity.description.isNotBlank()) {
                            Text(
                                opportunity.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Metadata chips row
                        Row(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            opportunity.estimatedDurationMinutes?.let { est ->
                                MetaTag(Icons.Filled.Schedule, "${est}m est · ${opportunity.actualDurationMinutes}m logged")
                            } ?: run {
                                if (opportunity.actualDurationMinutes > 0) {
                                    MetaTag(Icons.Filled.Schedule, "${opportunity.actualDurationMinutes}m logged")
                                }
                            }
                            opportunity.deadlineEpochMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                MetaTag(Icons.Filled.Schedule, "Due ${DateTimeFormatter.ofPattern("MMM d").format(date)}")
                            }
                            if (reminderConfig.enabled) {
                                val freqLabel = if (reminderConfig.frequencyMinutes >= 1440) "Daily" else "Every ${reminderConfig.frequencyMinutes / 60}h"
                                MetaTag(Icons.Filled.NotificationsActive, freqLabel)
                            }
                            if (recurrenceRule.frequency != RecurrenceFrequency.NONE) {
                                MetaTag(Icons.Filled.Repeat, recurrenceRule.frequency.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }

            if (opportunity.status == OpportunityStatus.INBOX) {
                item { TriageSection(viewModel) }
            }

            item {
                if (opportunity.status != OpportunityStatus.COMPLETED) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onStartTimer(opportunity.id) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Focus", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.complete(); onBack() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Complete")
                            }
                            OutlinedButton(
                                onClick = viewModel::postpone,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text("Postpone (${opportunity.postponementCount})")
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = viewModel::reopen,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reopen Task")
                    }
                }
            }

            // Subtasks & breakdown section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SUBTASKS",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val total = state.children.size
                            val done = state.children.count { it.status == OpportunityStatus.COMPLETED }
                            if (total > 0) {
                                Text(
                                    "$done / $total done",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.children.isNotEmpty()) {
                            val total = state.children.size
                            val done = state.children.count { it.status == OpportunityStatus.COMPLETED }
                            LinearProgressIndicator(
                                progress = { done.toFloat() / total },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSubtaskTitle,
                                onValueChange = { newSubtaskTitle = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Add a step…") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newSubtaskTitle.isNotBlank()) {
                                            viewModel.addSubtask(newSubtaskTitle)
                                            newSubtaskTitle = ""
                                        }
                                    }
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        viewModel.addSubtask(newSubtaskTitle)
                                        newSubtaskTitle = ""
                                    }
                                },
                                enabled = newSubtaskTitle.isNotBlank()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add subtask", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (state.children.isEmpty()) {
                            OutlinedButton(
                                onClick = viewModel::decomposeWithAi,
                                enabled = !state.isDecomposing,
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                if (state.isDecomposing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                                }
                                Text("Break this down with AI")
                            }
                            state.decomposeError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }

            items(state.children, key = { it.id }) { child ->
                OpportunityCard(
                    opportunity = child,
                    onClick = { onOpenOpportunity(child.id) },
                    onToggleComplete = { viewModel.toggleChildComplete(child) }
                )
            }
        }
    }

    if (showEditSheet) {
        EditOpportunitySheet(
            opportunity = opportunity,
            onDismiss = { showEditSheet = false },
            onSave = { title, desc, cat, type, bucket, minutes, deadline, reminderMins, recurrence ->
                viewModel.updateDetails(title, desc, cat, type, bucket, minutes, deadline, reminderMins, recurrence)
                showEditSheet = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this?") },
            text = { Text("\"${opportunity.title}\" will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteItem(); onBack() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
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

@Composable
private fun MetaTag(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditOpportunitySheet(
    opportunity: OpportunityEntity,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String?,
        type: OpportunityType,
        bucket: PriorityBucket?,
        minutes: Int?,
        deadlineMillis: Long?,
        reminderMinutes: Int?,
        recurrence: RecurrenceFrequency
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(opportunity.title) }
    var description by remember { mutableStateOf(opportunity.description) }
    var category by remember { mutableStateOf(opportunity.category.orEmpty()) }
    var type by remember { mutableStateOf(opportunity.type) }
    var priority by remember { mutableStateOf(opportunity.priorityBucket ?: PriorityBucket.SHOULD_DO) }
    var duration by remember { mutableStateOf(opportunity.estimatedDurationMinutes) }
    var deadline by remember { mutableStateOf(opportunity.deadlineEpochMillis) }
    val initialReminder = remember(opportunity.reminderJson) { opportunity.reminder() }
    var reminderMinutes by remember {
        mutableStateOf(if (initialReminder.enabled) initialReminder.frequencyMinutes else null)
    }
    val initialRecurrence = remember(opportunity.recurrenceJson) { opportunity.recurrence() }
    var recurrence by remember { mutableStateOf(initialRecurrence.frequency) }

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val deadlineDate = deadline?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Opportunity", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                TextButton(
                    onClick = {
                        onSave(
                            title,
                            description,
                            category.takeIf { it.isNotBlank() },
                            type,
                            priority,
                            duration,
                            deadline,
                            reminderMinutes,
                            recurrence
                        )
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / description") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g. Work, College, Gym)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                OpportunityType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    PriorityBucket.DO_SOON to "Do Soon",
                    PriorityBucket.SHOULD_DO to "Should Do",
                    PriorityBucket.GROW to "Grow",
                    PriorityBucket.EXPLORE to "Explore",
                    PriorityBucket.PERSONAL to "Personal",
                    PriorityBucket.SOMEDAY to "Someday"
                ).forEach { (b, label) ->
                    FilterChip(
                        selected = priority == b,
                        onClick = { priority = b },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Text("Estimated Duration", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(15, 30, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = duration == mins,
                        onClick = { duration = if (duration == mins) null else mins },
                        label = { Text("${mins}m") },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Text("Deadline", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = deadlineDate == today,
                    onClick = { deadline = OpportunityDateHelpers.endOfTodayEpochMillis() },
                    label = { Text("Today") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadlineDate == today.plusDays(1),
                    onClick = { deadline = OpportunityDateHelpers.endOfTomorrowEpochMillis() },
                    label = { Text("Tomorrow") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadlineDate != null && deadlineDate > today.plusDays(1) && deadlineDate <= today.plusDays(6),
                    onClick = { deadline = OpportunityDateHelpers.endOfThisWeekEpochMillis() },
                    label = { Text("This week") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
                FilterChip(
                    selected = deadline == null,
                    onClick = { deadline = null },
                    label = { Text("No deadline") },
                    shape = RoundedCornerShape(999.dp),
                    colors = opportunityFilterChipColors(),
                    border = opportunityFilterChipBorder()
                )
            }

            Text("Reminder & Recurrence", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(null to "No reminder", 120 to "Every 2h", 1440 to "Daily reminder").forEach { (mins, label) ->
                    FilterChip(
                        selected = reminderMinutes == mins,
                        onClick = { reminderMinutes = mins },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    RecurrenceFrequency.NONE to "One-time",
                    RecurrenceFrequency.DAILY to "Repeats daily",
                    RecurrenceFrequency.WEEKLY to "Repeats weekly"
                ).forEach { (freq, label) ->
                    FilterChip(
                        selected = recurrence == freq,
                        onClick = { recurrence = freq },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Button(
                onClick = {
                    onSave(
                        title,
                        description,
                        category.takeIf { it.isNotBlank() },
                        type,
                        priority,
                        duration,
                        deadline,
                        reminderMinutes,
                        recurrence
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 4.dp),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TriageSection(viewModel: ProjectDetailViewModel) {
    var minutes by remember { mutableIntStateOf(30) }
    var priority by remember { mutableIntStateOf(3) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                        label = { Text("${option}m") },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                listOf(1 to "High", 3 to "Medium", 5 to "Low").forEach { (value, label) ->
                    FilterChip(
                        selected = priority == value,
                        onClick = { priority = value },
                        label = { Text(label) },
                        shape = RoundedCornerShape(999.dp),
                        colors = opportunityFilterChipColors(),
                        border = opportunityFilterChipBorder()
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.addToToday(minutes, priority, dueToday = true) }) { Text("Due today") }
                OutlinedButton(onClick = { viewModel.addToToday(minutes, priority, dueToday = false) }) {
                    Text("Add, no deadline")
                }
            }
        }
    }
}
