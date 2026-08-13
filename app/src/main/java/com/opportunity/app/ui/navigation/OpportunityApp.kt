package com.opportunity.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.opportunity.app.ui.capture.QuickCaptureSheet
import com.opportunity.app.ui.growth.GrowthScreen
import com.opportunity.app.ui.project.ProjectDetailScreen
import com.opportunity.app.ui.rightnow.RightNowScreen
import com.opportunity.app.ui.settings.SettingsScreen
import com.opportunity.app.ui.timeline.TimelineScreen
import com.opportunity.app.ui.timer.TimerScreen
import com.opportunity.app.ui.today.TodayScreen

sealed class Destination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Focus : Destination("focus", "Focus", Icons.Filled.GpsFixed)
    object Tasks : Destination("tasks", "Tasks", Icons.Filled.CheckCircle)
    object Growth : Destination("growth", "Growth", Icons.Filled.TrendingUp)
    object Vault : Destination("vault", "Vault", Icons.Filled.Lock)
}

private val bottomDestinations = listOf(Destination.Focus, Destination.Tasks, Destination.Growth, Destination.Vault)

/**
 * Redesign nav (August 2026): 4 tabs -- Focus=RightNow, Tasks=Today, Growth=Explore+Insights
 * merged, Vault=Settings. Inbox is no longer a top-level tab; capture happens through the
 * QuickCapture bottom sheet (FAB), and Timeline is reachable from Tasks rather than its own tab.
 */
@Composable
fun OpportunityApp(initialOpportunityId: String?) {
    val navController = rememberNavController()
    var showQuickCapture by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpportunityId) {
        initialOpportunityId?.let { navController.navigate("opportunity/$it") }
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                bottomDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickCapture = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Quick Capture")
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Focus.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Focus.route) {
                RightNowScreen(onStartTimer = { navController.navigate("timer/$it") })
            }
            composable(Destination.Tasks.route) {
                TodayScreen(
                    onOpenOpportunity = { navController.navigate("opportunity/$it") },
                    onOpenTimeline = { navController.navigate("timeline") }
                )
            }
            composable(Destination.Growth.route) {
                GrowthScreen(onOpenOpportunity = { navController.navigate("opportunity/$it") })
            }
            composable(Destination.Vault.route) {
                SettingsScreen()
            }
            composable("timeline") {
                TimelineScreen(onOpenOpportunity = { navController.navigate("opportunity/$it") })
            }
            composable("opportunity/{opportunityId}") {
                ProjectDetailScreen(onStartTimer = { navController.navigate("timer/$it") })
            }
            composable("timer/{opportunityId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("opportunityId").orEmpty()
                TimerScreen(opportunityId = id, onDone = { navController.popBackStack() })
            }
        }
    }

    if (showQuickCapture) {
        QuickCaptureSheet(onDismiss = { showQuickCapture = false })
    }
}
