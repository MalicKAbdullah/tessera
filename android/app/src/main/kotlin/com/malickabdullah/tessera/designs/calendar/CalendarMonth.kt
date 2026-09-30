package com.malickabdullah.tessera.designs.calendar

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.min

object CalendarMonth : WidgetDesign {
    override val id = "calendar.month"
    override val category = Category.CALENDAR
    override val name = "Month Grid"
    override val blurb = "The whole month with today ringed and a dot under every day that has events."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF111214, radius = 30f, padding = 14f)
    override val toggles = listOf(hourFormatToggle, Toggle.Switch("weekends", "Dim weekends", true))
    override val signals = setOf(Signal.CALENDAR)

    override fun liveKey(scene: SceneInputs) = CalendarKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        when {
            s.w >= s.h * 1.5f -> {
                val split = b.left + b.width() * 0.4f
                todayPanel(s, RectF(b.left, b.top, split - 12f, b.bottom))
                s.canvas.drawLine(split, b.top + 4f, split, b.bottom - 4f, s.stroke(s.ink(0.1f), 1f))
                grid(s, RectF(split + 12f, b.top, b.right, b.bottom), header = true)
            }
            s.h >= 300f -> {
                val gridBottom = b.top + b.height() * 0.64f
                grid(s, RectF(b.left, b.top, b.right, gridBottom), header = true)
                s.canvas.drawLine(b.left, gridBottom + 8f, b.right, gridBottom + 8f, s.stroke(s.ink(0.1f), 1f))
                events(s, RectF(b.left, gridBottom + 18f, b.right, b.bottom))
            }
            else -> grid(s, b, header = true)
        }
    }

    private fun grid(s: Scene, r: RectF, header: Boolean) {
        val today = s.now.toLocalDate()
        val month = YearMonth.from(today)
        val weeks = CalendarMath.monthGrid(month, CalendarKit.firstDay)
        val days = CalendarMath.eventDays(s.data.calendar.events, s.now.zone)
        var top = r.top
        if (header) {
            val p = CalendarKit.mono(s, 10f, s.accent, weight = 700)
            s.canvas.drawText(CalendarKit.month(today).uppercase(), r.left, top + 9f * s.k, p)
            s.canvas.drawText("${month.year}", r.right, top + 9f * s.k, CalendarKit.mono(s, 10f, s.ink(0.4f), Paint.Align.RIGHT))
            top += 18f * s.k
        }
        val cw = r.width() / 7f
        val labelH = 13f * s.k
        val labels = CalendarMath.weekdays(CalendarKit.firstDay)
        labels.forEachIndexed { i, d ->
            val p = CalendarKit.mono(s, 7.5f, s.ink(if (weekend(d)) 0.3f else 0.45f), Paint.Align.CENTER, tracking = 0.05f)
            s.canvas.drawText(CalendarKit.weekday(d, TextStyle.NARROW).uppercase(), r.left + cw * (i + 0.5f), top + 8f * s.k, p)
        }
        top += labelH
        val ch = (r.bottom - top) / weeks.size
        val size = min(cw, ch) * 0.46f
        weeks.forEachIndexed { row, week ->
            week.forEachIndexed { col, date ->
                if (date == null) return@forEachIndexed
                val cx = r.left + cw * (col + 0.5f)
                val cy = top + ch * row + ch * 0.44f
                val isToday = date == today
                if (isToday) s.canvas.drawCircle(cx, cy, min(cw, ch) * 0.42f, s.stroke(s.accent, 1.6f))
                val alpha = when {
                    isToday -> 1f
                    date.isBefore(today) -> 0.3f
                    s.flag("weekends") && weekend(date.dayOfWeek) -> 0.55f
                    else -> 0.88f
                }
                val p = s.paint(size, if (isToday) s.accent else s.ink(alpha), align = Paint.Align.CENTER)
                s.textMid("${date.dayOfMonth}", cx, cy, p)
                days[date]?.let { colors ->
                    val dots = colors.distinct().take(3)
                    val dr = min(1.6f, ch * 0.06f)
                    val gap = dr * 3f
                    val y = cy + min(cw, ch) * 0.42f - dr * 0.2f
                    val x0 = cx - gap * (dots.size - 1) / 2f
                    dots.forEachIndexed { i, c ->
                        val color = if (date.isBefore(today)) s.ink(0.25f) else c
                        s.canvas.drawCircle(x0 + gap * i, if (isToday) y + dr * 2.2f else y, dr, s.fill(color))
                    }
                }
            }
        }
    }

    private fun todayPanel(s: Scene, r: RectF) {
        val today = s.now.toLocalDate()
        s.canvas.drawText(CalendarKit.weekday(today.dayOfWeek, TextStyle.FULL).uppercase(), r.left, r.top + 9f * s.k, CalendarKit.mono(s, 9.5f, s.ink(0.6f)))
        val numArea = RectF(r.left, r.top + 16f, r.right, r.top + r.height() * 0.58f)
        val num = s.paint(s.fit("28", numArea.width(), numArea.height()) * s.hero, s.text)
        s.textMid("${today.dayOfMonth}", r.left - 2f, numArea.centerY(), num)
        val next = CalendarMath.nextTimed(s.data.calendar.events, s.now.toInstant())
            ?.takeIf { it.begin.atZone(s.now.zone).toLocalDate() <= today.plusDays(1) }
        val base = r.bottom - 2f
        when {
            !s.data.calendar.granted -> CalendarKit.connectHint(s, r.left, base)
            next == null -> {
                s.canvas.drawText("NOTHING NEXT", r.left, base - 16f * s.k, CalendarKit.mono(s, 8f, s.ink(0.4f)))
                s.canvas.drawText("A clear day", r.left, base, s.paint(12f * s.k, s.ink(0.75f), weight = 400))
            }
            else -> {
                CalendarKit.chip(s.canvas, r.left, base - 24f * s.k, base, next.color, s)
                val day = next.begin.atZone(s.now.zone).toLocalDate()
                val label = if (day == today) CalendarKit.time(s, next) else "TMRW ${CalendarKit.time(s, next)}"
                s.canvas.drawText(label, r.left + 8f, base - 14f * s.k, CalendarKit.mono(s, 8.5f, s.accent))
                val tp = s.paint(12f * s.k, s.ink(0.9f), weight = 400)
                s.canvas.drawText(CalendarKit.fitText(next.title, r.width() - 8f, tp), r.left + 8f, base, tp)
            }
        }
    }

    private fun events(s: Scene, r: RectF) {
        val upcoming = CalendarMath.upcoming(s.data.calendar.events, s.now.toInstant()).take(3)
        val rowH = r.height() / 3f
        if (upcoming.isEmpty()) {
            for (i in 0 until 3) {
                val y = r.top + rowH * i + rowH / 2f
                s.canvas.drawRoundRect(RectF(r.left, y - 7f, r.left + 3f, y + 7f), 1.5f, 1.5f, s.fill(s.ink(0.1f)))
                s.canvas.drawRoundRect(RectF(r.left + 12f, y - 2f, r.left + r.width() * (0.55f - 0.12f * i), y + 2f), 2f, 2f, s.fill(s.ink(0.07f)))
            }
            val msg = if (s.data.calendar.granted) "NO UPCOMING EVENTS" else "CONNECT CALENDAR"
            s.canvas.drawText(msg, r.right, r.top + rowH / 2f + 3f, CalendarKit.mono(s, 8.5f, s.ink(0.45f), Paint.Align.RIGHT))
            return
        }
        upcoming.forEachIndexed { i, e ->
            val cy = r.top + rowH * i + rowH / 2f
            CalendarKit.chip(s.canvas, r.left, cy - 9f, cy + 9f, e.color, s)
            val date = CalendarMath.dayOf(e, s.now.zone, s.now.toLocalDate())
            val meta = if (e.allDay) CalendarKit.dayLabel(s, date) else "${CalendarKit.dayLabel(s, date)} · ${CalendarKit.time(s, e)}"
            val mp = CalendarKit.mono(s, 8f, s.ink(0.5f))
            s.canvas.drawText(meta, r.left + 11f, cy - 2f, mp)
            val tp = s.paint(12.5f * s.k, s.ink(0.92f), weight = 400)
            s.canvas.drawText(CalendarKit.fitText(e.title, r.width() - 11f, tp), r.left + 11f, cy + 11f * s.k, tp)
        }
    }

    private fun weekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY
}
