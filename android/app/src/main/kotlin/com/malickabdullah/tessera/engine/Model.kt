package com.malickabdullah.tessera.engine

import org.json.JSONArray
import org.json.JSONObject

/** Ids are shared with the Dart catalog and stored per placed widget, so they never change. */
enum class Category(val id: String, val label: String) {
    CLOCK("clock", "Clock"),
    BATTERY("battery", "Battery"),
    CALENDAR("calendar", "Calendar"),
    WEATHER("weather", "Weather"),
    COUNTDOWN("countdown", "Countdown"),
    NOTE("note", "Note"),
    PHOTO("photo", "Photo"),
    SKY("sky", "Sun & Moon"),
    DEVICE("device", "Device"),
}

/** Launcher footprint a provider is registered at; designs adapt to any resize from there. */
enum class SizeClass(val id: String, val cols: Int, val rows: Int, val widthDp: Int, val heightDp: Int) {
    SMALL("small", 2, 2, 170, 170),
    WIDE("wide", 4, 2, 350, 170),
    LARGE("large", 4, 4, 350, 350),
}

/** Data changes that make a design redraw. */
enum class Signal { BATTERY, WEATHER, CONTENT, ALARM, CALENDAR }

sealed class Toggle(val key: String, val label: String) {
    abstract fun toJson(): JSONObject

    class Switch(key: String, label: String, val default: Boolean) : Toggle(key, label) {
        override fun toJson(): JSONObject =
            JSONObject().put("key", key).put("label", label).put("type", "switch").put("default", default)
    }

    class Choice(key: String, label: String, val options: List<Pair<String, String>>, val default: String) :
        Toggle(key, label) {
        override fun toJson(): JSONObject = JSONObject()
            .put("key", key).put("label", label).put("type", "choice").put("default", default)
            .put(
                "options",
                JSONArray(options.map { (value, text) -> JSONObject().put("value", value).put("label", text) }),
            )
    }
}

/** Shared toggle: follow the system's 12/24-hour setting or force one. */
val hourFormatToggle = Toggle.Choice(
    "hours",
    "Hour format",
    listOf("system" to "System", "12" to "12-hour", "24" to "24-hour"),
    "system",
)

/**
 * One widget design. Register it in [Registry] and it appears in the gallery,
 * the pin flow and the configure screen with no other wiring.
 */
interface WidgetDesign {
    val id: String
    val category: Category
    val name: String
    val blurb: String
    val sizes: List<SizeClass>
    val defaults: Style
    val toggles: List<Toggle> get() = emptyList()
    val signals: Set<Signal> get() = emptySet()

    /** Human-readable description of any motion, shown in the gallery. */
    val motion: String? get() = null

    /**
     * Identifies what the bitmap currently shows for time-varying parts that
     * no TextClock covers (e.g. "hour 14", "battery 83 charging"). The live
     * ticker redraws an instance only when this changes; null means the
     * bitmap never goes stale on its own.
     */
    fun liveKey(scene: SceneInputs): String? = null

    fun draw(s: Scene)
}
