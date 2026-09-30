package com.malickabdullah.tessera.designs.battery

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.PI
import kotlin.math.sin

object BatteryNumeric : WidgetDesign {
    override val id = "battery.numeric"
    override val category = Category.BATTERY
    override val name = "Big Numeric"
    override val blurb = "A towering condensed percentage over a liquid fill that rises with your charge."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("condensed", 700, text = 0xFF111111, accent = 0xFFD4FF3A, background = 0xFFEEF0F3, radius = 30f, padding = 16f)
    override val signals = setOf(Signal.BATTERY)
    override val motion = "While charging, the surface of the fill ripples."

    override fun liveKey(scene: SceneInputs) = BatteryKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val color = BatteryKit.levelColor(s)
        val surface = s.h - s.h * bat.level / 100f
        // The wave phase follows the render minute so each refresh looks freshly poured.
        val phase = s.now.minute / 60f * 2f * PI.toFloat()
        s.canvas.drawPath(wave(s, surface, phase, closed = true), s.fill(s.ink(0.18f, color)))
        s.canvas.drawPath(wave(s, surface, phase, closed = false), s.stroke(s.ink(0.55f, color), 1.4f))
        if (bat.charging) {
            val band = RectF(0f, surface - 7f, s.w, surface + 7f)
            s.flipper(band, 400, 3) { i ->
                drawPath(wave(s, surface, phase + (i + 1) * 0.9f, closed = false), s.stroke(color, 1.8f))
            }
        }

        val statusH = 14f * s.k
        val numArea = RectF(b.left, b.top, b.right, b.bottom - statusH - 4f)
        val size = s.fit("100", numArea.width() * 0.8f, numArea.height() * 1.18f) * s.hero
        val num = s.paint(size, s.text, tracking = -0.02f)
        val cap = s.capHeight(num)
        val baseline = numArea.centerY() + cap / 2f
        s.canvas.drawText("${bat.level}", numArea.left - size * 0.02f, baseline, num)
        val pct = s.paint(size * 0.28f, color)
        s.canvas.drawText("%", numArea.left + num.measureText("${bat.level}") + 3f, baseline - cap + s.capHeight(pct), pct)

        val status = if (bat.charging || bat.full) BatteryKit.status(s) + " · " + BatteryKit.estimate(s) else BatteryKit.estimate(s)
        s.canvas.drawText(status.uppercase(), b.left, b.bottom - 2f, s.paint(10f * s.k, s.ink(0.7f), font = "mono", weight = 500, tracking = 0.1f))
    }

    private fun wave(s: Scene, y: Float, phase: Float, closed: Boolean): Path = Path().apply {
        val amp = 2.6f
        val steps = 48
        for (i in 0..steps) {
            val x = s.w * i / steps
            val yy = y + amp * sin(i / steps.toFloat() * 4f * PI.toFloat() + phase)
            if (i == 0) moveTo(x, yy) else lineTo(x, yy)
        }
        if (closed) {
            lineTo(s.w, s.h)
            lineTo(0f, s.h)
            close()
        }
    }
}
