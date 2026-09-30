package com.malickabdullah.tessera.designs.calendar

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.CalendarEvent
import com.malickabdullah.tessera.data.CalendarSource
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

object CalendarNext : WidgetDesign {
    override val id = "calendar.next"
    override val category = Category.CALENDAR
    override val name = "Next Up"
    override val blurb = "A countdown to your next event, with its time and place; the wide size cycles through what follows."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of(
        "sans", 300, text = 0xFFF4F4F2, accent = 0xFFD4FF3A, background = 0xFF17191D,
        kind = BgKind.GRADIENT, background2 = 0xFF262A31, radius = 30f, padding = 16f,
    )
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CALENDAR)
    override val motion = "On the wide size, the events after the next one take turns every five seconds."

    private const val FOLLOWING = 3

    override fun liveKey(scene: SceneInputs): String {
        val now = scene.now.toInstant()
        val next = CalendarMath.nextTimed(scene.data.calendar.events, now)
        return "${CalendarKit.key(scene)}|${next?.let { CalendarMath.countdown(it, now).text }}"
    }

    override fun draw(s: Scene) {
        val b = s.box
        val wide = s.w >= s.h * 1.5f
        val main = if (wide) RectF(b.left, b.top, b.left + b.width() * 0.54f, b.bottom) else b
        val now = s.now.toInstant()
        val events = s.data.calendar.events
        val next = CalendarMath.nextTimed(events, now)
        header(s, main, next)
        when {
            next != null -> countdown(s, main, next)
            else -> idle(s, main)
        }
        if (wide) {
            val split = main.right + 14f
            s.canvas.drawLine(split, b.top + 4f, split, b.bottom - 4f, s.stroke(s.ink(0.1f), 1f))
            val after = CalendarMath.upcoming(events, now).filter { it != next }.take(FOLLOWING)
            following(s, RectF(split + 14f, b.top, b.right, b.bottom), after)
        }
    }

    private fun header(s: Scene, r: RectF, next: CalendarEvent?) {
        val started = next != null && !next.begin.isAfter(s.now.toInstant())
        s.canvas.drawCircle(r.left + 3f, r.top + 5f * s.k, 3f, s.fill(if (next != null) s.accent else s.ink(0.25f)))
        val label = if (started) "HAPPENING NOW" else "NEXT UP"
        s.canvas.drawText(label, r.left + 11f, r.top + 8.5f * s.k, CalendarKit.mono(s, 8.5f, if (next != null) s.accent else s.ink(0.5f), weight = 700))
        if (next != null) {
            val day = CalendarMath.dayOf(next, s.now.zone, s.now.toLocalDate())
            val tag = if (day == s.now.toLocalDate()) CalendarKit.time(s, next) else CalendarKit.dayLabel(s, day)
            s.canvas.drawText(tag, r.right, r.top + 8.5f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.5f), Paint.Align.RIGHT))
        }
    }

    private fun countdown(s: Scene, r: RectF, e: CalendarEvent) {
        val c = CalendarMath.countdown(e, s.now.toInstant())
        val top = r.top + 20f * s.k
        val footer = 40f * s.k
        val area = RectF(r.left, top, r.right, r.bottom - footer)
        val unitP = CalendarKit.mono(s, 9f, s.ink(0.6f), weight = 700)
        val tailP = CalendarKit.mono(s, 8f, s.ink(0.4f))
        val tail = if (c.started) "LEFT" else "TO GO"
        val unitW = maxOf(unitP.measureText(c.unit), tailP.measureText(tail)) + 5f
        val size = s.fit(c.value, area.width() - unitW, area.height()) * s.hero
        val num = s.paint(size)
        val cap = s.capHeight(num)
        val y = area.centerY() + cap / 2f
        s.canvas.drawText(c.value, r.left - 1f, y, num)
        val x = r.left + num.measureText(c.value) + 5f
        s.canvas.drawText(c.unit, x, y - cap + s.capHeight(unitP), unitP)
        s.canvas.drawText(tail, x, y, tailP)

        val base = r.bottom
        CalendarKit.chip(s.canvas, r.left, base - 30f * s.k, base, e.color, s)
        val tp = s.paint(13f * s.k, s.text, weight = 500)
        s.canvas.drawText(CalendarKit.fitText(e.title, r.width() - 9f, tp), r.left + 9f, base - 17f * s.k, tp)
        val span = "${CalendarKit.time(s, e)} – ${CalendarKit.time(s, e.end)}" + if (e.location.isNotEmpty()) " · ${e.location}" else ""
        val mp = CalendarKit.mono(s, 8f, s.ink(0.5f), tracking = 0.04f)
        s.canvas.drawText(CalendarKit.fitText(span, r.width() - 9f, mp), r.left + 9f, base - 2f, mp)
        if (c.started) {
            val total = (e.end.epochSecond - e.begin.epochSecond).coerceAtLeast(1)
            val done = (s.now.toEpochSecond() - e.begin.epochSecond).toFloat() / total
            val bar = RectF(r.left + 9f, base - 34f * s.k, r.right, base - 32f * s.k)
            s.canvas.drawRoundRect(bar, 1f, 1f, s.fill(s.ink(0.12f)))
            s.canvas.drawRoundRect(RectF(bar.left, bar.top, bar.left + bar.width() * done.coerceIn(0f, 1f), bar.bottom), 1f, 1f, s.fill(s.accent))
        }
    }

    /** No timed event ahead: the date carries the widget, with today's all-day events or a clear-days line. */
    private fun idle(s: Scene, r: RectF) {
        val today = s.now.toLocalDate()
        val area = RectF(r.left, r.top + 20f * s.k, r.right, r.bottom - 40f * s.k)
        val num = s.paint(s.fit("28", area.width() * 0.5f, area.height()) * s.hero)
        val y = area.centerY() + s.capHeight(num) / 2f
        s.canvas.drawText("${today.dayOfMonth}", r.left - 1f, y, num)
        val x = r.left + num.measureText("${today.dayOfMonth}") + 6f
        s.canvas.drawText(CalendarKit.weekday(today.dayOfWeek, TextStyle.FULL).uppercase(), x, y - s.capHeight(num) + 8f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.8f), weight = 700))
        s.canvas.drawText(CalendarKit.month(today).uppercase(), x, y - s.capHeight(num) + 20f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.45f)))

        val base = r.bottom
        val cal = s.data.calendar
        if (!cal.granted) {
            CalendarKit.connectHint(s, r.left, base - 2f)
            s.canvas.drawText("See what's next", r.left, base - 17f * s.k, s.paint(13f * s.k, s.ink(0.75f), weight = 400))
            return
        }
        val allDay = cal.events.firstOrNull { it.allDay && CalendarMath.dayOf(it, s.now.zone, today) == today }
        if (allDay != null) {
            CalendarKit.chip(s.canvas, r.left, base - 30f * s.k, base, allDay.color, s)
            val tp = s.paint(13f * s.k, s.text, weight = 500)
            s.canvas.drawText(CalendarKit.fitText(allDay.title, r.width() - 9f, tp), r.left + 9f, base - 17f * s.k, tp)
            s.canvas.drawText("ALL DAY · NOTHING TIMED AHEAD", r.left + 9f, base - 2f, CalendarKit.mono(s, 7.5f, s.ink(0.5f)))
        } else {
            s.canvas.drawText("Free", r.left, base - 17f * s.k, s.paint(13f * s.k, s.text, weight = 500))
            s.canvas.drawText("NEXT ${CalendarSource.WINDOW_DAYS} DAYS CLEAR", r.left, base - 2f, CalendarKit.mono(s, 7.5f, s.ink(0.5f)))
        }
    }

    private fun following(s: Scene, r: RectF, after: List<CalendarEvent>) {
        s.canvas.drawText("THEN", r.left, r.top + 8.5f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.5f), weight = 700))
        val body = RectF(r.left, r.top + 16f * s.k, r.right, r.bottom)
        if (after.isEmpty()) {
            for (i in 0 until 3) {
                val y = body.top + body.height() * (0.25f + 0.25f * i)
                s.canvas.drawRoundRect(RectF(r.left, y - 2f, r.left + r.width() * (0.8f - 0.2f * i), y + 2f), 2f, 2f, s.fill(s.ink(0.06f)))
            }
            val msg = if (s.data.calendar.granted) "NOTHING AFTER" else "CONNECT CALENDAR"
            s.canvas.drawText(msg, r.left, body.bottom - 2f, CalendarKit.mono(s, 7.5f, s.ink(0.4f)))
            return
        }
        if (after.size == 1) {
            card(s.canvas, s, body, after[0], 0, 1)
        } else {
            s.flipper(body, 5000, after.size) { i -> card(this, s, body, after[i], i, after.size) }
        }
    }

    private fun card(c: Canvas, s: Scene, r: RectF, e: CalendarEvent, index: Int, count: Int) {
        val day = CalendarMath.dayOf(e, s.now.zone, s.now.toLocalDate())
        val meta = if (e.allDay) "${CalendarKit.dayLabel(s, day)} · ALL DAY" else "${CalendarKit.dayLabel(s, day)} · ${CalendarKit.time(s, e)}"
        c.drawText(meta, r.left + 9f, r.top + 14f * s.k, CalendarKit.mono(s, 8f, s.accent, tracking = 0.06f))
        val tp = s.paint(14f * s.k, s.text, weight = 500)
        val lines = wrap(e.title, r.width() - 9f, tp, 2)
        lines.forEachIndexed { i, line -> c.drawText(line, r.left + 9f, r.top + (32f + 17f * i) * s.k, tp) }
        CalendarKit.chip(c, r.left, r.top + 4f * s.k, r.top + (36f + 17f * (lines.size - 1)) * s.k, e.color, s)
        if (e.location.isNotEmpty()) {
            val lp = s.paint(10f * s.k, s.ink(0.5f), weight = 400)
            c.drawText(CalendarKit.fitText(e.location, r.width() - 9f, lp), r.left + 9f, r.top + (50f + 17f * (lines.size - 1)) * s.k, lp)
        }
        if (count > 1) {
            for (i in 0 until count) {
                val x = r.left + 12f + i * 9f
                c.drawCircle(x, r.bottom - 3f, 2.2f, s.fill(if (i == index) s.accent else s.ink(0.18f)))
            }
        }
    }

    /** Greedy word wrap into at most [maxLines], ellipsizing the last. */
    private fun wrap(text: String, maxW: Float, p: Paint, maxLines: Int): List<String> {
        val lines = mutableListOf<String>()
        var rest = text.trim()
        while (rest.isNotEmpty() && lines.size < maxLines - 1) {
            val n = p.breakText(rest, true, maxW, null)
            if (n >= rest.length) break
            val cut = rest.lastIndexOf(' ', n).takeIf { it > 0 } ?: n
            lines += rest.substring(0, cut).trimEnd()
            rest = rest.substring(cut).trimStart()
        }
        if (rest.isNotEmpty()) lines += CalendarKit.fitText(rest, maxW, p)
        return lines
    }
}
