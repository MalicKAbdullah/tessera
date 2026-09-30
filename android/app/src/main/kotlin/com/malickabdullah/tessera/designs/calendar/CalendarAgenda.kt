package com.malickabdullah.tessera.designs.calendar

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.CalendarSource
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.format.TextStyle

object CalendarAgenda : WidgetDesign {
    override val id = "calendar.agenda"
    override val category = Category.CALENDAR
    override val name = "Agenda"
    override val blurb = "Your next events by day, each with its calendar colour, and a line marking now."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("sans", 500, text = 0xFF16171A, accent = 0xFF2F6BFF, background = 0xFFF4F1EA, radius = 30f, padding = 16f)
    override val toggles = listOf(hourFormatToggle, Toggle.Switch("location", "Show location", true))
    override val signals = setOf(Signal.CALENDAR)

    /** The now line moves whenever an event starts or ends, so the key tracks which events are running or over. */
    override fun liveKey(scene: SceneInputs): String {
        val now = scene.now.toInstant()
        val events = scene.data.calendar.events
        return "${CalendarKit.key(scene)}|${events.count { it.end <= now }}|${events.count { it.begin <= now }}"
    }

    override fun draw(s: Scene) {
        val b = s.box
        val today = s.now.toLocalDate()
        val headH = 30f * s.k
        val num = s.paint(26f * s.k, s.accent, weight = 700)
        s.canvas.drawText("${today.dayOfMonth}", b.left - 1f, b.top + 23f * s.k, num)
        val nx = b.left + num.measureText("${today.dayOfMonth}") + 7f
        s.canvas.drawText(CalendarKit.weekday(today.dayOfWeek, TextStyle.FULL).uppercase(), nx, b.top + 11f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.85f), weight = 700))
        s.canvas.drawText(CalendarKit.month(today).uppercase(), nx, b.top + 22f * s.k, CalendarKit.mono(s, 8.5f, s.ink(0.45f)))

        val cal = s.data.calendar
        val todayCount = CalendarMath.eventDays(cal.events, s.now.zone)[today].orEmpty().size
        val count = when {
            !cal.granted -> ""
            todayCount == 0 -> "FREE TODAY"
            todayCount == 1 -> "1 EVENT TODAY"
            else -> "$todayCount EVENTS TODAY"
        }
        s.canvas.drawText(count, b.right, b.top + 11f * s.k, CalendarKit.mono(s, 8f, s.ink(0.5f), Paint.Align.RIGHT))

        val list = RectF(b.left, b.top + headH + 8f, b.right, b.bottom)
        s.canvas.drawLine(b.left, list.top - 4f, b.right, list.top - 4f, s.stroke(s.ink(0.12f), 1f))
        val rowH = 22f * s.k
        val agenda = CalendarMath.agenda(cal.events, s.now, ((list.height() - 12f) / rowH).toInt())
        if (agenda.rows.isEmpty()) {
            empty(s, list)
            return
        }
        var y = list.top
        for (row in agenda.rows) {
            when (row) {
                is CalendarMath.Row.Day -> {
                    s.canvas.drawText(CalendarKit.dayLabel(s, row.date), b.left, y + rowH * 0.62f, CalendarKit.mono(s, 7.5f, s.ink(0.45f), weight = 700, tracking = 0.16f))
                    s.canvas.drawLine(b.left + 72f * s.k, y + rowH * 0.5f, b.right, y + rowH * 0.5f, s.stroke(s.ink(0.07f), 1f))
                }
                is CalendarMath.Row.Now -> {
                    val cy = y + rowH * 0.5f
                    s.canvas.drawCircle(b.left + 3f, cy, 3f, s.fill(s.accent))
                    s.canvas.drawLine(b.left + 3f, cy, b.right, cy, s.stroke(s.accent, 1.2f))
                    val p = CalendarKit.mono(s, 7f, s.accent, Paint.Align.RIGHT, weight = 700)
                    val bg = RectF(b.right - p.measureText("NOW") - 8f, cy - 6f, b.right, cy + 6f)
                    s.canvas.drawRoundRect(bg, 6f, 6f, s.fill(s.accent))
                    s.canvas.drawText("NOW", b.right - 4f, cy + s.capHeight(p) / 2f, CalendarKit.mono(s, 7f, s.style.bg.color, Paint.Align.RIGHT, weight = 700))
                }
                is CalendarMath.Row.Event -> eventRow(s, RectF(b.left, y, b.right, y + rowH), row)
            }
            y += rowH
        }
        if (agenda.hidden > 0) {
            s.canvas.drawText("+${agenda.hidden} MORE", b.right, b.bottom, CalendarKit.mono(s, 7.5f, s.ink(0.45f), Paint.Align.RIGHT))
        }
    }

    private fun eventRow(s: Scene, r: RectF, row: CalendarMath.Row.Event) {
        val e = row.event
        val alpha = if (row.ongoing) 1f else 0.92f
        CalendarKit.chip(s.canvas, r.left, r.top + 3f, r.bottom - 3f, e.color, s)
        val timeW = 58f * s.k
        val tp = CalendarKit.mono(s, 8.5f, s.ink(0.6f), weight = 500, tracking = 0.02f)
        val label = when {
            e.allDay -> "ALL DAY"
            row.ongoing -> "UNTIL"
            else -> CalendarKit.time(s, e)
        }
        val cy = r.centerY()
        s.canvas.drawText(label, r.left + 9f, cy + s.capHeight(tp) / 2f - if (row.ongoing && !e.allDay) 4f * s.k else 0f, tp)
        if (row.ongoing && !e.allDay) {
            s.canvas.drawText(CalendarKit.time(s, e.end), r.left + 9f, cy + s.capHeight(tp) / 2f + 5f * s.k, CalendarKit.mono(s, 8.5f, s.accent, weight = 700, tracking = 0.02f))
        }
        val title = s.paint(12.5f * s.k, s.ink(alpha), weight = if (row.ongoing) 700 else 500)
        val x = r.left + 9f + timeW
        val showLoc = s.flag("location") && e.location.isNotEmpty() && s.w >= 300f
        val locP = s.paint(10f * s.k, s.ink(0.45f), weight = 400)
        val locW = if (showLoc) minOf(locP.measureText(e.location), r.width() * 0.3f) else 0f
        val titleText = CalendarKit.fitText(e.title, r.right - x - locW - 8f, title)
        s.textMid(titleText, x, cy, title)
        if (showLoc) {
            s.textMid(CalendarKit.fitText(e.location, locW, locP), r.right - locW, cy, locP)
        }
    }

    private fun empty(s: Scene, r: RectF) {
        val rowH = 22f * s.k
        val rows = ((r.height() - 12f) / rowH).toInt().coerceIn(1, 6)
        for (i in 0 until rows) {
            val cy = r.top + rowH * i + rowH / 2f
            s.canvas.drawRoundRect(RectF(r.left, cy - 7f, r.left + 3f, cy + 7f), 1.5f, 1.5f, s.fill(s.ink(0.08f)))
            s.canvas.drawRoundRect(RectF(r.left + 9f, cy - 2f, r.left + 40f * s.k, cy + 2f), 2f, 2f, s.fill(s.ink(0.06f)))
            val w = r.width() * (0.62f - 0.09f * (i % 4))
            s.canvas.drawRoundRect(RectF(r.left + 67f * s.k, cy - 2.5f, r.left + 67f * s.k + w * 0.6f, cy + 2.5f), 2.5f, 2.5f, s.fill(s.ink(0.05f)))
        }
        val cx = r.centerX()
        val cy = r.top + (rowH * rows) / 2f
        val card = RectF(cx - 92f * s.k, cy - 20f * s.k, cx + 92f * s.k, cy + 20f * s.k)
        s.canvas.drawRoundRect(card, 12f, 12f, s.fill(s.style.bg.color))
        s.canvas.drawRoundRect(card, 12f, 12f, s.stroke(s.ink(0.1f), 1f))
        val granted = s.data.calendar.granted
        CalendarKit.glyph(s.canvas, card.left + 18f * s.k, cy, 12f * s.k, if (granted) s.accent else s.ink(0.45f), s)
        val title = if (granted) "Nothing scheduled" else "Calendar not connected"
        val sub = if (granted) "NEXT ${CalendarSource.WINDOW_DAYS} DAYS ARE CLEAR" else "OPEN TESSERA TO CONNECT"
        s.canvas.drawText(title, card.left + 34f * s.k, cy - 1f, s.paint(12f * s.k, s.ink(0.85f), weight = 500))
        s.canvas.drawText(sub, card.left + 34f * s.k, cy + 11f * s.k, CalendarKit.mono(s, 7f, s.ink(0.45f)))
    }
}
