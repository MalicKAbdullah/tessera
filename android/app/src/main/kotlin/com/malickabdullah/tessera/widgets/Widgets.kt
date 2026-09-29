package com.malickabdullah.tessera.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import com.malickabdullah.tessera.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Time and date are TextClocks, so this widget never needs a refresh. */
class ClockWidget : TesseraWidget() {
    override val kind = "clock"
    override val layout = R.layout.widget_clock
    override val valueSizeSp = 44f
    override fun text(context: Context, prefs: SharedPreferences): TileText? = null
}

class CalendarWidget : TesseraWidget() {
    override val kind = "calendar"
    override val layout = R.layout.widget_calendar
    override val valueSizeSp = 44f
    override fun text(context: Context, prefs: SharedPreferences): TileText? = null
}

/** Reads the sticky battery broadcast at draw time, so every redraw is current. */
class BatteryWidget : TesseraWidget() {
    override val kind = "battery"
    override fun text(context: Context, prefs: SharedPreferences): TileText {
        val status = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return TileText("BATTERY", "—", null)
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 /
            status.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val plugged = status.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        return TileText(
            if (plugged) "CHARGING" else "BATTERY",
            "$level%",
            if (plugged) "Plugged in" else "On battery",
            level / 100f,
        )
    }
}

class WeatherWidget : TesseraWidget() {
    override val kind = "weather"
    override fun text(context: Context, prefs: SharedPreferences): TileText {
        val city = prefs.getString("weather_city", "").orEmpty()
        if (city.isEmpty()) return TileText("WEATHER", "—", "Open Tessera to set a city")
        return TileText(
            city.uppercase(),
            prefs.getString("weather_temp", null) ?: "—",
            prefs.getString("weather_condition", null) ?: "Updating…",
        )
    }
}

/** Days are computed at draw time from the stored date, never cached. */
class CountdownWidget : TesseraWidget() {
    override val kind = "countdown"
    override fun text(context: Context, prefs: SharedPreferences): TileText {
        val title = prefs.getString("countdown_title", null) ?: "New Year"
        val target = prefs.getString("countdown_target", null)?.split("-")?.map { it.toInt() }
            ?.let { LocalDate.of(it[0], it[1], it[2]) }
            ?: LocalDate.now().plusYears(1).withDayOfYear(1)
        val days = ChronoUnit.DAYS.between(LocalDate.now(), target)
        val caption = when {
            days == 0L -> "is today"
            days == 1L -> "day to go"
            days == -1L -> "day ago"
            days > 1 -> "days to go"
            else -> "days ago"
        }
        return TileText(title.uppercase(), "${kotlin.math.abs(days)}", caption)
    }
}

class NoteWidget : TesseraWidget() {
    override val kind = "note"
    override val layout = R.layout.widget_note
    override val valueSizeSp = 20f
    override fun text(context: Context, prefs: SharedPreferences): TileText {
        val author = prefs.getString("note_author", null) ?: "Dieter Rams"
        return TileText(
            null,
            prefs.getString("note_text", null) ?: "Less, but better.",
            if (author.isEmpty()) null else "— $author",
        )
    }
}
