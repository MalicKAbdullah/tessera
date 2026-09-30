package com.malickabdullah.tessera.designs.sky

import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object SkyDial : WidgetDesign {
    override val id = "sky.dial"
    override val category = Category.SKY
    override val name = "Sky Clock"
    override val blurb = "A 24-hour dial of today's night, twilight and daylight, with the sun on it and the moon inside."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFEEF0F3, accent = 0xFFF7D774, background = 0xFF101318, radius = 44f, padding = 10f)
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = SkyKit.key(scene, 5)

    /** Noon at the top, midnight at the bottom. */
    private fun angle(fraction: Float) = Math.toRadians((90.0 + fraction * 360.0))

    override fun draw(s: Scene) {
        val place = s.data.skyPlace ?: return SkyKit.noPlace(s)
        val b = s.box
        val side = min(b.width(), b.height())
        val cx = b.centerX()
        val cy = b.centerY()
        val stroke = side * 0.075f
        val r = side / 2f - stroke / 2f - side * 0.06f
        val ring = RectF(cx - r, cy - r, cx + r, cy + r)

        val alts = SkyKit.altitudes(s, place, 5)
        val step = 360f / (alts.size - 1)
        for (i in 0 until alts.size - 1) {
            val start = 90f + i * step
            s.canvas.drawArc(ring, start, step + 0.4f, false, s.stroke(SkyKit.color(s, SkyKit.light((alts[i] + alts[i + 1]) / 2)), stroke, round = false))
        }
        val tickR0 = r + stroke / 2f + side * 0.012f
        for (h in 0 until 24) {
            val a = angle(h / 24f)
            val major = h % 6 == 0
            val r1 = tickR0 + if (major) side * 0.03f else side * 0.014f
            s.canvas.drawLine(
                cx + cos(a).toFloat() * tickR0, cy + sin(a).toFloat() * tickR0,
                cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1,
                s.stroke(s.ink(if (major) 0.55f else 0.25f), if (major) 1.4f else 0.9f),
            )
        }
        val labelR = r - stroke / 2f - side * 0.06f
        listOf(0 to "00", 6 to "06", 12 to "12", 18 to "18").forEach { (h, t) ->
            val a = angle(h / 24f)
            s.textMid(t, cx + cos(a).toFloat() * labelR, cy + sin(a).toFloat() * labelR, SkyKit.label(s, side / 170f * 7.5f, s.ink(0.4f), Paint.Align.CENTER))
        }

        val a = angle(SkyKit.nowFraction(s))
        val sx = cx + cos(a).toFloat() * r
        val sy = cy + sin(a).toFloat() * r
        s.canvas.drawCircle(sx, sy, stroke * 0.95f, s.fill(s.style.bg.color))
        s.canvas.drawCircle(sx, sy, stroke * 0.7f, s.fill(s.text))
        s.canvas.drawCircle(sx, sy, stroke * 0.4f, s.fill(s.accent))

        val timeH = side * 0.17f
        val timeRect = RectF(cx - r * 0.72f, cy - timeH * 0.95f, cx + r * 0.72f, cy + timeH * 0.25f)
        val size = s.fit(Kit.widest(s, "88:88"), timeRect.width(), timeRect.height()) * s.hero
        s.textClock(timeRect, s.timeFormats("h:mm", "HH:mm"), size, gravity = Gravity.CENTER)

        val phase = Astronomy.moonPhase(s.now.toInstant())
        val mr = side * 0.055f
        val my = cy + side * 0.14f
        SkyKit.moonAt(s, cx - mr * 1.6f, my, mr, phase)
        s.textMid("${(phase.illumination * 100).roundToInt()}%", cx - mr * 0.2f, my, s.paint(side / 170f * 10f * s.k, s.ink(0.7f), font = "mono", weight = 500))
        if (s.h >= 300f) {
            val day = Astronomy.sunDay(s.now.toLocalDate(), place.latitude, place.longitude)
            val y = cy + side * 0.24f
            val p = SkyKit.label(s, 9f, s.ink(0.6f), Paint.Align.CENTER)
            s.canvas.drawText("↑ ${SkyKit.time(s, day.sunrise)}   ↓ ${SkyKit.time(s, day.sunset)}", cx, y, p)
        }
    }
}
