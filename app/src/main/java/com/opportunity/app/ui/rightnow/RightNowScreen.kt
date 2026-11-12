package com.opportunity.app.ui.rightnow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.ui.common.EmptyState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.theme.SurfaceContainer
import com.opportunity.app.ui.theme.SurfaceContainerHigh
import com.opportunity.app.ui.theme.SurfaceContainerHighest
import com.opportunity.app.ui.theme.SurfaceContainerLowest
import com.opportunity.app.ui.theme.TertiaryContainer
import com.opportunity.app.ui.theme.opportunityFilterChipBorder
import com.opportunity.app.ui.theme.opportunityFilterChipColors
import com.opportunity.app.ui.theme.toColor
import com.opportunity.app.util.DateFormatting

private val timeWindows = listOf(15, 30, 60, 120)

/**
 * Focus tab ("What can I do right now?"): Warm Smoked Atelier hero recommendation ring,
 * context-aware location banner, time-window chips, and quick micro-task alternatives.
 */
@Composable
fun RightNowScreen(
    onStartTimer: (String) -> Unit,
    onOpenTasks: () -> Unit = {},
    viewModel: RightNowViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current

    val pulseTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sub-header status + energy badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Right Now",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Matched to your energy & time window",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SurfaceContainerHigh,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(TertiaryContainer)
                        )
                        Text(
                            text = "High Energy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Context-aware location pill when near a saved place
        state.nearbyPlace?.let { match ->
            item {
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "At ${match.place.name} · ${match.matchingOpportunities.size} matching tasks",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Available Window selector pills
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE WINDOW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.availableMinutes}m selected",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeWindows.forEach { mins ->
                        val selected = state.availableMinutes == mins
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setAvailableMinutes(mins) },
                            label = {
                                Text(
                                    text = if (mins >= 60) "${mins / 60}h" else "${mins}m",
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            shape = CircleShape,
                            colors = opportunityFilterChipColors(),
                            border = opportunityFilterChipBorder(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Loading / Error / Hero Card
        when {
            state.isLoading && state.recommendation == null -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            state.error != null -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = state.error ?: "Couldn't load recommendations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = viewModel::refresh) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            state.recommendation?.best == null -> {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), ambientGlow = true) {
                        EmptyState(
                            title = "No urgent match right now",
                            body = "Tap + to capture something new or open Tasks to browse your full plan.",
                            modifier = Modifier.height(260.dp)
                        )
                    }
                }
            }
            else -> {
                val best = state.recommendation!!.best!!
                item {
                    HeroFocusRecommendationCard(
                        opportunity = best,
                        availableMinutes = state.availableMinutes,
                        onStartFocus = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartTimer(best.id)
                        },
                        onRefresh = viewModel::refresh
                    )
                }

                val alternatives = state.recommendation?.alternatives.orEmpty()
                if (alternatives.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "If you only have a few minutes",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "See All",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenTasks)
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 4.dp)
                        ) {
                            items(alternatives, key = { it.id }) { alt ->
                                AlternativeQuickCard(
                                    opportunity = alt,
                                    onStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStartTimer(alt.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFocusRecommendationCard(
    opportunity: OpportunityEntity,
    availableMinutes: Int,
    onStartFocus: () -> Unit,
    onRefresh: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val priorityColor = opportunity.priorityBucket.toColor()
    val estMinutes = opportunity.estimatedDurationMinutes ?: availableMinutes
    val fillRatio = (estMinutes.toFloat() / availableMinutes.coerceAtLeast(1)).coerceIn(0.22f, 1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        ambientGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        Text(
                            text = (opportunity.category?.takeIf { it.isNotBlank() } ?: "BEST MATCH").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh suggestion",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Dual concentric glowing ring
            Box(
                modifier = Modifier.size(156.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeOuter = 11.dp.toPx()
                    val strokeInner = 2.dp.toPx()
                    val pad = strokeOuter / 2f + 4.dp.toPx()
                    val outerSize = Size(size.width - pad * 2f, size.height - pad * 2f)

                    // Outer dark smoked track
                    drawArc(
                        color = SurfaceContainerHighest,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = outerSize,
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )

                    // Primary gradient sweep arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(accent, accentContainer, accent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * fillRatio,
                        useCenter = false,
                        topLeft = Offset(pad, pad),
                        size = outerSize,
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )

                    // Inner subtle dashed ring
                    val innerPad = pad + 18.dp.toPx()
                    val innerSize = Size(size.width - innerPad * 2f, size.height - innerPad * 2f)
                    drawArc(
                        color = tertiary.copy(alpha = 0.28f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(innerPad, innerPad),
                        size = innerSize,
                        style = Stroke(
                            width = strokeInner,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "%02d:00".format(estMinutes),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (opportunity.estimatedDurationMinutes != null) "ESTIMATED" else "WINDOW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Title & description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = opportunity.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (opportunity.description.isNotBlank()) {
                    Text(
                        text = opportunity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3 concise sub-metrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainerLowest.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCell(
                    label = "Window",
                    value = DateFormatting.formatDuration(estMinutes)
                )
                MetricCell(
                    label = "Urgency",
                    value = "${opportunity.urgency}/100"
                )
                MetricCell(
                    label = "Deadline",
                    value = opportunity.deadlineEpochMillis?.let { DateFormatting.formatDeadlineLabel(it) } ?: "Flexible"
                )
            }

            // Gradient Start Focus CTA
            Button(
                onClick = onStartFocus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "START FOCUS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AlternativeQuickCard(
    opportunity: OpportunityEntity,
    onStart: () -> Unit
) {
    GlassCard(
        modifier = Modifier.width(224.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(opportunity.priorityBucket.toColor())
                    )
                    Text(
                        text = opportunity.category?.takeIf { it.isNotBlank() } ?: "Quick Win",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SurfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = opportunity.estimatedDurationMinutes?.let { DateFormatting.formatDuration(it) } ?: "Flex",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = opportunity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .clickable(onClick = onStart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Start ${opportunity.title}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
