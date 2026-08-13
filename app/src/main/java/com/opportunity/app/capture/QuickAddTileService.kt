package com.opportunity.app.capture

import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.opportunity.app.ui.MainActivity

/** Section 13 "Quick Settings": a fast way to capture something without unlocking into the app. */
class QuickAddTileService : TileService() {

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = android.net.Uri.parse("opportunity://opportunity/quick-add")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivityAndCollapse(
                android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
