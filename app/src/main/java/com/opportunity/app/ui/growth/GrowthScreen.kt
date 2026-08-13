package com.opportunity.app.ui.growth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.common.OpportunityCard

/** Growth tab: merged Explore (goals/ideas/someday) + Insights (weekly stats/reflection). */
@Composable
fun GrowthScreen(
    onOpenOpportunity: (String) -> Unit,
    viewModel: GrowthViewModel = hiltViewModel()
) {
    val lists by viewModel.lists.collectAsState()
    val stats by viewModel.stats.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadStats() }

    Scaffold(topBar = { TopAppBar(title = { Text("Growth") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { StatsRow(stats) }
            item { ReflectionCard(stats) }
            section("GOALS & HABITS", lists.goals + lists.habits, onOpenOpportunity)
            section("IDEAS", lists.ideas, onOpenOpportunity)
            section("SOMEDAY", lists.someday, onOpenOpportunity)
            if (lists.goals.isEmpty() && lists.ideas.isEmpty() && lists.someday.isEmpty() && lists.habits.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing to explore yet",
                        body = "Goals, habits, and ideas you capture will show up here."
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: GrowthStatsState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(Icons.Filled.CheckCircle, "${stats.completedCount}", "Completed", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        StatCard(Icons.Filled.Undo, "${stats.postponedCount}", "Postponed", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        StatCard(Icons.Filled.Schedule, "${stats.focusedMinutes / 60}h ${stats.focusedMinutes % 60}m", "Focused", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.padding(bottom = 8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReflectionCard(stats: GrowthStatsState) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("REFLECTION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (stats.isLoadingReflection) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
            } else {
                Text(stats.reflection ?: "", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    label: String,
    items: List<OpportunityEntity>,
    onOpenOpportunity: (String) -> Unit
) {
    if (items.isEmpty()) return
    item { Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp)) }
    items(items, key = { it.id }) { opportunity ->
        OpportunityCard(opportunity = opportunity, onClick = { onOpenOpportunity(opportunity.id) })
    }
}
