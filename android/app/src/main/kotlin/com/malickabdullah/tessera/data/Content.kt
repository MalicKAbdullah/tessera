package com.malickabdullah.tessera.data

import android.app.AlarmManager
import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

data class City(val name: String, val region: String, val latitude: Double, val longitude: Double) {
    /** Identifies the place a cached forecast belongs to. */
    val key: String get() = "$latitude,$longitude"
}

data class CountdownEvent(val title: String, val date: LocalDate)

data class ChecklistItem(val text: String, val done: Boolean)

/** User-entered content, pushed from Dart as the `WidgetContent` JSON. */
data class Content(
    val note: String,
    val noteAuthor: String,
    val countdownTitle: String,
    /** Null until the user picks a date; designs then count to next New Year. */
    val countdownDate: LocalDate?,
    /** The day the target was chosen: where a progress ring starts. Null until a date is picked. */
    val countdownStart: LocalDate?,
    /** Further events for the multi-countdown list. */
    val events: List<CountdownEvent>,
    val checklist: List<ChecklistItem>,
    val city: City?,
)

object ContentSource : DataSource<Content> {
    private const val PREFS = "tessera.content"
    private const val KEY = "content"

    /** Content before the app first pushes any: the same defaults the Dart model starts with. */
    private val INITIAL = Content("Less, but better.", "Dieter Rams", "New Year", null, null, emptyList(), emptyList(), null)

    override fun read(context: Context): Content {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return INITIAL
        val json = JSONObject(raw)
        return Content(
            note = json.getString("note"),
            noteAuthor = json.getString("noteAuthor"),
            countdownTitle = json.getString("countdownTitle"),
            countdownDate = if (json.isNull("countdownDate")) {
                null
            } else {
                LocalDate.parse(json.getString("countdownDate").substring(0, 10))
            },
            // Content saved before these fields existed simply has none.
            countdownStart = json.optString("countdownStart").takeIf { it.isNotEmpty() && it != "null" }?.let { LocalDate.parse(it.substring(0, 10)) },
            events = json.optJSONArray("events")?.let { arr ->
                (0 until arr.length()).map { i ->
                    arr.getJSONObject(i).let { CountdownEvent(it.getString("title"), LocalDate.parse(it.getString("date").substring(0, 10))) }
                }
            } ?: emptyList(),
            checklist = json.optJSONArray("checklist")?.let { arr ->
                (0 until arr.length()).map { i -> arr.getJSONObject(i).let { ChecklistItem(it.getString("text"), it.getBoolean("done")) } }
            } ?: emptyList(),
            city = if (json.isNull("city")) {
                null
            } else {
                json.getJSONObject("city").let {
                    City(it.getString("name"), it.getString("region"), it.getDouble("latitude"), it.getDouble("longitude"))
                }
            },
        )
    }

    fun write(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json).apply()
    }
}

data class NextAlarm(val at: Instant)

object AlarmSource : DataSource<NextAlarm?> {
    override fun read(context: Context): NextAlarm? =
        context.getSystemService(AlarmManager::class.java).nextAlarmClock?.let { NextAlarm(Instant.ofEpochMilli(it.triggerTime)) }
}
