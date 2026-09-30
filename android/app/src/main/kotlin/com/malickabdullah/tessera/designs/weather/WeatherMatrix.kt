package com.malickabdullah.tessera.designs.weather

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.WeatherState
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.floor
import kotlin.math.min

object WeatherMatrix : WidgetDesign {
    override val id = "weather.matrix"
    override val category = Category.WEATHER
    override val name = "Dot Matrix"
    override val blurb = "Temperature lit on an LED grid, with a 24-dot track whose brightness is the chance of rain each hour."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("dot", 700, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF0C0C0D, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    private const val ROWS = 5

    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene)

    override fun draw(s: Scene) {
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.07f))
        val w = WeatherKit.ready(s) ?: return
        val b = s.box
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)
        val track = RectF(b.left, b.bottom - 8f, b.right, b.bottom)
        drawTrack(s, w, track)
        val wide = s.w >= 280f
        val info = if (wide) RectF(b.left + b.width() * 0.56f, b.top + 22f, b.right, track.top - 12f) else RectF(b.left, track.top - 38f * s.k, b.right, track.top - 10f)
        val digitsBox = if (wide) RectF(b.left, b.top + 22f, info.left - 12f, track.top - 12f) else RectF(b.left, b.top + 22f, b.right, info.top - 6f)
        drawTemperature(s, w, digitsBox)
        drawInfo(s, w, info, wide)
    }

    /** The temperature as lit cells over faint unlit ones, pitch chosen to fill [r]. */
    private fun drawTemperature(s: Scene, w: WeatherState, r: RectF) {
        val text = WeatherMath.degrees(w.temperature, WeatherKit.fahrenheit(s)).toString()
        val cols = WeatherMath.dotColumns(text) + 3
        val pitch = floor(min(r.width() / cols, r.height() / ROWS) * s.hero)
        val dot = pitch * 0.4f
        val top = r.centerY() - pitch * (ROWS - 1) / 2f
        val lit = WeatherMath.dotCells(text).toSet()
        for (c in 0 until cols - 3) for (row in 0 until ROWS) {
            val x = r.left + dot + c * pitch
            val y = top + row * pitch
            s.canvas.drawCircle(x, y, dot, s.fill(if ((c to row) in lit) s.text else s.ink(0.08f)))
        }
        val degX = r.left + dot + (cols - 2) * pitch
        s.canvas.drawCircle(degX, top, dot * 1.05f, s.stroke(s.accent, dot * 0.7f))
    }

    private fun drawInfo(s: Scene, w: WeatherState, r: RectF, wide: Boolean) {
        val today = w.daysFrom(s.now.toInstant()).first()
        val cond = s.paint((if (wide) 17f else 15f) * s.k, s.text)
        val dim = WeatherKit.mono(s, 8.5f, s.ink(0.58f), tracking = 0.08f)
        if (wide) {
            val glyph = min(r.width() * 0.34f, r.height() * 0.34f)
            WeatherKit.glyph(s.canvas, WeatherKit.sky(w), w.isDay, r.left + glyph / 2f, r.top + glyph / 2f, glyph, s.ink(0.9f), s.accent, Layer.ALL, 0)
            s.canvas.drawText(w.condition.uppercase(), r.left, r.top + glyph + 22f * s.k, cond)
            s.canvas.drawText("H ${WeatherKit.deg(s, today.high)}  L ${WeatherKit.deg(s, today.low)}", r.left, r.bottom - 14f * s.k, dim)
            s.canvas.drawText("FEELS ${WeatherKit.deg(s, w.feelsLike)} · ${w.humidity}% RH", r.left, r.bottom, dim)
        } else {
            s.canvas.drawText(w.condition.uppercase(), r.left, r.top + 12f * s.k, cond)
            s.canvas.drawText("H ${WeatherKit.deg(s, today.high)}  L ${WeatherKit.deg(s, today.low)}", r.left, r.bottom, dim)
        }
    }

    /** One dot per coming hour; lit in the accent as strongly as rain is likely, a ring on the current hour. */
    private fun drawTrack(s: Scene, w: WeatherState, r: RectF) {
        val hours = w.hoursFrom(s.now.toInstant()).take(24)
        val step = r.width() / 23f
        hours.forEachIndexed { i, h ->
            val x = r.left + i * step
            val chance = h.precipitationChance / 100f
            val color = if (chance < 0.1f) s.ink(0.16f) else s.ink(0.25f + 0.75f * chance, s.accent)
            s.canvas.drawCircle(x, r.centerY(), if (i % 6 == 0) 2f else 1.6f, s.fill(color))
            if (i == 0) s.canvas.drawCircle(x, r.centerY(), 3.6f, s.stroke(s.text, 1f))
        }
        s.canvas.drawText("RAIN · 24H", r.right, r.top - 4f, WeatherKit.mono(s, 7f, s.ink(0.4f), Paint.Align.RIGHT))
    }
}
