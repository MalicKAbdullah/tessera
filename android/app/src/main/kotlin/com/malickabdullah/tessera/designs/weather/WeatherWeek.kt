package com.malickabdullah.tessera.designs.weather

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.DayForecast
import com.malickabdullah.tessera.data.WeatherState
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min

object WeatherWeek : WidgetDesign {
    override val id = "weather.week"
    override val category = Category.WEATHER
    override val name = "Five Days"
    override val blurb = "The days ahead with glyphs, rain chance and low-to-high bars on one shared scale; a full week on 4×4."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF1A1B1E, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    private const val COOL = 0xFF6FB7FF.toInt()

    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene, scene.data.weather?.localTime(scene.now.toInstant())?.toLocalDate().toString())

    override fun draw(s: Scene) {
        val w = WeatherKit.ready(s) ?: return
        val b = s.box
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)
        val days = w.daysFrom(s.now.toInstant())
        if (s.h >= 300f) rows(s, w, days.take(7), RectF(b.left, b.top + 20f, b.right, b.bottom)) else columns(s, w, days.take(5), RectF(b.left, b.top + 18f, b.right, b.bottom))
    }

    private fun dayName(s: Scene, w: WeatherState, d: DayForecast, short: Boolean): String =
        if (d.date == w.localTime(s.now.toInstant()).toLocalDate()) {
            "TODAY"
        } else {
            d.date.dayOfWeek.getDisplayName(if (short) TextStyle.SHORT else TextStyle.FULL, Locale.getDefault()).uppercase()
        }

    private fun columns(s: Scene, w: WeatherState, days: List<DayForecast>, r: RectF) {
        val lo = days.minOf { it.low }
        val hi = days.maxOf { it.high }
        val colW = r.width() / days.size
        val glyph = min(colW * 0.42f, 22f)
        val barTop = r.top + 14f * s.k + glyph + 20f * s.k
        val barBottom = r.bottom - 26f * s.k
        days.forEachIndexed { i, d ->
            val cx = r.left + colW * i + colW / 2f
            val today = i == 0
            if (today) Kit.roundRect(s.canvas, RectF(cx - colW / 2f + 2f, r.top - 2f, cx + colW / 2f - 2f, r.bottom + 2f), 12f, s.fill(s.ink(0.06f)))
            s.canvas.drawText(dayName(s, w, d, true), cx, r.top + 9f * s.k, WeatherKit.mono(s, 8f, s.ink(if (today) 0.9f else 0.55f), Paint.Align.CENTER, 0.08f))
            val gy = r.top + 14f * s.k + glyph / 2f
            WeatherKit.glyph(s.canvas, WeatherMath.sky(d.code), true, cx, gy, glyph, s.ink(0.85f), s.accent, Layer.ALL, 0)
            s.canvas.drawText(WeatherKit.deg(s, d.high), cx + 1.5f, barTop - 5f, s.paint(12.5f * s.k, s.text, align = Paint.Align.CENTER))
            val (f0, f1) = WeatherMath.rangeBar(d.low, d.high, lo, hi)
            val yTop = barBottom - (barBottom - barTop) * f1
            val yBot = barBottom - (barBottom - barTop) * f0
            val bw = 5f
            Kit.roundRect(s.canvas, RectF(cx - bw / 2f, barTop, cx + bw / 2f, barBottom), bw / 2f, s.fill(s.ink(0.07f)))
            val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, barTop, 0f, barBottom, s.accent, COOL, Shader.TileMode.CLAMP)
            }
            Kit.roundRect(s.canvas, RectF(cx - bw / 2f, yTop, cx + bw / 2f, maxOf(yBot, yTop + bw)), bw / 2f, bar)
            s.canvas.drawText(WeatherKit.deg(s, d.low), cx + 1.5f, barBottom + 12f * s.k, s.paint(10.5f * s.k, s.ink(0.55f), align = Paint.Align.CENTER))
            val rain = if (d.precipitationChance >= 10) "${d.precipitationChance}%" else "·"
            s.canvas.drawText(rain, cx, r.bottom, WeatherKit.mono(s, 7.5f, if (d.precipitationChance >= 10) s.ink(0.95f, WeatherKit.RAIN) else s.ink(0.3f), Paint.Align.CENTER, 0.02f))
        }
    }

    private fun rows(s: Scene, w: WeatherState, days: List<DayForecast>, r: RectF) {
        val head = RectF(r.left, r.top, r.right, r.top + 56f * s.k)
        val tp = s.paint(min(head.height(), 46f * s.k), s.text)
        s.canvas.drawText(WeatherKit.deg(s, w.temperature), head.left, head.bottom - 4f, tp)
        val tx = head.left + tp.measureText(WeatherKit.deg(s, w.temperature)) + 12f
        s.canvas.drawText(w.condition, tx, head.bottom - 22f * s.k, s.paint(13f * s.k, s.text, weight = 500))
        s.canvas.drawText("FEELS ${WeatherKit.deg(s, w.feelsLike)}  ·  ${WeatherKit.freshness(s, w).uppercase()}", tx, head.bottom - 6f, WeatherKit.mono(s, 8f, s.ink(0.5f)))

        val list = RectF(r.left, head.bottom + 10f, r.right, r.bottom)
        val lo = days.minOf { it.low }
        val hi = days.maxOf { it.high }
        val rowH = list.height() / days.size
        val nameW = 58f * s.k
        val glyph = min(rowH * 0.62f, 22f)
        val rainW = 34f * s.k
        val numW = 30f * s.k
        val barL = list.left + nameW + glyph + rainW + numW + 8f
        val barR = list.right - numW
        days.forEachIndexed { i, d ->
            val cy = list.top + rowH * i + rowH / 2f
            if (i > 0) s.canvas.drawLine(list.left, cy - rowH / 2f, list.right, cy - rowH / 2f, s.stroke(s.ink(0.07f), 1f))
            s.textMid(dayName(s, w, d, true), list.left, cy, WeatherKit.mono(s, 9f, s.ink(if (i == 0) 0.95f else 0.65f), tracking = 0.08f))
            WeatherKit.glyph(s.canvas, WeatherMath.sky(d.code), true, list.left + nameW + glyph / 2f, cy, glyph, s.ink(0.85f), s.accent, Layer.ALL, 0)
            if (d.precipitationChance >= 10) {
                s.textMid("${d.precipitationChance}%", list.left + nameW + glyph + 6f, cy, WeatherKit.mono(s, 8f, s.ink(0.95f, WeatherKit.RAIN), tracking = 0.02f))
            }
            s.textMid(WeatherKit.deg(s, d.low), barL - 6f, cy, s.paint(12f * s.k, s.ink(0.55f), align = Paint.Align.RIGHT))
            s.textMid(WeatherKit.deg(s, d.high), list.right, cy, s.paint(12f * s.k, s.text, align = Paint.Align.RIGHT))
            val bh = 5f
            Kit.roundRect(s.canvas, RectF(barL, cy - bh / 2f, barR - 6f, cy + bh / 2f), bh / 2f, s.fill(s.ink(0.08f)))
            val (f0, f1) = WeatherMath.rangeBar(d.low, d.high, lo, hi)
            val x0 = barL + (barR - 6f - barL) * f0
            val x1 = maxOf(barL + (barR - 6f - barL) * f1, x0 + bh)
            val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(barL, 0f, barR, 0f, COOL, s.accent, Shader.TileMode.CLAMP)
            }
            Kit.roundRect(s.canvas, RectF(x0, cy - bh / 2f, x1, cy + bh / 2f), bh / 2f, bar)
            if (i == 0) {
                val t = WeatherMath.rangeBar(w.temperature, w.temperature, lo, hi).first
                val nx = barL + (barR - 6f - barL) * t
                s.canvas.drawCircle(nx, cy, bh * 0.9f, s.fill(s.text))
                s.canvas.drawCircle(nx, cy, bh * 0.9f, s.stroke(s.style.bg.color, 1.5f))
            }
        }
    }
}
