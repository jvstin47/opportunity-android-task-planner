package com.opportunity.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.opportunity.app.reminder.ReminderScheduler
import com.opportunity.app.ui.navigation.OpportunityApp
import com.opportunity.app.ui.theme.OpportunityTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderScheduler.ensureScheduled()
        requestNotificationPermissionIfNeeded()

        val deepLinkOpportunityId = intent?.data?.pathSegments?.lastOrNull()

        setContent {
            OpportunityTheme {
                OpportunityApp(initialOpportunityId = deepLinkOpportunityId)
            }
        }
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
