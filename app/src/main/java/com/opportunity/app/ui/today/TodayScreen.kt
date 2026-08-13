package com.opportunity.app.ui.today

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.domain.usecase.TodayPlan
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.common.OpportunityCard

/** Tasks tab (formerly "Today") -- Must Happen / Recommended / If You Have Time, per the redesign mockup. */
@Composable
fun TodayScreen(
    onOpenOpportunity: (String) -> Unit,
    onOpenTimeline: () -> Unit = {},
    viewModel: TodayViewModel = hiltViewModel()
) {
    val plan by viewModel.plan.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today Plan") },
                actions = {
                    IconButton(onClick = onOpenTimeline) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Timeline")
                    }
                }
            )
        }
    ) { padding ->
        val currentPlan = plan
        if (currentPlan == null || isPlanEmpty(currentPlan)) {
            EmptyState(
                modifier = Modifier.padding(padding).fillMaxSize(),
                title = "Nothing planned yet",
                body = "That's okay. Check Focus if you want a suggestion for what to do."
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { CapacityCard(currentPlan) }
            item {
                BucketSection(
                    label = "Must Happen",
                    accent = MaterialTheme.colorScheme.error,
                    items = currentPlan.mustHappen,
                    onOpenOpportunity = onOpenOpportunity
                )
            }
            item {
                BucketSection(
                    label = "Recommended",
                    accent = MaterialTheme.colorScheme.primary,
                    items = currentPlan.recommended,
                    onOpenOpportunity = onOpenOpportunity
                )
            }
            item {
                BucketSection(
                    label = "If You Have Time",
                    accent = MaterialTheme.colorScheme.outlineVariant,
                    items = currentPlan.ifYouHaveTime,
                    onOpenOpportunity = onOpenOpportunity,
                    startExpanded = false
                )
            }
        }
    }
}

private fun isPlanEmpty(plan: TodayPlan) =
    plan.mustHappen.isEmpty() && plan.recommended.isEmpty() && plan.ifYouHaveTime.isEmpty()

@Composable
private fun CapacityCard(plan: TodayPlan) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CAPACITY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${plan.plannedMinutes / 60}h ${plan.plannedMinutes % 60}m Planned" +
                    (plan.availableMinutes?.let { " • ${it / 60}h ${it % 60}m Available" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            val fraction = plan.availableMinutes?.takeIf { it > 0 }
                ?.let { (plan.plannedMinutes.toFloat() / it).coerceIn(0f, 1.5f) } ?: 0f
            LinearProgressIndicator(
                progress = { (fraction / 1.5f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (fraction > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            if (plan.overCapacityMinutes > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            "OVER CAPACITY BY ${plan.overCapacityMinutes / 60}h ${plan.overCapacityMinutes % 60}m",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (plan.suggestedToMove.isNotEmpty()) {
                        Text(
                            "Consider moving: " + plan.suggestedToMove.joinToString(", ") { it.title },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BucketSection(
    label: String,
    accent: Color,
    items: List<OpportunityEntity>,
    onOpenOpportunity: (String) -> Unit,
    startExpanded: Boolean = true
) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(startExpanded) }
    val totalMinutes = items.sumOf { it.estimatedDurationMinutes ?: 0 }

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(4.dp).background(accent))
        GlassCard(modifier = Modifier.weight(1f).animateContentSize()) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Text(
                            "${items.size} ${if (items.size == 1) "task" else "tasks"} • ${totalMinutes / 60}h ${totalMinutes % 60}m",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
                if (expanded) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.forEach { opportunity ->
                            OpportunityCard(
                                opportunity = opportunity,
                                onClick = { onOpenOpportunity(opportunity.id) },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
