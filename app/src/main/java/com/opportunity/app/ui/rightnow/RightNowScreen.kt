package com.opportunity.app.ui.rightnow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.theme.toColor

private val TIME_PRESETS = listOf(15, 30, 45, 60, 90)

/** Focus tab (formerly "Right Now") -- section 9's decision engine, per the redesign mockup. */
@Composable
fun RightNowScreen(
    onStartTimer: (String) -> Unit,
    viewModel: RightNowViewModel = hiltViewModel()
) {
    val availableMinutes by viewModel.availableMinutes.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val nearbyPlace by viewModel.nearbyPlace.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Focus") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            nearbyPlace?.takeIf { it.matchingOpportunities.isNotEmpty() }?.let { match ->
                LocationBanner(
                    text = "You are at ${match.place.name} · ${match.matchingOpportunities.size} tasks match your location"
                )
            }

            Row(
                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TIME_PRESETS.forEach { minutes ->
                    FilterChip(
                        selected = availableMinutes == minutes,
                        onClick = { viewModel.setAvailableMinutes(minutes) },
                        label = { Text("${minutes}m") }
                    )
                }
            }

            val best = recommendation?.best
            if (best == null) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    title = "Nothing fits right now",
                    body = "Nothing matches this window. Try a longer block, or check your Inbox."
                )
            } else {
                HeroCard(
                    opportunity = best,
                    targetMinutes = availableMinutes,
                    onStart = { onStartTimer(best.id) }
                )

                val alternatives = recommendation?.alternatives.orEmpty()
                if (alternatives.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Text(
                            "If you only have a few minutes",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        alternatives.forEach { alt -> AltCard(alt, onClick = { onStartTimer(alt.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationBanner(text: String) {
    GlassCard(cornerRadius = 999.dp, modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun HeroCard(opportunity: OpportunityEntity, targetMinutes: Int, onStart: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                GlassCard(cornerRadius = 999.dp) {
                    Text(
                        opportunity.category ?: "Task",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                opportunity.priorityBucket?.let { bucket ->
                    val priorityColor = bucket.toColor()
                    GlassCard(cornerRadius = 999.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(priorityColor, androidx.compose.foundation.shape.CircleShape))
                            Text(
                                bucket.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelLarge,
                                color = priorityColor,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            Text(
                opportunity.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )

            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${opportunity.estimatedDurationMinutes ?: targetMinutes}:00", style = MaterialTheme.typography.headlineMedium)
                    Text("Estimated", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("START NOW", modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 14.dp))
            }
        }
    }
}

@Composable
private fun AltCard(opportunity: OpportunityEntity, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.width(180.dp).height(110.dp).clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${opportunity.estimatedDurationMinutes ?: 15} min",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                opportunity.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            Text(
                opportunity.category ?: "Task",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
