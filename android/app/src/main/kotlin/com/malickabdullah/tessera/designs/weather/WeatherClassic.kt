package com.malickabdullah.tessera.designs.weather

import com.malickabdullah.tessera.designs.classic.caption
import com.malickabdullah.tessera.designs.classic.classicStyle
import com.malickabdullah.tessera.designs.classic.heroText
import com.malickabdullah.tessera.designs.classic.label
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.Duration

object WeatherClassic : WidgetDesign {
    override val id = "weather.classic"
    override val category = Category.WEATHER
    override val name = "Classic Conditions"
    override val blurb = "Temperature and conditions for your city, with how fresh the reading is."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    override fun liveKey(scene: SceneInputs): String {
        val w = scene.data.weather ?: return "none|${scene.data.content.city?.key}"
        return "${w.fetchedAt}|${ago(Duration.between(w.fetchedAt, scene.now.toInstant()))}"
    }

    override fun draw(s: Scene) {
        val city = s.data.content.city
        val w = s.data.weather
        when {
            city == null -> {
                s.label("Weather")
                s.heroText("—")
                s.caption("Open Tessera to set a city")
            }
            w == null -> {
                s.label(city.name)
                s.heroText("—")
                s.caption("Fetching the forecast…")
            }
            else -> {
                s.label(city.name)
                s.heroText("${w.temperature.toInt()}°")
                s.caption("${w.condition} · feels ${w.feelsLike.toInt()}° · ${ago(Duration.between(w.fetchedAt, s.now.toInstant()))}")
            }
        }
    }

    private fun ago(d: Duration): String = when {
        d.toMinutes() < 1 -> "just now"
        d.toMinutes() < 5 -> "<5m ago"
        d.toMinutes() < 60 -> "${d.toMinutes() / 5 * 5}m ago"
        else -> "${d.toHours()}h ago"
    }
}
