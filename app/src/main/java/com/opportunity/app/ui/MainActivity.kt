package com.opportunity.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.opportunity.app.reminder.ReminderScheduler
import com.opportunity.app.ui.navigation.OpportunityApp
import com.opportunity.app.ui.theme.OpportunityTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

data class LaunchDirective(
    val key: String = UUID.randomUUID().toString(),
    val opportunityId: String? = null,
    val openCapture: Boolean = false,
    val startTimer: Boolean = false,
    val editDeadline: Boolean = false
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var reminderScheduler: ReminderScheduler

    private var launchDirective by mutableStateOf<LaunchDirective?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderScheduler.ensureScheduled()
        requestNotificationPermissionIfNeeded()

        launchDirective = parseIntent(intent)

        setContent {
            OpportunityTheme {
                OpportunityApp(launchDirective = launchDirective)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDirective = parseIntent(intent)
    }

    private fun parseIntent(intent: Intent?): LaunchDirective? {
        if (intent == null) return null
        val data = intent.data
        val lastSegment = data?.pathSegments?.lastOrNull()
        val openCapture = intent.getBooleanExtra(EXTRA_OPEN_CAPTURE, false) || lastSegment == "quick-add"
        if (openCapture) {
            return LaunchDirective(openCapture = true)
        }
        if (data != null && data.scheme == "opportunity" && !lastSegment.isNullOrBlank()) {
            return LaunchDirective(
                opportunityId = lastSegment,
                startTimer = data.getQueryParameter("start") == "true",
                editDeadline = data.getQueryParameter("edit") == "deadline"
            )
        }
        return null
    }

    companion object {
        const val EXTRA_OPEN_CAPTURE = "open_capture"
    }

    /**
     * Section 8/13: reminders and notification actions are core to the app's value, so this is
     * asked once up front rather than deferred -- unlike calendar/location, which are asked for
     * contextually where they're used (TimelineScreen, PlacesRepository call sites).
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
