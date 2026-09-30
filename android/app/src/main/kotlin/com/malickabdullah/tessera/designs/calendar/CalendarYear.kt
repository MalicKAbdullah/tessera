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
import java.time.LocalDate
import kotlin.math.min

object CalendarYear : WidgetDesign {
    override val id = "calendar.year"
    override val category = Category.CALENDAR
    override val name = "Year in Dots"
    override val blurb = "Every day of the year as a dot: the past filled, today lit, upcoming event days ringed."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFF141414, accent = 0xFFFF4B3E, background = 0xFFEFEEE9, radius = 30f, padding = 16f)
    override val toggles = listOf(Toggle.Switch("events", "Ring event days", true))
    override val signals = setOf(Signal.CALENDAR)

    override fun liveKey(scene: SceneInputs) = CalendarKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        val today = s.now.toLocalDate()
        val len = today.lengthOfYear()
        val day = today.dayOfYear

        val hp = CalendarKit.mono(s, 9.5f, s.accent, weight = 700)
        s.canvas.drawText("${today.year}", b.left, b.top + 9f * s.k, hp)
        s.canvas.drawText("DAY $day / $len", b.right, b.top + 9f * s.k, CalendarKit.mono(s, 9f, s.ink(0.55f), Paint.Align.RIGHT))

        val footer = if (s.h >= 300f) 58f * s.k else 34f * s.k
        val grid = RectF(b.left, b.top + 20f * s.k, b.right, b.bottom - footer)
        val (cols, rows) = CalendarMath.yearGrid(len, grid.width() / grid.height())
        val pitch = min(grid.width() / cols, grid.height() / rows)
        val r = pitch * 0.32f
        val ox = grid.left + (grid.width() - pitch * cols) / 2f + pitch / 2f
        val oy = grid.top + (grid.height() - pitch * rows) / 2f + pitch / 2f
        val rings = if (s.flag("events")) CalendarMath.eventDays(s.data.calendar.events, s.now.zone) else emptyMap()
        val past = s.fill(s.ink(0.72f))
        val future = s.fill(s.ink(0.13f))
        val monthStart = s.fill(s.ink(0.3f))
        val jan1 = LocalDate.of(today.year, 1, 1)
        for (i in 0 until len) {
            val x = ox + (i % cols) * pitch
            val y = oy + (i / cols) * pitch
            val date = jan1.plusDays(i.toLong())
            when {
                i + 1 == day -> {
                    s.canvas.drawCircle(x, y, r * 1.35f, s.fill(s.accent))
                    s.canvas.drawCircle(x, y, r * 2.3f, s.stroke(s.ink(0.35f, s.accent), 1f))
                }
                i + 1 < day -> s.canvas.drawCircle(x, y, r, past)
                else -> {
                    s.canvas.drawCircle(x, y, r, if (date.dayOfMonth == 1) monthStart else future)
                    rings[date]?.let { s.canvas.drawCircle(x, y, r * 1.7f, s.stroke(it.first(), 1.1f)) }
                }
            }
        }

        val base = b.bottom
        val pct = day * 100f / len
        val big = s.paint(if (s.h >= 300f) 30f * s.k else 20f * s.k, s.text, font = s.style.font, weight = 700)
        val pctText = "%.1f".format(pct)
        s.canvas.drawText(pctText, b.left - 1f, base, big)
        val px = b.left + big.measureText(pctText) + 3f
        s.canvas.drawText("%", px, base, CalendarKit.mono(s, 9f, s.accent, weight = 700))
        val left = len - day
        s.canvas.drawText("$left DAYS LEFT", b.right, base, CalendarKit.mono(s, 8.5f, s.ink(0.55f), Paint.Align.RIGHT))
        if (s.h >= 300f) {
            val bar = RectF(b.left, base - 42f * s.k, b.right, base - 40f * s.k)
            s.canvas.drawRoundRect(bar, 1f, 1f, s.fill(s.ink(0.1f)))
            s.canvas.drawRoundRect(RectF(bar.left, bar.top, bar.left + bar.width() * pct / 100f, bar.bottom), 1f, 1f, s.fill(s.accent))
            val ringed = rings.keys.count { it.isAfter(today) }
            val note = when {
                !s.data.calendar.granted -> ""
                ringed == 0 -> "NO EVENT DAYS IN THE NEXT TWO WEEKS"
                else -> "$ringed EVENT DAYS IN THE NEXT TWO WEEKS"
            }
            if (note.isNotEmpty()) s.canvas.drawText(note, b.right, base - 16f * s.k, CalendarKit.mono(s, 7.5f, s.ink(0.45f), Paint.Align.RIGHT))
            else CalendarKit.connectHint(s, b.right - 110f * s.k, base - 16f * s.k, 7.5f)
        }
    }
}
