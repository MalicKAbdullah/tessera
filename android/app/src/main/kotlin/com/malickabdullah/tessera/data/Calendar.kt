package com.malickabdullah.tessera.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** One occurrence of an event (a recurring event yields one per instance). */
data class CalendarEvent(
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    /** Opaque ARGB colour the calendar app shows for the event. */
    val color: Int,
    val location: String,
)

/**
 * What designs know about the calendar. [granted] false means READ_CALENDAR
 * has not been granted, so [events] is empty and designs draw their date-only
 * state with a "connect calendar" hint.
 */
data class CalendarState(val granted: Boolean, val events: List<CalendarEvent>) {
    /** Identifies the events a bitmap shows, for live keys. */
    val fingerprint: String
        get() = if (!granted) "off" else events.joinToString(",") { "${it.begin.epochSecond}-${it.end.epochSecond}-${it.title.hashCode()}" }.hashCode().toString()
}

object CalendarSource : DataSource<CalendarState> {
    /** Days of instances read from the start of today; enough for a week strip and a two-week agenda. */
    const val WINDOW_DAYS = 14L
    private const val LIMIT = 120

    private val projection = arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.DISPLAY_COLOR,
        CalendarContract.Instances.EVENT_LOCATION,
    )

    fun granted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override fun read(context: Context): CalendarState {
        if (!granted(context)) return CalendarState(false, emptyList())
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val end = start.plusSeconds(WINDOW_DAYS * 86_400)
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, start.toEpochMilli())
            ContentUris.appendId(it, end.toEpochMilli())
        }.build()
        val selection = "${CalendarContract.Instances.VISIBLE} = 1 AND " +
            "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext() && events.size < LIMIT) {
                val allDay = c.getInt(3) == 1
                events += CalendarEvent(
                    title = c.getString(0)?.trim().orEmpty().ifEmpty { "(No title)" },
                    begin = instant(c.getLong(1), allDay, zone),
                    end = instant(c.getLong(2), allDay, zone),
                    allDay = allDay,
                    color = c.getInt(4) or 0xFF000000.toInt(),
                    location = c.getString(5)?.trim().orEmpty(),
                )
            }
        }
        return CalendarState(true, events.sortedWith(compareBy<CalendarEvent> { it.begin }.thenBy { !it.allDay }))
    }

    /** All-day instances are stored as UTC midnights; they span local days, not UTC ones. */
    private fun instant(ms: Long, allDay: Boolean, zone: ZoneId): Instant {
        val t = Instant.ofEpochMilli(ms)
        return if (allDay) t.atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant() else t
    }
}
