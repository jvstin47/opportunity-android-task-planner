package com.opportunity.app.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.usecase.TodayPlan
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.common.OpportunityCard
import com.opportunity.app.ui.theme.SurfaceContainer
import com.opportunity.app.ui.theme.SurfaceContainerHigh
import com.opportunity.app.ui.theme.SurfaceContainerHighest
import com.opportunity.app.util.DateFormatting
import kotlinx.coroutines.launch

/**
 * Tasks tab ("Today Plan"): borderless pill search bar, real calendar capacity gauge,
 * collapsible Must Happen / Recommended / If You Have Time sections, and Undo completion snackbar.
 */
@Composable
fun TodayScreen(
    onOpenOpportunity: (String) -> Unit,
    onOpenTimeline: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var mustExpanded by rememberSaveable { mutableStateOf(true) }
    var recommendedExpanded by rememberSaveable { mutableStateOf(true) }
    var optionalExpanded by rememberSaveable { mutableStateOf(true) }
    var completedExpanded by rememberSaveable { mutableStateOf(false) }

    val onCompleteWithUndo: (OpportunityEntity) -> Unit = { item ->
        viewModel.complete(item.id)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Completed \"${item.title}\"",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.reopen(item.id)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 84.dp)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header + Timeline Pill Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Today Plan",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Realistic day capacity & priorities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenTimeline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Timeline",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Borderless smoked pill search bar
            item {
                AtelierSearchField(
                    query = state.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = "Search today's plan..."
                )
            }

            // Error banner if present
            state.error?.let { err ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::clearError) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            when {
                state.isLoading && state.plan == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                !state.hasAnyItems -> {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth(), ambientGlow = true) {
                            EmptyState(
                                title = if (state.searchQuery.isBlank()) "A calm, open slate today" else "No matching tasks",
                                body = if (state.searchQuery.isBlank()) {
                                    "Tap + to add tasks or check Timeline for scheduled blocks."
                                } else {
                                    "Try clearing your search filter to see the full plan."
                                },
                                modifier = Modifier.height(250.dp)
                            )
                        }
                    }
                }
                else -> {
                    state.plan?.let { plan ->
                        item {
                            CapacityGaugeCard(plan = plan)
                        }

                        if (plan.mustHappen.isNotEmpty()) {
                            item {
                                BucketSectionCard(
                                    title = "Must Happen",
                                    items = plan.mustHappen,
                                    accentColor = MaterialTheme.colorScheme.error,
                                    expanded = mustExpanded,
                                    onToggleExpand = { mustExpanded = !mustExpanded },
                                    showOverdueBadge = plan.mustHappen.any {
                                        val dl = it.deadlineEpochMillis
                                        dl != null && dl < System.currentTimeMillis()
                                    },
                                    onOpenOpportunity = onOpenOpportunity,
                                    onToggleComplete = onCompleteWithUndo
                                )
                            }
                        }

                        if (plan.recommended.isNotEmpty()) {
                            item {
                                BucketSectionCard(
                                    title = "Recommended",
                                    items = plan.recommended,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    expanded = recommendedExpanded,
                                    onToggleExpand = { recommendedExpanded = !recommendedExpanded },
                                    showOverdueBadge = false,
                                    onOpenOpportunity = onOpenOpportunity,
                                    onToggleComplete = onCompleteWithUndo
                                )
                            }
                        }

                        if (plan.ifYouHaveTime.isNotEmpty()) {
                            item {
                                BucketSectionCard(
                                    title = "If You Have Time",
                                    items = plan.ifYouHaveTime,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    expanded = optionalExpanded,
                                    onToggleExpand = { optionalExpanded = !optionalExpanded },
                                    showOverdueBadge = false,
                                    onOpenOpportunity = onOpenOpportunity,
                                    onToggleComplete = onCompleteWithUndo
                                )
                            }
                        }
                    }

                    if (state.completedToday.isNotEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { completedExpanded = !completedExpanded }
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Completed Today",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                                            ) {
                                                Text(
                                                    text = "${state.completedToday.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (completedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    AnimatedVisibility(visible = completedExpanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            state.completedToday.forEach { item ->
                                                OpportunityCard(
                                                    opportunity = item,
                                                    onClick = { onOpenOpportunity(item.id) },
                                                    onToggleComplete = { viewModel.reopen(item.id) }
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
    }
}

@Composable
private fun AtelierSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    Surface(
        shape = CircleShape,
        color = SurfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapacityGaugeCard(plan: TodayPlan) {
    val available = plan.availableMinutes
    val planned = plan.plannedMinutes
    val over = plan.overCapacityMinutes
    val ratio = when {
        available != null && available > 0 -> (planned.toFloat() / available).coerceIn(0f, 1.25f)
        else -> (planned / 360f).coerceIn(0f, 1f)
    }
    val pctText = if (available != null && available > 0) {
        "${((planned.toFloat() / available) * 100).toInt()}% Allocated"
    } else {
        "Calendar Off"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        ambientGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "CAPACITY",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = pctText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (over > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = DateFormatting.formatDuration(planned),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (available != null) {
                            "of ${DateFormatting.formatDuration(available)} free calendar space"
                        } else {
                            "planned · open Timeline to grant Calendar access"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (available != null) {
                    val statusColor = if (over > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    val statusLabel = if (over > 0) {
                        "${DateFormatting.formatDuration(over)} over"
                    } else {
                        "${DateFormatting.formatDuration(available - planned)} buffer"
                    }
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Smooth 6.dp rounded progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (over > 0) {
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.secondary)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                )
                            }
                        )
                )
            }

            if (plan.suggestedToMove.isNotEmpty()) {
                Text(
                    text = "Tip: Move \"${plan.suggestedToMove.first().title}\" to stay under capacity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun BucketSectionCard(
    title: String,
    items: List<OpportunityEntity>,
    accentColor: Color,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    showOverdueBadge: Boolean,
    onOpenOpportunity: (String) -> Unit,
    onToggleComplete: (OpportunityEntity) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = SurfaceContainerHighest
                    ) {
                        Text(
                            text = "${items.size} ${if (items.size == 1) "item" else "items"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (showOverdueBadge) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { opportunity ->
                        OpportunityCard(
                            opportunity = opportunity,
                            onClick = { onOpenOpportunity(opportunity.id) },
                            onToggleComplete = { onToggleComplete(opportunity) }
                        )
                    }
                }
            }
        }
    }
}
