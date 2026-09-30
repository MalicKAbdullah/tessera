package com.malickabdullah.tessera.designs.weather

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import com.malickabdullah.tessera.data.WeatherState
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object WeatherGauges : WidgetDesign {
    override val id = "weather.gauges"
    override val category = Category.WEATHER
    override val name = "Instruments"
    override val blurb = "A wind compass, UV dial and humidity ring side by side; pressure and gusts join them on 4×4."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFECECEA, accent = 0xFF7FD1FF, background = 0xFF0F1012, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    private const val START = 135f
    private const val SWEEP = 270f
    private val uvColors = intArrayOf(0xFF4CC38A.toInt(), 0xFFF2D14B.toInt(), 0xFFFF9B3D.toInt(), 0xFFFF4D4D.toInt(), 0xFFB36BFF.toInt())

    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene)

    override fun draw(s: Scene) {
        val w = WeatherKit.ready(s) ?: return
        val b = s.box
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)
        val area = RectF(b.left, b.top + 20f, b.right, b.bottom)
        if (s.h >= 300f) {
            val half = area.height() / 2f
            val top = RectF(area.left, area.top, area.right, area.top + half)
            val bottom = RectF(area.left, top.bottom + 6f, area.right, area.bottom)
            row(s, w, top, listOf(::wind, ::uv, ::humidity))
            row(s, w, bottom, listOf(::pressure, ::gusts, ::feels))
        } else {
            row(s, w, area, listOf(::wind, ::uv, ::humidity))
        }
    }

    private fun row(s: Scene, w: WeatherState, r: RectF, cells: List<(Scene, WeatherState, RectF) -> Unit>) {
        val cw = r.width() / cells.size
        cells.forEachIndexed { i, cell ->
            if (i > 0) s.canvas.drawLine(r.left + cw * i, r.top + 8f, r.left + cw * i, r.bottom - 8f, s.stroke(s.ink(0.08f), 1f))
            cell(s, w, RectF(r.left + cw * i + 4f, r.top, r.left + cw * (i + 1) - 4f, r.bottom))
        }
    }

    /** Label under a round instrument; returns the dial's centre and radius. */
    private fun frame(s: Scene, r: RectF, label: String): Triple<Float, Float, Float> {
        val labelH = 14f * s.k
        s.canvas.drawText(label, r.centerX(), r.bottom, WeatherKit.mono(s, 7.5f, s.ink(0.5f), Paint.Align.CENTER))
        val side = min(r.width(), r.height() - labelH - 4f)
        return Triple(r.centerX(), r.top + (r.height() - labelH) / 2f, side / 2f)
    }

    private fun ticks(s: Scene, cx: Float, cy: Float, r: Float, count: Int, start: Float, sweep: Float, majorEvery: Int) {
        for (i in 0..count) {
            if (sweep >= 360f && i == count) break
            val a = Math.toRadians((start + sweep * i / count).toDouble())
            val major = i % majorEvery == 0
            val r1 = r * if (major) 0.82f else 0.88f
            s.canvas.drawLine(
                cx + cos(a).toFloat() * r, cy + sin(a).toFloat() * r,
                cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1,
                s.stroke(s.ink(if (major) 0.5f else 0.2f), if (major) 1.2f else 0.8f),
            )
        }
    }

    private fun wind(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "WIND ${WeatherMath.compass(w.windDirection)}")
        s.canvas.drawCircle(cx, cy, rad, s.stroke(s.ink(0.14f), 1f))
        ticks(s, cx, cy, rad, 36, -90f, 360f, 9)
        val np = WeatherKit.mono(s, 6.5f, s.ink(0.55f), Paint.Align.CENTER, 0f)
        listOf("N" to -90.0, "E" to 0.0, "S" to 90.0, "W" to 180.0).forEach { (t, deg) ->
            val a = Math.toRadians(deg)
            s.textMid(t, cx + cos(a).toFloat() * rad * 0.64f, cy + sin(a).toFloat() * rad * 0.64f, if (t == "N") WeatherKit.mono(s, 6.5f, s.accent, Paint.Align.CENTER, 0f) else np)
        }
        // Meteorological direction is where the wind comes from; the arrow points where it blows.
        val a = Math.toRadians(w.windDirection + 90.0)
        val dx = cos(a).toFloat()
        val dy = sin(a).toFloat()
        val tip = rad * 0.8f
        val arrow = Path().apply {
            moveTo(cx + dx * tip, cy + dy * tip)
            lineTo(cx - dy * rad * 0.14f - dx * rad * 0.1f, cy + dx * rad * 0.14f - dy * rad * 0.1f)
            lineTo(cx - dx * rad * 0.8f, cy - dy * rad * 0.8f)
            lineTo(cx + dy * rad * 0.14f - dx * rad * 0.1f, cy - dx * rad * 0.14f - dy * rad * 0.1f)
            close()
        }
        s.canvas.drawPath(arrow, s.fill(s.ink(0.25f, s.accent)))
        val speed = WeatherMath.speed(w.windKmh, WeatherKit.fahrenheit(s)).split(" ")
        val num = s.paint(rad * 0.46f, s.text, align = Paint.Align.CENTER)
        s.canvas.drawCircle(cx, cy, rad * 0.4f, s.fill(s.style.bg.color))
        s.textMid(speed[0], cx, cy - rad * 0.06f, num)
        s.textMid(speed[1].uppercase(), cx, cy + rad * 0.26f, WeatherKit.mono(s, 6f, s.ink(0.5f), Paint.Align.CENTER, 0.04f))
    }

    private fun uv(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "UV ${WeatherMath.uvLabel(w.uv).uppercase()}")
        val arc = RectF(cx - rad * 0.86f, cy - rad * 0.86f, cx + rad * 0.86f, cy + rad * 0.86f)
        val stroke = rad * 0.14f
        s.canvas.drawArc(arc, START, SWEEP, false, s.stroke(s.ink(0.08f), stroke))
        val grad = Paint(s.stroke(0, stroke)).apply {
            // Drawn in a frame rotated to START, so the gradient's 0 is the dial's 0 and 0.75 its full 270°.
            shader = SweepGradient(cx, cy, uvColors, floatArrayOf(0f, 0.1875f, 0.375f, 0.5625f, 0.75f))
        }
        s.canvas.save()
        s.canvas.rotate(START, cx, cy)
        s.canvas.drawArc(arc, 0f, SWEEP * (w.uv / 11f).coerceIn(0.02f, 1f), false, grad)
        s.canvas.restore()
        ticks(s, cx, cy, rad * 0.62f, 11, START, SWEEP, 11)
        s.textMid("%.0f".format(w.uv), cx, cy, s.paint(rad * 0.62f, s.text, align = Paint.Align.CENTER))
        val today = w.daysFrom(s.now.toInstant()).first()
        s.textMid("MAX ${today.uvMax.roundToInt()}", cx, cy + rad * 0.62f, WeatherKit.mono(s, 6.5f, s.ink(0.5f), Paint.Align.CENTER, 0.04f))
    }

    private fun humidity(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "HUMIDITY")
        val clip = Path().apply { addCircle(cx, cy, rad * 0.82f, Path.Direction.CW) }
        s.canvas.drawCircle(cx, cy, rad * 0.82f, s.fill(s.ink(0.05f)))
        s.canvas.save()
        s.canvas.clipPath(clip)
        val level = cy + rad * 0.82f - rad * 1.64f * w.humidity / 100f
        val wave = Path().apply {
            moveTo(cx - rad, level)
            val seg = rad / 2f
            for (i in 0 until 4) {
                val x0 = cx - rad + seg * i
                quadTo(x0 + seg / 2f, level + (if (i % 2 == 0) -1f else 1f) * rad * 0.06f, x0 + seg, level)
            }
            lineTo(cx + rad, cy + rad)
            lineTo(cx - rad, cy + rad)
            close()
        }
        s.canvas.drawPath(wave, s.fill(s.ink(0.55f, s.accent)))
        s.canvas.restore()
        s.canvas.drawCircle(cx, cy, rad * 0.82f, s.stroke(s.ink(0.25f), 1f))
        val num = s.paint(rad * 0.5f, s.text, align = Paint.Align.CENTER)
        s.textMid("${w.humidity}%", cx, cy, num)
    }

    private fun pressure(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "PRESSURE HPA")
        val arc = RectF(cx - rad * 0.86f, cy - rad * 0.86f, cx + rad * 0.86f, cy + rad * 0.86f)
        s.canvas.drawArc(arc, START, SWEEP, false, s.stroke(s.ink(0.1f), rad * 0.05f))
        ticks(s, cx, cy, rad * 0.8f, 20, START, SWEEP, 5)
        val t = ((w.pressureHpa - 960f) / 90f).coerceIn(0f, 1f)
        val a = Math.toRadians((START + SWEEP * t).toDouble())
        s.canvas.drawLine(cx, cy, cx + cos(a).toFloat() * rad * 0.78f, cy + sin(a).toFloat() * rad * 0.78f, s.stroke(s.accent, 1.6f))
        s.canvas.drawCircle(cx, cy, rad * 0.07f, s.fill(s.accent))
        s.textMid("${w.pressureHpa.roundToInt()}", cx, cy + rad * 0.52f, s.paint(rad * 0.3f, s.text, align = Paint.Align.CENTER))
    }

    private fun gusts(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "GUSTS")
        val f = WeatherKit.fahrenheit(s)
        val max = 100f
        val bars = 9
        val bw = rad * 1.6f / bars
        for (i in 0 until bars) {
            val x = cx - rad * 0.8f + bw * i + bw / 2f
            val hgt = rad * (0.3f + 1.1f * i / bars)
            val on = w.gustsKmh >= max * i / bars
            s.canvas.drawLine(x, cy + rad * 0.55f, x, cy + rad * 0.55f - hgt, s.stroke(if (on) s.accent else s.ink(0.12f), bw * 0.55f))
        }
        s.textMid(WeatherMath.speed(w.gustsKmh, f), cx, cy + rad * 0.85f, s.paint(rad * 0.26f, s.text, align = Paint.Align.CENTER))
    }

    private fun feels(s: Scene, w: WeatherState, r: RectF) {
        val (cx, cy, rad) = frame(s, r, "FEELS LIKE")
        val bulbR = rad * 0.2f
        val top = cy - rad * 0.75f
        val bottom = cy + rad * 0.5f
        val tube = RectF(cx - bulbR * 0.45f, top, cx + bulbR * 0.45f, bottom)
        s.canvas.drawRoundRect(tube, bulbR, bulbR, s.fill(s.ink(0.1f)))
        val t = ((w.feelsLike + 10f) / 50f).coerceIn(0.05f, 1f)
        s.canvas.drawRoundRect(RectF(tube.left, bottom - (bottom - top) * t, tube.right, bottom), bulbR, bulbR, s.fill(s.accent))
        s.canvas.drawCircle(cx, bottom + bulbR * 0.6f, bulbR, s.fill(s.accent))
        s.textMid(WeatherKit.deg(s, w.feelsLike), cx + rad * 0.55f, cy, s.paint(rad * 0.34f, s.text, align = Paint.Align.CENTER))
        s.textMid(WeatherKit.deg(s, w.temperature), cx - rad * 0.55f, cy, s.paint(rad * 0.24f, s.ink(0.45f), align = Paint.Align.CENTER))
    }
}
