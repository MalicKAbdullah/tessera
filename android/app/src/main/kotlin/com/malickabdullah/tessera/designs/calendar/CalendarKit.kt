package com.malickabdullah.tessera.designs.calendar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.CalendarEvent
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal object CalendarKit {
    /** Redraw when the day or the events change; every calendar bitmap shows today's date. */
    fun key(s: SceneInputs): String = "${s.now.toLocalDate()}|${s.data.calendar.fingerprint}"

    val firstDay: DayOfWeek get() = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    fun weekday(d: DayOfWeek, style: TextStyle = TextStyle.SHORT): String = d.getDisplayName(style, Locale.getDefault())

    fun month(d: LocalDate, style: TextStyle = TextStyle.FULL): String = d.month.getDisplayName(style, Locale.getDefault())

    fun mono(s: Scene, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT, weight: Int = 500, tracking: Float = 0.12f) =
        s.paint(size * s.k, color, font = "mono", weight = weight, align = align, tracking = tracking)

    /** "09:30" or "9:30 AM" for an event's start in the widget's zone. */
    fun time(s: Scene, e: CalendarEvent): String = time(s, e.begin)

    fun time(s: Scene, at: Instant): String {
        val (t, suffix) = CalendarMath.clock(at.atZone(s.now.zone).toLocalTime(), s.use24h)
        return if (suffix.isEmpty()) t else "$t $suffix"
    }

    /** "TODAY", "TOMORROW" or "THU 3". */
    fun dayLabel(s: Scene, d: LocalDate): String {
        val today = s.now.toLocalDate()
        return when (d) {
            today -> "TODAY"
            today.plusDays(1) -> "TOMORROW"
            else -> "${weekday(d.dayOfWeek).uppercase()} ${d.dayOfMonth}"
        }
    }

    /** Title shortened to the pixel width available. */
    fun fitText(text: String, maxW: Float, p: Paint): String {
        if (p.measureText(text) <= maxW) return text
        var n = text.length
        while (n > 1 && p.measureText(text.take(n).trimEnd() + "…") > maxW) n--
        return text.take(n).trimEnd() + "…"
    }

    /** Vertical colour chip of an event. */
    fun chip(c: Canvas, x: Float, top: Float, bottom: Float, color: Int, s: Scene) {
        c.drawRoundRect(RectF(x, top, x + 3f, bottom), 1.5f, 1.5f, s.fill(color))
    }

    /** A small calendar glyph: a rounded page with two rings and a header bar. */
    fun glyph(c: Canvas, cx: Float, cy: Float, size: Float, color: Int, s: Scene) {
        val r = RectF(cx - size / 2f, cy - size * 0.42f, cx + size / 2f, cy + size * 0.5f)
        c.drawRoundRect(r, size * 0.16f, size * 0.16f, s.stroke(color, size * 0.1f))
        c.drawLine(r.left, r.top + size * 0.28f, r.right, r.top + size * 0.28f, s.stroke(color, size * 0.1f))
        c.drawLine(r.left + size * 0.28f, r.top - size * 0.12f, r.left + size * 0.28f, r.top + size * 0.1f, s.stroke(color, size * 0.12f))
        c.drawLine(r.right - size * 0.28f, r.top - size * 0.12f, r.right - size * 0.28f, r.top + size * 0.1f, s.stroke(color, size * 0.12f))
    }

    /** One-line hint with the calendar glyph, drawn only when calendar access is missing. */
    fun connectHint(s: Scene, x: Float, baseline: Float, size: Float = 8.5f) {
        if (s.data.calendar.granted) return
        val p = mono(s, size, s.ink(0.42f))
        val g = size * s.k * 1.1f
        glyph(s.canvas, x + g / 2f, baseline - s.capHeight(p) / 2f, g, s.ink(0.42f), s)
        s.canvas.drawText("CONNECT CALENDAR", x + g + 5f, baseline, p)
    }
}
