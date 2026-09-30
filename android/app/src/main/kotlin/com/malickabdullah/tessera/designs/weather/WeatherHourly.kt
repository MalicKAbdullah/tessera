package com.malickabdullah.tessera.designs.weather

import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import kotlin.math.min

object WeatherHourly : WidgetDesign {
    override val id = "weather.hourly"
    override val category = Category.WEATHER
    override val name = "Hourly Curve"
    override val blurb = "The next hours as a smooth temperature curve over bars of rain chance, with the day's high and low marked."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFF15161A, accent = 0xFF2F6BFF, background = 0xFFF2F3F5, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle, hourFormatToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene)

    override fun draw(s: Scene) {
        val w = WeatherKit.ready(s) ?: return
        val b = s.box
        val large = s.h >= 300f
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)

        val headH = if (large) 64f * s.k else 30f * s.k
        val head = RectF(b.left, b.top + 18f, b.right, b.top + 18f + headH)
        val tempSize = min(head.height(), 26f * s.k * if (large) 2.1f else 1f)
        val tp = s.paint(tempSize, s.text)
        s.canvas.drawText(WeatherKit.deg(s, w.temperature), head.left, head.bottom - 2f, tp)
        val tx = head.left + tp.measureText(WeatherKit.deg(s, w.temperature)) + 10f
        val glyph = tempSize * 0.9f
        WeatherKit.glyph(s.canvas, WeatherKit.sky(w), w.isDay, tx + glyph * 0.5f, head.bottom - s.capHeight(tp) / 2f - 2f, glyph, s.ink(0.85f), s.accent, Layer.ALL, 0)
        val cond = s.paint(11.5f * s.k, s.ink(0.75f), weight = 500, align = Paint.Align.RIGHT)
        s.canvas.drawText(w.condition, head.right, head.bottom - 14f * s.k, cond)
        s.canvas.drawText("FEELS ${WeatherKit.deg(s, w.feelsLike)}", head.right, head.bottom - 1f, WeatherKit.mono(s, 8f, s.ink(0.5f), Paint.Align.RIGHT))

        val hours = w.hoursFrom(s.now.toInstant()).take(if (large) 24 else 12)
        val labelsH = 12f * s.k
        val barsH = if (large) 34f else 16f
        val chart = RectF(b.left + 4f, head.bottom + (if (large) 44f else 16f) * s.k, b.right - 4f, b.bottom - labelsH - barsH - 6f)
        val bars = RectF(chart.left, chart.bottom + 6f, chart.right, chart.bottom + 6f + barsH)
        val step = chart.width() / (hours.size - 1)
        val range = WeatherMath.niceRange(hours.map { it.temperature })
        fun x(i: Int) = chart.left + i * step
        fun y(t: Float) = WeatherMath.scaleY(t, range, chart.top, chart.bottom)

        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.ink(0.1f)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(2f, 3f), 0f)
        }
        s.canvas.drawLine(chart.left, chart.bottom, chart.right, chart.bottom, grid)
        s.canvas.drawLine(chart.left, chart.top, chart.right, chart.top, grid)

        hours.forEachIndexed { i, h ->
            val bw = min(step * 0.56f, 7f)
            val bh = bars.height() * h.precipitationChance / 100f
            s.canvas.drawRect(x(i) - bw / 2f, bars.bottom - bars.height(), x(i) + bw / 2f, bars.bottom, s.fill(s.ink(0.05f)))
            if (bh > 0f) s.canvas.drawRect(x(i) - bw / 2f, bars.bottom - bh, x(i) + bw / 2f, bars.bottom, s.fill(s.ink(0.85f, WeatherKit.RAIN)))
        }
        val rainPeak = hours.maxBy { it.precipitationChance }
        if (rainPeak.precipitationChance >= 20) {
            val i = hours.indexOf(rainPeak)
            s.canvas.drawText("${rainPeak.precipitationChance}%", x(i), bars.top - 2f, WeatherKit.mono(s, 7.5f, s.ink(0.95f, WeatherKit.RAIN), Paint.Align.CENTER, 0.02f))
        }

        val line = Path().apply {
            moveTo(x(0), y(hours[0].temperature))
            for (i in 1 until hours.size) {
                val (x0, y0) = x(i - 1) to y(hours[i - 1].temperature)
                val (x1, y1) = x(i) to y(hours[i].temperature)
                cubicTo(x0 + step / 2f, y0, x1 - step / 2f, y1, x1, y1)
            }
        }
        val area = Path(line).apply {
            lineTo(x(hours.size - 1), chart.bottom)
            lineTo(x(0), chart.bottom)
            close()
        }
        s.canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, chart.top, 0f, chart.bottom, s.ink(0.28f, s.accent), s.ink(0f, s.accent), Shader.TileMode.CLAMP)
        })
        s.canvas.drawPath(line, s.stroke(s.accent, 2f).apply { strokeJoin = Paint.Join.ROUND })

        val hi = hours.indices.maxBy { hours[it].temperature }
        val lo = hours.indices.minBy { hours[it].temperature }
        val tag = s.paint(10.5f * s.k, s.text, weight = 500, align = Paint.Align.CENTER)
        for (i in setOf(0, hi, lo)) {
            s.canvas.drawCircle(x(i), y(hours[i].temperature), if (i == 0) 3.6f else 2.6f, s.fill(if (i == 0) s.text else s.accent))
            if (i == 0) s.canvas.drawCircle(x(i), y(hours[i].temperature), 6f, s.stroke(s.ink(0.25f), 1f))
            val ty = y(hours[i].temperature) - 7f
            s.canvas.drawText(WeatherKit.deg(s, hours[i].temperature), x(i).coerceIn(chart.left + 8f, chart.right - 8f), ty, tag)
        }

        val every = if (large) 4 else 3
        val lp = WeatherKit.mono(s, 7.5f, s.ink(0.5f), Paint.Align.CENTER, 0.04f)
        hours.forEachIndexed { i, h ->
            if (i % every != 0) return@forEachIndexed
            val label = if (i == 0) "NOW" else WeatherKit.hourLabel(s, h.time).uppercase()
            s.canvas.drawText(label, x(i).coerceIn(chart.left + 8f, chart.right - 8f), b.bottom, lp)
            if (large && i > 0) {
                WeatherKit.glyph(s.canvas, WeatherMath.sky(h.code), WeatherKit.isDayAt(w, h.time), x(i), head.bottom + 18f * s.k, 14f, s.ink(0.55f), s.accent, Layer.ALL, 0)
            }
        }
    }
}
