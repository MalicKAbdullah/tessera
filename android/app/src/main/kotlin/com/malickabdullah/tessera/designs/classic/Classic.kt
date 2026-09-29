package com.malickabdullah.tessera.designs.classic

import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The v0.1 tile — label, hero value, caption — on the engine. These keep
 * v0.1 widgets working and are the starting point for each category's
 * full design set.
 */
private val classicStyle = Style.of("sans", 300, text = 0xFFF3F1EC, accent = 0xFFC9A77C, background = 0xFF1C1D20, opacity = 0.92f, radius = 24f, padding = 20f)

private fun Scene.label(text: String) =
    canvas.drawText(text.uppercase(), box.left, box.top + 10f * k, paint(11f * k, accent, weight = 500, tracking = 0.14f))

private fun Scene.caption(text: String) =
    canvas.drawText(text, box.left, box.bottom - 2f, paint(13f * k, ink(0.66f), weight = 400))

private fun Scene.heroArea() = RectF(box.left, box.top + 16f * k, box.right, box.bottom - 20f * k)

private fun Scene.heroText(text: String) {
    val area = heroArea()
    val p = paint(fit(text, area.width(), area.height()) * hero)
    val fm = p.fontMetrics
    canvas.drawText(text, area.left, area.centerY() - (fm.ascent + fm.descent) / 2f, p)
}

object CalendarClassic : WidgetDesign {
    override val id = "calendar.classic"
    override val category = Category.CALENDAR
    override val name = "Classic Day"
    override val blurb = "Month, day and weekday at a glance."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle

    override fun draw(s: Scene) {
        val b = s.box
        val area = s.heroArea()
        s.textClock(RectF(b.left, b.top, b.right, b.top + 16f * s.k), "MMMM" to "MMMM", 11f * s.k, s.accent, weight = 500, gravity = Gravity.START or Gravity.TOP, caps = true)
        s.textClock(area, "d" to "d", s.fit("28", area.width(), area.height()) * s.hero, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        s.textClock(RectF(b.left, b.bottom - 18f * s.k, b.right, b.bottom), "EEEE" to "EEEE", 13f * s.k, s.ink(0.66f), weight = 400, gravity = Gravity.START or Gravity.BOTTOM)
    }
}

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

object CountdownClassic : WidgetDesign {
    override val id = "countdown.classic"
    override val category = Category.COUNTDOWN
    override val name = "Classic Countdown"
    override val blurb = "Days until the date that matters."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val content = s.data.content
        val today = s.now.toLocalDate()
        val target = content.countdownDate ?: LocalDate.of(today.year + 1, 1, 1)
        val days = ChronoUnit.DAYS.between(today, target)
        s.label(content.countdownTitle)
        s.heroText("${abs(days)}")
        s.caption(
            when {
                days == 0L -> "is today"
                days == 1L -> "day to go"
                days == -1L -> "day ago"
                days > 1 -> "days to go"
                else -> "days ago"
            },
        )
    }
}

object NoteClassic : WidgetDesign {
    override val id = "note.classic"
    override val category = Category.NOTE
    override val name = "Classic Note"
    override val blurb = "A line worth keeping in view, with optional attribution."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle
    override val signals = setOf(Signal.CONTENT)

    override fun draw(s: Scene) {
        val content = s.data.content
        val b = s.box
        val author = content.noteAuthor
        val bottom = if (author.isEmpty()) b.bottom else b.bottom - 20f * s.k
        val paint = TextPaint(s.paint(20f * s.k))
        val layout = StaticLayout.Builder.obtain(content.note, 0, content.note.length, paint, b.width().toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(((bottom - b.top) / (paint.fontSpacing * 1.15f)).toInt().coerceAtLeast(1))
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        s.canvas.save()
        s.canvas.translate(b.left, b.top + (bottom - b.top - layout.height) / 2f)
        layout.draw(s.canvas)
        s.canvas.restore()
        if (author.isNotEmpty()) s.caption("— $author")
    }
}
