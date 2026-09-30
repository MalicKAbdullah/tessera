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
import com.malickabdullah.tessera.engine.hourFormatToggle
import kotlin.math.min

object WeatherNow : WidgetDesign {
    override val id = "weather.now"
    override val category = Category.WEATHER
    override val name = "Conditions"
    override val blurb = "A big temperature beside a drawn sky glyph, with feels-like, the day's range and the hours ahead."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF4F2EE, accent = 0xFFFFB23F, background = 0xFF121315, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle, hourFormatToggle, WeatherKit.motionToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)
    override val motion = WeatherKit.MOTION

    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene)

    override fun draw(s: Scene) {
        val w = WeatherKit.ready(s) ?: return
        val b = s.box
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)
        val body = RectF(b.left, b.top + 20f, b.right, b.bottom)
        when {
            s.h >= 300f -> large(s, w, body)
            s.w >= 280f -> wide(s, w, body)
            else -> small(s, w, body)
        }
    }

    private fun small(s: Scene, w: WeatherState, r: RectF) {
        val glyph = min(r.width(), r.height()) * 0.42f
        WeatherKit.liveGlyph(s, WeatherKit.sky(w), w.isDay, r.right - glyph * 0.55f, r.top + glyph * 0.5f, glyph, s.ink(0.92f), s.accent, s.flag("motion"))
        val detail = RectF(r.left, r.bottom - 30f * s.k, r.right, r.bottom)
        val tempBox = RectF(r.left, r.top + glyph * 0.55f, r.right, detail.top - 2f)
        hero(s, w, tempBox)
        details(s, w, detail)
    }

    private fun wide(s: Scene, w: WeatherState, r: RectF) {
        val left = RectF(r.left, r.top, r.left + r.width() * 0.46f, r.bottom)
        val detail = RectF(left.left, left.bottom - 30f * s.k, left.right, left.bottom)
        hero(s, w, RectF(left.left, left.top, left.right, detail.top - 2f))
        details(s, w, detail)
        val right = RectF(left.right + 14f, r.top, r.right, r.bottom)
        s.canvas.drawLine(left.right + 6f, r.top + 4f, left.right + 6f, r.bottom - 4f, s.stroke(s.ink(0.1f), 1f))
        val glyph = min(right.width() * 0.34f, right.height() * 0.5f)
        WeatherKit.liveGlyph(s, WeatherKit.sky(w), w.isDay, right.centerX(), right.top + glyph * 0.5f, glyph, s.ink(0.92f), s.accent, s.flag("motion"))
        hours(s, w, RectF(right.left, right.top + glyph + 6f, right.right, right.bottom), 5)
    }

    private fun large(s: Scene, w: WeatherState, r: RectF) {
        val top = RectF(r.left, r.top, r.right, r.top + r.height() * 0.52f)
        val glyph = min(top.width() * 0.36f, top.height() * 0.78f)
        WeatherKit.liveGlyph(s, WeatherKit.sky(w), w.isDay, top.right - glyph * 0.55f, top.top + glyph * 0.52f, glyph, s.ink(0.92f), s.accent, s.flag("motion"))
        val detail = RectF(top.left, top.bottom - 32f * s.k, top.right - glyph, top.bottom)
        hero(s, w, RectF(top.left, top.top, top.right - glyph * 1.05f, detail.top - 2f))
        details(s, w, detail)
        val hoursBox = RectF(r.left, top.bottom + 12f, r.right, top.bottom + 12f + r.height() * 0.26f)
        s.canvas.drawLine(r.left, top.bottom + 6f, r.right, top.bottom + 6f, s.stroke(s.ink(0.1f), 1f))
        hours(s, w, hoursBox, 6)
        stats(s, w, RectF(r.left, hoursBox.bottom + 8f, r.right, r.bottom))
    }

    /** Temperature numerals with a raised degree ring, filling [r] bottom-left. */
    private fun hero(s: Scene, w: WeatherState, r: RectF) {
        val value = WeatherKit.deg(s, w.temperature).dropLast(1)
        val size = s.fit("-88", r.width() * 0.84f, r.height()) * s.hero
        val p = s.paint(size, s.text)
        val fm = p.fontMetrics
        val base = r.bottom - fm.descent * 0.35f
        s.canvas.drawText(value, r.left - size * 0.04f, base, p)
        val cap = s.capHeight(p)
        val ring = size * 0.075f
        val x = r.left + p.measureText(value) + ring * 1.6f
        s.canvas.drawCircle(x, base - cap + ring, ring, s.stroke(s.accent, ring * 0.7f))
    }

    /** Condition with feels-like on one line, the day's range on the next. */
    private fun details(s: Scene, w: WeatherState, r: RectF) {
        val today = w.daysFrom(s.now.toInstant()).first()
        s.canvas.drawText(w.condition, r.left, r.top + 12f * s.k, s.paint(12.5f * s.k, s.text, weight = 500))
        s.canvas.drawText("FEELS ${WeatherKit.deg(s, w.feelsLike)}", r.right, r.top + 12f * s.k, WeatherKit.mono(s, 8.5f, s.ink(0.58f), Paint.Align.RIGHT, 0.06f))
        s.canvas.drawText("H ${WeatherKit.deg(s, today.high)}  ·  L ${WeatherKit.deg(s, today.low)}", r.left, r.bottom - 1f, WeatherKit.mono(s, 8.5f, s.ink(0.58f), tracking = 0.06f))
    }

    /** The next [count] hours as columns: hour, glyph, temperature, rain chance. */
    private fun hours(s: Scene, w: WeatherState, r: RectF, count: Int) {
        val list = w.hoursFrom(s.now.toInstant()).take(count)
        val colW = r.width() / count
        val glyph = min(colW * 0.52f, r.height() * 0.34f)
        list.forEachIndexed { i, h ->
            val cx = r.left + colW * i + colW / 2f
            val label = if (i == 0) "NOW" else WeatherKit.hourLabel(s, h.time).uppercase()
            s.canvas.drawText(label, cx, r.top + 9f * s.k, WeatherKit.mono(s, 8f, s.ink(if (i == 0) 0.9f else 0.5f), Paint.Align.CENTER, 0.04f))
            val gy = r.top + 12f * s.k + glyph * 0.6f
            WeatherKit.glyph(s.canvas, WeatherMath.sky(h.code), WeatherKit.isDayAt(w, h.time), cx, gy, glyph, s.ink(0.85f), s.accent, Layer.ALL, 0)
            s.canvas.drawText(WeatherKit.deg(s, h.temperature), cx + 1.5f, gy + glyph * 0.62f + 12f * s.k, s.paint(12f * s.k, s.text, align = Paint.Align.CENTER))
            if (h.precipitationChance >= 10 && r.height() > 70f) {
                s.canvas.drawText("${h.precipitationChance}%", cx, r.bottom, WeatherKit.mono(s, 7.5f, s.ink(0.9f, WeatherKit.RAIN), Paint.Align.CENTER, 0.02f))
            }
        }
    }

    private fun stats(s: Scene, w: WeatherState, r: RectF) {
        val f = WeatherKit.fahrenheit(s)
        val today = w.daysFrom(s.now.toInstant()).first()
        val items = listOf(
            "WIND" to "${WeatherMath.speed(w.windKmh, f)} ${WeatherMath.compass(w.windDirection)}",
            "HUMIDITY" to "${w.humidity}%",
            "UV" to "%.0f %s".format(w.uv, WeatherMath.uvLabel(w.uv)),
            (if (w.isDay) "SUNSET" else "SUNRISE") to WeatherKit.clockLabel(s, if (w.isDay) today.sunset else today.sunrise),
        )
        val colW = r.width() / items.size
        items.forEachIndexed { i, (label, value) ->
            val x = r.left + colW * i
            s.canvas.drawText(label, x, r.top + 10f, WeatherKit.mono(s, 7.5f, s.ink(0.45f)))
            s.canvas.drawText(value, x, r.top + 26f * s.k, s.paint(11f * s.k, s.text, weight = 500))
        }
        s.canvas.drawText(WeatherKit.freshness(s, w).uppercase(), r.left, r.bottom, WeatherKit.mono(s, 7.5f, s.ink(0.35f)))
    }
}
