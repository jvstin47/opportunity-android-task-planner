package com.opportunity.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opportunity.app.timer.TimerRunState
import com.opportunity.app.timer.TimerUiState
import com.opportunity.app.ui.LaunchDirective
import com.opportunity.app.ui.capture.QuickCaptureSheet
import com.opportunity.app.ui.growth.GrowthScreen
import com.opportunity.app.ui.project.ProjectDetailScreen
import com.opportunity.app.ui.rightnow.RightNowScreen
import com.opportunity.app.ui.settings.SettingsScreen
import com.opportunity.app.ui.theme.SurfaceContainerHigh
import com.opportunity.app.ui.theme.SurfaceContainerHighest
import com.opportunity.app.ui.theme.SurfaceContainerLowest
import com.opportunity.app.ui.theme.opportunityNavigationBarItemColors
import com.opportunity.app.ui.timeline.TimelineScreen
import com.opportunity.app.ui.timer.TimerScreen
import com.opportunity.app.ui.today.TodayScreen

sealed class Destination(val route: String, val label: String, val icon: ImageVector) {
    object Focus : Destination("focus", "Focus", Icons.Filled.Explore)
    object Tasks : Destination("tasks", "Tasks", Icons.Filled.CheckCircle)
    object Growth : Destination("growth", "Growth", Icons.Filled.AutoAwesome)
    object Vault : Destination("vault", "Vault", Icons.Filled.Shield)
}

private val bottomDestinations = listOf(Destination.Focus, Destination.Tasks, Destination.Growth, Destination.Vault)
private val topLevelRoutes = bottomDestinations.map { it.route }.toSet()

/**
 * App shell: Warm Smoked Atelier mobile_tab chrome with floating glass top bar,
 * live active timer bar, circular Quick Capture FAB with Inbox count, and dark bottom navigation.
 */
@Composable
fun OpportunityApp(
    launchDirective: LaunchDirective?,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showTabChrome = currentRoute in topLevelRoutes
    val activeTab = bottomDestinations.firstOrNull { it.route == currentRoute } ?: Destination.Focus

    var showCapture by remember { mutableStateOf(false) }
    val inboxCount by viewModel.inboxCount.collectAsState()
    val activeTimer by viewModel.activeTimer.collectAsState()

    val navigateToTab: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Consume deep links / widget taps / notification actions exactly once per directive key
    LaunchedEffect(launchDirective?.key) {
        val directive = launchDirective ?: return@LaunchedEffect
        when {
            directive.openCapture -> showCapture = true
            directive.opportunityId != null && directive.startTimer -> {
                navController.navigate("timer/${directive.opportunityId}")
            }
            directive.opportunityId != null -> {
                navController.navigate("opportunity/${directive.opportunityId}?edit=${directive.editDeadline}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTabChrome) {
                AtelierTopBar(
                    title = activeTab.label,
                    onSearchClick = { navigateToTab(Destination.Tasks) },
                    onAvatarClick = { navigateToTab(Destination.Vault) }
                )
            }
        },
        floatingActionButton = {
            if (showTabChrome) {
                BadgedBox(
                    badge = {
                        if (inboxCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Text(inboxCount.toString(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { showCapture = true },
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Quick Capture",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val running = activeTimer
                val onTimerRoute = currentRoute?.startsWith("timer") == true
                AnimatedVisibility(
                    visible = running != null && !onTimerRoute,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    if (running != null) {
                        ActiveTimerFloatingBar(
                            timerState = running,
                            onTogglePause = viewModel::toggleTimerPauseResume,
                            onExpand = { navController.navigate("timer/${running.opportunityId}") },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .let { mod -> if (!showTabChrome) mod.navigationBarsPadding() else mod }
                        )
                    }
                }

                if (showTabChrome) {
                    Surface(
                        color = SurfaceContainerLowest.copy(alpha = 0.94f),
                        tonalElevation = 0.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                            NavigationBar(
                                containerColor = SurfaceContainerLowest.copy(alpha = 0.0f),
                                tonalElevation = 0.dp
                            ) {
                                bottomDestinations.forEach { destination ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateToTab(destination) },
                                        icon = {
                                            Icon(
                                                imageVector = destination.icon,
                                                contentDescription = destination.label,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = destination.label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            )
                                        },
                                        colors = opportunityNavigationBarItemColors()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Focus.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Destination.Focus.route) {
                RightNowScreen(
                    onStartTimer = { id -> navController.navigate("timer/$id") },
                    onOpenTasks = { navigateToTab(Destination.Tasks) }
                )
            }
            composable(Destination.Tasks.route) {
                TodayScreen(
                    onOpenOpportunity = { id -> navController.navigate("opportunity/$id") },
                    onOpenTimeline = { navController.navigate("timeline") }
                )
            }
            composable(Destination.Growth.route) {
                GrowthScreen(onOpenOpportunity = { id -> navController.navigate("opportunity/$id") })
            }
            composable(Destination.Vault.route) {
                SettingsScreen()
            }
            composable("timeline") {
                TimelineScreen(
                    onOpenOpportunity = { id -> navController.navigate("opportunity/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "opportunity/{opportunityId}?edit={edit}",
                arguments = listOf(
                    navArgument("opportunityId") { type = NavType.StringType },
                    navArgument("edit") { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val openEdit = entry.arguments?.getBoolean("edit") ?: false
                ProjectDetailScreen(
                    onStartTimer = { id -> navController.navigate("timer/$id") },
                    onBack = { navController.popBackStack() },
                    onOpenOpportunity = { id -> navController.navigate("opportunity/$id") },
                    initialOpenEdit = openEdit
                )
            }
            composable("timer/{opportunityId}") { entry ->
                val id = entry.arguments?.getString("opportunityId") ?: return@composable
                TimerScreen(
                    opportunityId = id,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

    if (showCapture) {
        QuickCaptureSheet(onDismiss = { showCapture = false })
    }
}

@Composable
private fun AtelierTopBar(
    title: String,
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(SurfaceContainerHigh)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AllInclusive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Quick Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Vault & Profile",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerFloatingBar(
    timerState: TimerUiState,
    onTogglePause: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_timer_spin")
    val spinDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_timer_angle"
    )
    val isPaused = timerState.runState == TimerRunState.PAUSED
    val timeText = remember(timerState.elapsedMillis, timerState.targetMinutes) {
        val targetMin = timerState.targetMinutes
        if (targetMin != null && targetMin > 0) {
            val remainingSec = ((targetMin * 60_000L - timerState.elapsedMillis).coerceAtLeast(0L) / 1000L)
            "%02d:%02d remaining".format(remainingSec / 60, remainingSec % 60)
        } else {
            val elapsedSec = (timerState.elapsedMillis / 1000L).coerceAtLeast(0L)
            "%02d:%02d elapsed".format(elapsedSec / 60, elapsedSec % 60)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onExpand),
        color = SurfaceContainerHigh.copy(alpha = 0.96f),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Timelapse,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (isPaused) 0f else spinDegrees)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timerState.opportunityTitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isPaused) "$timeText · Paused" else timeText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest)
                        .clickable(onClick = onTogglePause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume timer" else "Pause timer",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest)
                        .clickable(onClick = onExpand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = "Open focus timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
