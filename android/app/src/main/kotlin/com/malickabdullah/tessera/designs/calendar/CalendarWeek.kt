package com.malickabdullah.tessera.designs.calendar

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.BgKind
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

object CalendarWeek : WidgetDesign {
    override val id = "calendar.week"
    override val category = Category.CALENDAR
    override val name = "Week Strip"
    override val blurb = "This week as seven columns of hour cells, lit where you're busy, with a marker at the current hour."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of(
        "grotesk", 500, text = 0xFFEDEFF2, accent = 0xFF9AD0C2, background = 0xFF15171B,
        kind = BgKind.GRADIENT, background2 = 0xFF22262C, radius = 30f, padding = 16f,
    )
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CALENDAR)

    override fun liveKey(scene: SceneInputs) = "${CalendarKit.key(scene)}|${scene.now.hour}"

    override fun draw(s: Scene) {
        val b = s.box
        val today = s.now.toLocalDate()
        val days = CalendarMath.week(today, CalendarKit.firstDay)
        val large = s.h >= 300f
        val from = if (large) 7 else 8
        val to = if (large) 22 else 20

        val first = days.first()
        val last = days.last()
        val range = "${CalendarKit.month(first, TextStyle.SHORT).uppercase()} ${first.dayOfMonth} – " +
            (if (last.month != first.month) "${CalendarKit.month(last, TextStyle.SHORT).uppercase()} " else "") + "${last.dayOfMonth}"
        s.canvas.drawText(range, b.left, b.top + 9f * s.k, CalendarKit.mono(s, 9f, s.ink(0.8f), weight = 700))
        s.canvas.drawText("WEEK ${s.now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}", b.right, b.top + 9f * s.k, CalendarKit.mono(s, 9f, s.accent, Paint.Align.RIGHT, weight = 700))

        val events = s.data.calendar.events
        val footer = if (large) 44f * s.k else 12f * s.k
        val gutter = if (large) 22f * s.k else 0f
        val grid = RectF(b.left + gutter, b.top + 18f * s.k, b.right, b.bottom - footer)
        val cw = grid.width() / 7f
        val headH = 33f * s.k
        val slots = to - from
        val slotH = (grid.height() - headH) / slots
        val barH = slotH * 0.72f
        val eventDays = CalendarMath.eventDays(events.filter { it.allDay }, s.now.zone)
        var booked = 0f

        days.forEachIndexed { i, d ->
            val cx = grid.left + cw * (i + 0.5f)
            val isToday = d == today
            val past = d.isBefore(today)
            s.canvas.drawText(
                CalendarKit.weekday(d.dayOfWeek, TextStyle.NARROW).uppercase(), cx, grid.top + 8f * s.k,
                CalendarKit.mono(s, 7.5f, if (isToday) s.accent else s.ink(0.45f), Paint.Align.CENTER, weight = 700),
            )
            val numP = s.paint(13f * s.k, if (isToday) s.style.bg.color else s.ink(if (past) 0.4f else 0.9f), align = Paint.Align.CENTER)
            val ny = grid.top + 19f * s.k
            if (isToday) s.canvas.drawCircle(cx, ny, 9f * s.k, s.fill(s.accent))
            s.textMid("${d.dayOfMonth}", cx, ny, numP)
            eventDays[d]?.let { colors ->
                s.canvas.drawRoundRect(RectF(cx - cw * 0.3f, grid.top + headH - 3f, cx + cw * 0.3f, grid.top + headH - 1.5f), 1f, 1f, s.fill(colors.first()))
            }

            val busy = CalendarMath.busyHours(events, d, s.now.zone, from, to)
            booked += busy.sum()
            val bw = min(cw * 0.62f, 26f)
            for (h in 0 until slots) {
                val top = grid.top + headH + slotH * h + (slotH - barH) / 2f
                val cell = RectF(cx - bw / 2f, top, cx + bw / 2f, top + barH)
                val f = busy[h]
                val color = when {
                    f > 0f -> s.ink((0.35f + 0.65f * f) * if (past) 0.5f else 1f, s.accent)
                    isToday && from + h < s.now.hour -> s.ink(0.1f)
                    else -> s.ink(0.06f)
                }
                s.canvas.drawRoundRect(cell, barH / 2.5f, barH / 2.5f, s.fill(color))
            }
            if (isToday && s.now.hour in from until to) {
                val y = grid.top + headH + slotH * (s.now.hour - from + 0.5f)
                s.canvas.drawLine(cx - bw / 2f - 3f, y, cx + bw / 2f + 3f, y, s.stroke(s.text, 1.4f))
                s.canvas.drawCircle(cx - bw / 2f - 3f, y, 2.2f, s.fill(s.text))
            }
        }

        if (large) {
            val lp = CalendarKit.mono(s, 7f, s.ink(0.35f), Paint.Align.LEFT, tracking = 0.02f)
            for (h in from until to step 3) {
                val y = grid.top + headH + slotH * (h - from) + slotH / 2f
                s.textMid(CalendarMath.hourLabel(h, s.use24h), b.left, y, lp)
            }
            footer(s, RectF(b.left, b.bottom - footer + 12f * s.k, b.right, b.bottom), booked)
        }
        if (!s.data.calendar.granted) CalendarKit.connectHint(s, b.left, b.bottom, 7.5f)
    }

    private fun footer(s: Scene, r: RectF, booked: Float) {
        s.canvas.drawLine(r.left, r.top, r.right, r.top, s.stroke(s.ink(0.1f), 1f))
        if (!s.data.calendar.granted) return
        val hours = booked.toInt()
        s.canvas.drawText("BOOKED", r.left, r.top + 14f * s.k, CalendarKit.mono(s, 7.5f, s.ink(0.45f)))
        s.canvas.drawText("${hours}h", r.left, r.bottom, s.paint(15f * s.k, s.text))
        val next = CalendarMath.nextTimed(s.data.calendar.events, s.now.toInstant())
        val x = r.left + r.width() * 0.3f
        s.canvas.drawText("NEXT", x, r.top + 14f * s.k, CalendarKit.mono(s, 7.5f, s.ink(0.45f)))
        val tp = s.paint(12.5f * s.k, s.ink(0.9f), weight = 400)
        val text = if (next == null) {
            "Nothing ahead this fortnight"
        } else {
            "${CalendarKit.dayLabel(s, CalendarMath.dayOf(next, s.now.zone, s.now.toLocalDate()))} ${CalendarKit.time(s, next)} · ${next.title}"
        }
        s.canvas.drawText(CalendarKit.fitText(text, r.right - x, tp), x, r.bottom, tp)
    }
}
