package com.opportunity.app.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEvent(
    val title: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long
)

data class FreeWindow(val startEpochMillis: Long, val endEpochMillis: Long) {
    val minutes: Int get() = ((endEpochMillis - startEpochMillis) / 60_000).toInt()
}

/**
 * Section 14 "Calendar Integration": reads the device's Android/Google calendar via
 * CalendarContract so scheduling and Right Now suggestions respect real constraints
 * (class, gym, etc.) instead of operating independently of the user's actual day.
 */
@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun eventsBetween(startEpochMillis: Long, endEpochMillis: Long): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            )
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startEpochMillis.toString())
                .appendPath(endEpochMillis.toString())
                .build()

            val events = mutableListOf<CalendarEvent>()
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                    while (cursor.moveToNext()) {
                        events += CalendarEvent(
                            title = cursor.getString(titleIdx) ?: "Busy",
                            startEpochMillis = cursor.getLong(beginIdx),
                            endEpochMillis = cursor.getLong(endIdx)
                        )
                    }
                }
            events
        }

    /** Section 14/9: gaps between calendar events, used to size realistic "available time" windows. */
    suspend fun freeWindowsBetween(startEpochMillis: Long, endEpochMillis: Long, minGapMinutes: Int = 10): List<FreeWindow> {
        val busy = eventsBetween(startEpochMillis, endEpochMillis).sortedBy { it.startEpochMillis }
        val windows = mutableListOf<FreeWindow>()
        var cursor = startEpochMillis
        for (event in busy) {
            if (event.startEpochMillis > cursor) {
                val gapMinutes = ((event.startEpochMillis - cursor) / 60_000)
                if (gapMinutes >= minGapMinutes) windows += FreeWindow(cursor, event.startEpochMillis)
            }
            cursor = maxOf(cursor, event.endEpochMillis)
        }
        if (endEpochMillis > cursor) windows += FreeWindow(cursor, endEpochMillis)
        return windows
    }
}
