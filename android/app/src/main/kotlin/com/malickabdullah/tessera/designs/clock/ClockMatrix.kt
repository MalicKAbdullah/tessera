package com.malickabdullah.tessera.designs.clock

import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.temporal.IsoFields

object ClockMatrix : WidgetDesign {
    override val id = "clock.matrix"
    override val category = Category.CLOCK
    override val name = "Dot Matrix"
    override val blurb = "LED-grid time over a lattice of unlit dots, with a 24-dot track of the day."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("dot", 700, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF0C0C0D, radius = 30f)
    override val toggles = listOf(
        hourFormatToggle,
        Toggle.Switch("pulse", "Pulsing LED", true),
        Toggle.Switch("track", "Day track", true),
    )
    override val motion = "The LED beside the date pulses once a second."

    override fun liveKey(scene: SceneInputs) = "${scene.now.toLocalDate()}|${scene.now.hour}"

    override fun draw(s: Scene) {
        val b = s.box
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.07f))

        val headerH = 14f * s.k
        s.textClock(
            RectF(b.left, b.top, b.right - 14f, b.top + headerH),
            "EEE dd MMM" to "EEE dd MMM",
            10.5f * s.k,
            s.ink(0.72f),
            font = "mono",
            weight = 500,
            gravity = Gravity.START or Gravity.CENTER_VERTICAL,
            caps = true,
        )
        val led = RectF(b.right - 8f, b.top + headerH / 2f - 4f, b.right, b.top + headerH / 2f + 4f)
        s.canvas.drawCircle(led.centerX(), led.centerY(), 3.2f, s.fill(s.ink(0.12f, s.accent)))
        if (s.flag("pulse")) {
            s.flipper(led, 1000, 2) { i -> drawCircle(led.centerX(), led.centerY(), 3.2f, s.fill(if (i == 0) s.accent else s.ink(0.25f, s.accent))) }
        } else {
            s.canvas.drawCircle(led.centerX(), led.centerY(), 3.2f, s.fill(s.accent))
        }

        val track = s.flag("track") && s.h >= 120f
        val footerH = if (track) 16f else 0f
        val area = RectF(b.left, b.top + headerH + 6f, b.right, b.bottom - footerH)
        val stacked = s.w < 240f && s.h >= s.w * 0.8f
        if (stacked) {
            val rowH = area.height() / 2f
            val size = s.fit(Kit.widest(s, "88"), area.width(), rowH) * s.hero
            val top = RectF(area.left, area.top, area.right, area.top + rowH)
            val bottom = RectF(area.left, area.top + rowH, area.right, area.bottom)
            s.textClock(top, s.timeFormats("hh", "HH"), size, gravity = Gravity.START or Gravity.BOTTOM)
            s.textClock(bottom, "mm" to "mm", size, s.accent, gravity = Gravity.START or Gravity.TOP)
        } else {
            val size = s.fit(Kit.widest(s, "88:88"), area.width(), area.height()) * s.hero
            s.textClock(area, s.timeFormats("h:mm", "HH:mm"), size, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        }

        if (track) drawDayTrack(s, RectF(b.left, b.bottom - 8f, b.right, b.bottom))
        if (s.h >= 300f) {
            val week = s.now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            val label = "WK %02d · DAY %03d".format(week, s.now.dayOfYear)
            val p = s.paint(9.5f * s.k, s.ink(0.5f), font = "mono", weight = 500, align = android.graphics.Paint.Align.RIGHT, tracking = 0.12f)
            s.canvas.drawText(label, b.right, b.bottom - 18f, p)
        }
    }

    private fun drawDayTrack(s: Scene, r: RectF) {
        val step = r.width() / 23f
        val cy = r.centerY()
        for (i in 0 until 24) {
            val x = r.left + i * step
            val color = when {
                i < s.now.hour -> s.ink(0.55f)
                i == s.now.hour -> s.accent
                else -> s.ink(0.14f)
            }
            s.canvas.drawCircle(x, cy, if (i == s.now.hour) 2.6f else if (i % 6 == 0) 2f else 1.5f, s.fill(color))
        }
    }
}
