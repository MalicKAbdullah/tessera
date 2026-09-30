package com.malickabdullah.tessera.designs.countdown

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Toggle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val countModeToggle = Toggle.Choice(
    "mode",
    "Direction",
    listOf("auto" to "Count down, then up", "up" to "Count up from the date"),
    "auto",
)

/** The primary countdown resolved for today. */
internal class CountdownView(
    val title: String,
    val target: LocalDate,
    val start: LocalDate,
    val days: Long,
    val countUp: Boolean,
) {
    val reading = CountdownMath.reading(days, countUp)
    val span: Long
    val elapsed: Long

    init {
        val (s, e) = CountdownMath.span(start, target, days, countUp)
        span = s
        elapsed = e
    }

    val progress: Float get() = elapsed.toFloat() / span

    /** The big figure: the day count, or a word when the date is today. */
    val hero: String get() = if (days == 0L) "Today" else reading.number.toString()
}

/** The user's countdown title and date; next New Year until a date is picked. */
private fun SceneInputs.primary(): CountdownMath.Item {
    val content = data.content
    return CountdownMath.Item(
        content.countdownTitle.ifBlank { "Countdown" },
        content.countdownDate ?: LocalDate.of(now.toLocalDate().year + 1, 1, 1),
    )
}

/** The primary countdown with its count mode; only designs declaring [countModeToggle] call it. */
internal fun SceneInputs.countdown(): CountdownView {
    val primary = primary()
    return CountdownView(
        title = primary.title,
        target = primary.date,
        start = data.content.countdownStart ?: primary.date.minusYears(1),
        days = CountdownMath.days(now.toLocalDate(), primary.date),
        countUp = choice(countModeToggle.key) == "up",
    )
}

internal fun SceneInputs.countdownItems(): List<CountdownMath.Item> =
    listOf(primary()) + data.content.events.map { CountdownMath.Item(it.title, it.date) }

private val shortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

internal fun LocalDate.short(): String = format(shortDate)
internal fun LocalDate.dayMonth(): String = format(dayMonth)
internal fun LocalDate.weekday(): String = format(weekday).uppercase()

internal fun Paint.measurer(): (String) -> Float = { measureText(it) }

/** Small monospaced caps used for labels. */
internal fun Scene.tag(size: Float, color: Int = ink(0.55f), align: Paint.Align = Paint.Align.LEFT): Paint =
    paint(size * k, color, font = "mono", weight = 500, align = align, tracking = 0.14f)

/** A row of label/value columns under a hairline. */
internal fun Scene.statRow(r: RectF, items: List<Pair<String, String>>) {
    canvas.drawLine(r.left, r.top, r.right, r.top, stroke(ink(0.1f), 1f))
    val colW = r.width() / items.size
    val label = tag(8.5f, ink(0.5f), Paint.Align.CENTER)
    val value = paint(15f * k, text, align = Paint.Align.CENTER)
    items.forEachIndexed { i, (l, v) ->
        val x = r.left + colW * i + colW / 2f
        canvas.drawText(l, x, r.top + 15f, label)
        canvas.drawText(TextFit.ellipsize(v, colW - 6f, value.measurer()), x, r.top + 34f, value)
    }
}
