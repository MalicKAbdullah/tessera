package com.malickabdullah.tessera.designs.calendar

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.CalendarEvent
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import kotlin.math.min

object CalendarMatrix : WidgetDesign {
    override val id = "calendar.matrix"
    override val category = Category.CALENDAR
    override val name = "Dot Date"
    override val blurb = "Today's date lit on a 5×7 LED matrix, with the next events beside it."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("dot", 700, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF0C0C0D, radius = 30f, padding = 16f)
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CALENDAR)

    override fun liveKey(scene: SceneInputs): String {
        val now = scene.now.toInstant()
        return "${CalendarKit.key(scene)}|${CalendarMath.upcoming(scene.data.calendar.events, now).size}"
    }

    override fun draw(s: Scene) {
        val b = s.box
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.06f))
        val today = s.now.toLocalDate()
        val wide = s.w >= s.h * 1.5f
        val left = if (wide) RectF(b.left, b.top, b.left + b.width() * 0.5f, b.bottom) else b

        val head = CalendarKit.mono(s, 9.5f, s.ink(0.75f), weight = 700)
        s.canvas.drawText(CalendarKit.weekday(today.dayOfWeek, TextStyle.FULL).uppercase(), left.left, left.top + 9f * s.k, head)
        s.canvas.drawText(CalendarKit.month(today, TextStyle.SHORT).uppercase(), left.right, left.top + 9f * s.k, CalendarKit.mono(s, 9.5f, s.accent, Paint.Align.RIGHT, weight = 700))

        val footer = 30f * s.k
        val area = RectF(left.left, left.top + 18f * s.k, left.right, left.bottom - footer)
        drawMatrix(s, area, "%02d".format(today.dayOfMonth))

        val week = s.now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val meta = CalendarKit.mono(s, 7.5f, s.ink(0.45f), tracking = 0.14f)
        s.canvas.drawText("WK %02d · D %03d".format(week, today.dayOfYear), left.left, left.bottom - footer + 12f * s.k, meta)

        val now = s.now.toInstant()
        val upcoming = CalendarMath.upcoming(s.data.calendar.events, now)
        if (wide) {
            val x = left.right + 16f
            s.canvas.drawLine(x - 8f, b.top + 4f, x - 8f, b.bottom - 4f, s.stroke(s.ink(0.1f), 1f))
            list(s, RectF(x, b.top, b.right, b.bottom), upcoming.take(3))
        } else {
            val base = b.bottom
            val e = upcoming.firstOrNull()
            when {
                !s.data.calendar.granted -> CalendarKit.connectHint(s, b.left, base)
                e == null -> s.canvas.drawText("NO EVENTS AHEAD", b.left, base, CalendarKit.mono(s, 8f, s.ink(0.5f)))
                else -> {
                    s.canvas.drawCircle(b.left + 3f, base - 3f * s.k, 3f, s.fill(e.color))
                    val when_ = if (e.allDay) "ALL DAY" else CalendarKit.time(s, e)
                    val mp = CalendarKit.mono(s, 8.5f, s.accent, weight = 700)
                    s.canvas.drawText(when_, b.left + 10f, base, mp)
                    val tx = b.left + 10f + mp.measureText(when_) + 6f
                    val tp = CalendarKit.mono(s, 8.5f, s.ink(0.85f), tracking = 0.02f)
                    s.canvas.drawText(CalendarKit.fitText(e.title.uppercase(), b.right - tx, tp), tx, base, tp)
                }
            }
        }
    }

    /** Lit and unlit LED cells of [digits], centred in [r]. */
    private fun drawMatrix(s: Scene, r: RectF, digits: String) {
        val (cols, lit) = CalendarMath.matrix(digits)
        val pitch = min(r.width() / cols, r.height() / 7f) * s.hero
        val radius = pitch * 0.4f
        val ox = r.left + pitch / 2f
        val oy = r.centerY() - pitch * 3f
        val on = s.fill(s.text)
        val off = s.fill(s.ink(0.1f))
        for (c in 0 until cols) for (row in 0 until 7) {
            if (c % 6 == 5) continue
            s.canvas.drawCircle(ox + c * pitch, oy + row * pitch, radius, if ((c to row) in lit) on else off)
        }
    }

    private fun list(s: Scene, r: RectF, events: List<CalendarEvent>) {
        s.canvas.drawText("UP NEXT", r.left, r.top + 9f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.5f), weight = 700))
        val rowH = (r.height() - 18f * s.k) / 3f
        for (i in 0 until 3) {
            val top = r.top + 18f * s.k + rowH * i
            val e = events.getOrNull(i)
            if (e == null) {
                s.canvas.drawCircle(r.left + 3f, top + rowH * 0.5f, 2.5f, s.fill(s.ink(0.12f)))
                s.canvas.drawRoundRect(RectF(r.left + 10f, top + rowH * 0.5f - 1.5f, r.left + r.width() * (0.7f - 0.15f * i), top + rowH * 0.5f + 1.5f), 1.5f, 1.5f, s.fill(s.ink(0.07f)))
                if (i == events.size) {
                    val msg = if (s.data.calendar.granted) (if (i == 0) "NOTHING SCHEDULED" else "") else "CONNECT CALENDAR"
                    if (msg.isNotEmpty()) s.canvas.drawText(msg, r.right, top + rowH * 0.5f + 3f, CalendarKit.mono(s, 7.5f, s.ink(0.4f), Paint.Align.RIGHT))
                }
                continue
            }
            s.canvas.drawCircle(r.left + 3f, top + rowH * 0.34f, 3f, s.fill(e.color))
            val day = CalendarMath.dayOf(e, s.now.zone, s.now.toLocalDate())
            val meta = (if (day == s.now.toLocalDate()) "" else "${CalendarKit.dayLabel(s, day)} ") + if (e.allDay) "ALL DAY" else CalendarKit.time(s, e)
            s.canvas.drawText(meta, r.left + 10f, top + rowH * 0.34f + 3f, CalendarKit.mono(s, 8f, s.accent, weight = 700))
            val tp = s.paint(12f * s.k, s.ink(0.9f), font = "grotesk", weight = 500)
            s.canvas.drawText(CalendarKit.fitText(e.title, r.width() - 10f, tp), r.left + 10f, top + rowH * 0.34f + 17f * s.k, tp)
        }
    }
}
