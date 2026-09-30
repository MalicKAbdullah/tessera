package com.malickabdullah.tessera.designs.battery

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign

object BatterySegments : WidgetDesign {
    override val id = "battery.segments"
    override val category = Category.BATTERY
    override val name = "Segments"
    override val blurb = "Ten-segment bar with temperature, health, voltage and power source."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFFE9EDE4, accent = 0xFFC3D08E, background = 0xFF1A1D1A, radius = 26f, padding = 16f)
    override val signals = setOf(Signal.BATTERY)
    override val motion = "While charging, the next segment breathes."

    override fun liveKey(scene: SceneInputs) = BatteryKit.key(scene) + "|" + scene.data.battery.temperatureC.toInt()

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val color = BatteryKit.levelColor(s)
        val label = s.paint(9.5f * s.k, color, font = "mono", weight = 700, tracking = 0.16f)
        s.canvas.drawText("BATTERY", b.left, b.top + 9f, label)
        s.canvas.drawText(
            BatteryKit.status(s).uppercase(),
            b.right,
            b.top + 9f,
            s.paint(9.5f * s.k, s.ink(0.6f), font = "mono", weight = 500, align = Paint.Align.RIGHT, tracking = 0.1f),
        )

        val large = s.h >= 300f
        val specH = 36f
        val numberH = if (large) b.height() * 0.28f else b.height() - 22f - specH - 26f
        val numSize = s.fit("100%", b.width() * 0.6f, numberH) * s.hero
        val num = s.paint(numSize, s.text)
        val fm = num.fontMetrics
        val numTop = b.top + 16f
        val baseline = numTop + numberH / 2f - (fm.ascent + fm.descent) / 2f
        s.canvas.drawText("${bat.level}", b.left - 1f, baseline, num)
        s.canvas.drawText("%", b.left + num.measureText("${bat.level}") + 2f, baseline, s.paint(numSize * 0.4f, s.ink(0.5f)))
        s.canvas.drawText(
            BatteryKit.estimate(s),
            b.right,
            baseline,
            s.paint(11f * s.k, s.ink(0.7f), weight = 400, align = Paint.Align.RIGHT),
        )

        val bar = RectF(b.left, numTop + numberH + 6f, b.right, numTop + numberH + 6f + if (large) 26f else 16f)
        val gap = 3f
        val segW = (bar.width() - gap * 9) / 10f
        val full = bat.level / 10
        val partial = (bat.level % 10) / 10f
        for (i in 0 until 10) {
            val seg = RectF(bar.left + i * (segW + gap), bar.top, bar.left + i * (segW + gap) + segW, bar.bottom)
            s.canvas.drawRoundRect(seg, 3f, 3f, s.fill(s.ink(0.08f)))
            when {
                i < full -> s.canvas.drawRoundRect(seg, 3f, 3f, s.fill(color))
                i == full && partial > 0f -> s.canvas.drawRoundRect(RectF(seg.left, seg.top, seg.left + segW * partial, seg.bottom), 3f, 3f, s.fill(color))
            }
            if (bat.charging && i == full) {
                s.flipper(seg, 500, 3) { f -> drawRoundRect(seg, 3f, 3f, s.fill(s.ink(0.15f + 0.25f * f, color))) }
            }
        }

        if (large) {
            val chart = RectF(b.left, bar.bottom + 16f, b.right, b.bottom - specH - 14f)
            Sparkline.draw(s, chart, s.data.batteryHistory, color)
        }
        drawSpecs(s, RectF(b.left, b.bottom - specH, b.right, b.bottom))
    }

    private fun drawSpecs(s: Scene, r: RectF) {
        val bat = s.data.battery
        val items = listOf(
            "TEMP" to "%.1f°C".format(bat.temperatureC),
            "HEALTH" to bat.health,
            "VOLTS" to "%.2f".format(bat.voltageMv / 1000f),
            "SOURCE" to if (bat.plug.name == "NONE") "—" else bat.plug.label,
        )
        val colW = r.width() / items.size
        items.forEachIndexed { i, (label, value) ->
            val x = r.left + colW * i
            if (i > 0) s.canvas.drawLine(x - 4f, r.top + 2f, x - 4f, r.bottom - 2f, s.stroke(s.ink(0.1f), 1f))
            s.canvas.drawText(label, x, r.top + 10f, s.paint(8f * s.k, s.ink(0.45f), weight = 500, tracking = 0.14f))
            s.canvas.drawText(value, x, r.top + 29f, s.paint(12.5f * s.k, s.text, weight = 500))
        }
    }
}
