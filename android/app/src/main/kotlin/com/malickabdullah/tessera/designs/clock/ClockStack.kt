package com.malickabdullah.tessera.designs.clock

import android.graphics.Paint
import android.graphics.Path
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

object ClockStack : WidgetDesign {
    override val id = "clock.stack"
    override val category = Category.CLOCK
    override val name = "Bold Stack"
    override val blurb = "Hours over minutes, set big and heavy, with a ruler of the day along the edge."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 700, text = 0xFF141414, accent = 0xFFFF5A1F, background = 0xFFEDE9E1, radius = 30f, padding = 16f, tracking = -0.02f)
    override val toggles = listOf(hourFormatToggle, Toggle.Switch("ruler", "Hour ruler", true))

    override fun liveKey(scene: SceneInputs) = if (scene.flag("ruler")) "${scene.now.hour}" else null

    override fun draw(s: Scene) {
        val b = s.box
        val ruler = s.flag("ruler")
        val rulerW = if (ruler) 18f else 0f
        val metaH = 14f * s.k
        s.textClock(
            RectF(b.left, b.top, b.right - rulerW, b.top + metaH),
            "EEEE" to "EEEE",
            10.5f * s.k,
            s.ink(0.6f),
            font = "mono",
            weight = 500,
            gravity = Gravity.START or Gravity.CENTER_VERTICAL,
            caps = true,
        )
        s.textClock(
            RectF(b.left, b.top, b.right - rulerW - 4f, b.top + metaH),
            "d MMM" to "d MMM",
            10.5f * s.k,
            s.accent,
            font = "mono",
            weight = 500,
            gravity = Gravity.END or Gravity.CENTER_VERTICAL,
            caps = true,
        )

        val area = RectF(b.left - 2f, b.top + metaH + 2f, b.right - rulerW - 4f, b.bottom)
        val rowH = area.height() / 2f
        val size = s.fit(Kit.widest(s, "88"), area.width(), rowH) * s.hero
        s.textClock(RectF(area.left, area.top, area.right, area.top + rowH), s.timeFormats("hh", "HH"), size, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        s.textClock(RectF(area.left, area.top + rowH, area.right, area.bottom), "mm" to "mm", size, s.accent, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        s.canvas.drawLine(b.left, area.top + rowH, area.right, area.top + rowH, s.stroke(s.ink(0.1f), 1f))

        if (ruler) drawRuler(s, RectF(b.right - 10f, b.top + 2f, b.right, b.bottom - 2f))
    }

    private fun drawRuler(s: Scene, r: RectF) {
        val step = r.height() / 24f
        val tick = s.stroke(s.ink(0.3f), 1f)
        val major = s.stroke(s.ink(0.7f), 1.4f)
        for (i in 0..24) {
            val y = r.top + i * step
            val long = i % 6 == 0
            s.canvas.drawLine(r.right - if (long) 9f else 5f, y, r.right, y, if (long) major else tick)
        }
        val y = r.top + (s.now.hour + s.now.minute / 60f) * step
        val marker = Path().apply {
            moveTo(r.right - 10f, y)
            lineTo(r.right - 16f, y - 3.5f)
            lineTo(r.right - 16f, y + 3.5f)
            close()
        }
        s.canvas.drawPath(marker, s.fill(s.accent))
        s.canvas.drawLine(r.right - 10f, y, r.right, y, s.stroke(s.accent, 2f))
        val label = s.paint(7f * s.k, s.ink(0.45f), font = "mono", weight = 500, align = Paint.Align.RIGHT)
        for (i in listOf(6, 12, 18)) s.canvas.drawText("%02d".format(i), r.right - 11f, r.top + i * step + 2.5f, label)
    }
}
