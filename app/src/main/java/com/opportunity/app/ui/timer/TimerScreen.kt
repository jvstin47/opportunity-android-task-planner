package com.opportunity.app.ui.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.opportunity.app.domain.model.TimerMode
import com.opportunity.app.timer.TimerRunState
import com.opportunity.app.timer.TimerUiState
import com.opportunity.app.ui.common.GlassCard
import com.opportunity.app.ui.theme.SurfaceContainer
import com.opportunity.app.ui.theme.SurfaceContainerHigh
import com.opportunity.app.ui.theme.SurfaceContainerHighest
import com.opportunity.app.ui.theme.TertiaryContainer

/**
 * Full-screen Warm Smoked Atelier focus timer (`mobile_stack` shell):
 * 210dp dual-ring radial timer, pause/resume + glowing Finish & Log controls,
 * daily deep-work telemetry cards, and Quick Session Jotter note logging.
 */
@Composable
fun TimerScreen(
    opportunityId: String,
    onDone: () -> Unit,
    onBack: () -> Unit = onDone,
    viewModel: TimerScreenViewModel = hiltViewModel()
) {
    val timerState by viewModel.timerState.collectAsState()
    val opportunityTitle by viewModel.opportunityTitle.collectAsState()
    val opportunityCategory by viewModel.opportunityCategory.collectAsState()
    val todayFocused by viewModel.todayFocusedDuration.collectAsState()
    val completedToday by viewModel.completedTodayCount.collectAsState()

    var jotterNote by rememberSaveable { mutableStateOf("") }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(opportunityId) {
        viewModel.beginIfNeeded(opportunityId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back (keep timer running)",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ACTIVE SPRINT",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Focus Session",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { showDiscardDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Discard session",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { innerPadding ->
        val active = timerState
        if (active == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Timer Smoked-Glass Card
                HeroTimerSessionCard(
                    state = active,
                    title = opportunityTitle.ifBlank { active.opportunityTitle },
                    category = opportunityCategory,
                    onPauseResume = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (active.runState == TimerRunState.PAUSED) viewModel.resume() else viewModel.pause()
                    },
                    onFinish = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.finish(jotterNote)
                        onDone()
                    }
                )

                // 2-Column Micro Telemetry Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimerMicroStatCard(
                        icon = Icons.Filled.Bolt,
                        iconTint = MaterialTheme.colorScheme.primary,
                        label = "Deep Work Today",
                        value = todayFocused,
                        subtitle = "$completedToday tasks done",
                        modifier = Modifier.weight(1f)
                    )
                    TimerMicroStatCard(
                        icon = Icons.Filled.Timelapse,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        label = "Sprint Mode",
                        value = when {
                            active.targetMinutes == null -> "Stopwatch"
                            active.mode == TimerMode.COUNTDOWN -> "Countdown"
                            active.mode == TimerMode.STOPWATCH -> "Stopwatch"
                            active.mode == TimerMode.SESSION_BASED -> "Sessions"
                            else -> "Cumulative"
                        },
                        subtitle = active.targetMinutes?.let { "${it}m target" } ?: "Open-ended",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Session Jotter
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Session Jotter",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                if (jotterNote.isEmpty()) {
                                    Text(
                                        text = "Park quick thoughts or blockers here—saved with this sprint when you Finish & Log.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                }
                                BasicTextField(
                                    value = jotterNote,
                                    onValueChange = { jotterNote = it },
                                    minLines = 2,
                                    maxLines = 4,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            containerColor = SurfaceContainerHigh,
            title = { Text("Discard focus session?") },
            text = {
                Text(
                    "Discarding will stop this sprint without logging focus minutes. Choose Keep Running to step back while keeping the timer active."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discard()
                        onDone()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text("Keep Running", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun HeroTimerSessionCard(
    state: TimerUiState,
    title: String,
    category: String?,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit
) {
    val isRunning = state.runState == TimerRunState.RUNNING
    val isPaused = state.runState == TimerRunState.PAUSED
    val accent = MaterialTheme.colorScheme.primary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    val ringSweepFraction = if (state.targetMinutes != null && state.targetMinutes > 0) {
        state.progressFraction.coerceIn(0.03f, 1f)
    } else {
        // Gentle minute-hand sweep for open-ended stopwatch mode
        (((state.elapsedMillis / 1000L) % 60L) / 60f).coerceAtLeast(0.05f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "timer_ring")
    val outerOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "focus_dot"
    )

    val formattedClock = formatClock(state)
    val subStatusText = when {
        state.isOverTarget -> "Over target · ${state.elapsedMinutes}m logged"
        state.targetMinutes != null -> "Target ${state.targetMinutes}m"
        else -> "Open sprint · ${state.elapsedMinutes}m elapsed"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp,
        ambientGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Pill
            Surface(
                shape = CircleShape,
                color = SurfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .alpha(if (isRunning) dotAlpha else 1f)
                            .clip(CircleShape)
                            .background(if (isPaused) MaterialTheme.colorScheme.secondary else TertiaryContainer)
                    )
                    Text(
                        text = if (isPaused) "PAUSED · SPRINT" else "FOCUSING · DEEP WORK",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = if (isPaused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Title & Category
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                category?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 210.dp Dual Concentric Timer Gauge
            Box(
                modifier = Modifier.size(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dashedRadiusPad = 5.dp.toPx()
                    val mainStroke = 13.dp.toPx()
                    val mainPad = dashedRadiusPad + 14.dp.toPx()
                    val mainSize = Size(size.width - mainPad * 2f, size.height - mainPad * 2f)

                    // Subtle rotating dashed outer halo
                    rotate(if (isRunning) outerOrbitAngle else 0f) {
                        drawCircle(
                            color = accent.copy(alpha = 0.28f),
                            radius = size.minDimension / 2f - dashedRadiusPad,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                            )
                        )
                    }

                    // Smoked ring base
                    drawArc(
                        color = SurfaceContainerHighest,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(mainPad, mainPad),
                        size = mainSize,
                        style = Stroke(width = mainStroke, cap = StrokeCap.Round)
                    )

                    // Warm gradient progress sweep
                    drawArc(
                        brush = Brush.sweepGradient(listOf(accent, accentContainer, accent)),
                        startAngle = -90f,
                        sweepAngle = 360f * ringSweepFraction,
                        useCenter = false,
                        topLeft = Offset(mainPad, mainPad),
                        size = mainSize,
                        style = Stroke(width = mainStroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formattedClock,
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subStatusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceContainerHighest,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPauseResume)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPaused) " Resume" else " Pause",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Finish & Log",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // Focus Shield Notice
            Surface(
                shape = CircleShape,
                color = SurfaceContainer.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DoNotDisturbOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Foreground timer locked · Safe to switch apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerMicroStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatClock(state: TimerUiState): String {
    val targetMin = state.targetMinutes
    val displaySeconds = if (targetMin != null && targetMin > 0 && state.mode != TimerMode.STOPWATCH) {
        val targetMillis = targetMin * 60_000L
        if (state.elapsedMillis <= targetMillis) {
            (targetMillis - state.elapsedMillis) / 1000L
        } else {
            (state.elapsedMillis - targetMillis) / 1000L
        }
    } else {
        state.elapsedMillis / 1000L
    }
    val totalMinutes = displaySeconds / 60L
    val seconds = displaySeconds % 60L
    val prefix = if (targetMin != null && state.isOverTarget && state.mode != TimerMode.STOPWATCH) "+" else ""
    return "%s%02d:%02d".format(prefix, totalMinutes, seconds)
}
