package com.malickabdullah.tessera.designs.weather

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.WeatherState
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import com.malickabdullah.tessera.engine.withAlpha
import kotlin.math.min
import kotlin.random.Random

object WeatherSky : WidgetDesign {
    override val id = "weather.sky"
    override val category = Category.WEATHER
    override val name = "Sky"
    override val blurb = "A card painted in the colours of the sky outside: blue noon, amber dusk, slate rain, starry night."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("sans", 300, text = 0xFFFFFFFF, accent = 0xFFFFD27A, background = 0xFF2E7BE6, radius = 30f)
    override val toggles = listOf(WeatherKit.unitsToggle, hourFormatToggle, WeatherKit.motionToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)
    override val motion = WeatherKit.MOTION

    override fun liveKey(scene: SceneInputs): String {
        val w = WeatherKit.usable(scene) ?: return WeatherKit.key(scene)
        return WeatherKit.key(scene, phase(scene, w).name)
    }

    private fun phase(s: SceneInputs, w: WeatherState): Phase {
        val now = w.localTime(s.now.toInstant())
        val today = w.daysFrom(s.now.toInstant()).first()
        return WeatherMath.phase(now, today.sunrise, today.sunset)
    }

    override fun draw(s: Scene) {
        val w = WeatherKit.usable(s)
        if (w == null) {
            paintSky(s, Sky.CLOUDY, Phase.DAY)
            WeatherKit.ready(s)
            return
        }
        val sky = WeatherKit.sky(w)
        val phase = phase(s, w)
        paintSky(s, sky, phase)
        val b = s.box
        val day = phase != Phase.NIGHT
        val glyphInk = withAlpha(s.text, 0.92f)
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)
        when {
            s.h >= 300f -> {
                val glyph = b.width() * 0.3f
                WeatherKit.liveGlyph(s, sky, day, b.right - glyph * 0.55f, b.top + 24f + glyph * 0.5f, glyph, glyphInk, s.accent, s.flag("motion"))
                val hero = RectF(b.left, b.top + 20f, b.right - glyph, b.top + b.height() * 0.46f)
                temperature(s, w, hero)
                glass(s, w, RectF(b.left, b.top + b.height() * 0.52f, b.right, b.bottom))
            }
            else -> {
                val wide = s.w >= 280f
                val glyph = if (wide) b.height() * 0.62f else b.width() * 0.42f
                WeatherKit.liveGlyph(s, sky, day, b.right - glyph * 0.55f, b.top + 18f + glyph * 0.5f, glyph, glyphInk, s.accent, s.flag("motion"))
                val top = if (wide) b.top + 18f else b.top + 18f + glyph * 0.75f
                temperature(s, w, RectF(b.left, top, if (wide) b.right - glyph * 1.1f else b.right, b.bottom))
            }
        }
    }

    /** Full-bleed sky over the style's surface: a two-stop gradient, a sun or moon glow, stars at night, streaks in rain. */
    private fun paintSky(s: Scene, sky: Sky, phase: Phase) {
        val (top, bottom) = WeatherMath.palette(sky, phase)
        s.canvas.drawRect(0f, 0f, s.w, s.h, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, s.h, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
        })
        val glow = when (phase) {
            Phase.DAWN, Phase.DUSK -> 0x66FFC38A
            Phase.DAY -> 0x40FFFFFF
            Phase.NIGHT -> 0x22B8C8FF
        }.toInt()
        val gx = if (phase == Phase.DAWN) s.w * 0.15f else s.w * 0.85f
        val gy = if (phase == Phase.DAWN || phase == Phase.DUSK) s.h * 0.95f else s.h * 0.1f
        val gr = maxOf(s.w, s.h) * 0.8f
        s.canvas.drawCircle(gx, gy, gr, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(gx, gy, gr, glow, glow and 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        // Seeded by size so the same widget keeps the same stars from render to render.
        val random = Random((s.w * 31 + s.h).toInt())
        if (phase == Phase.NIGHT && (sky == Sky.CLEAR || sky == Sky.PARTLY)) {
            repeat((s.w * s.h / 900f).toInt()) {
                val x = random.nextFloat() * s.w
                val y = random.nextFloat() * s.h * 0.7f
                val a = 0.2f + random.nextFloat() * 0.6f
                s.canvas.drawCircle(x, y, 0.5f + random.nextFloat() * 0.7f, s.fill(withAlpha(0xFFFFFFFF.toInt(), a)))
            }
        }
        if (sky == Sky.RAIN || sky == Sky.STORM || sky == Sky.DRIZZLE) {
            val streak = s.stroke(withAlpha(0xFFFFFFFF.toInt(), 0.07f), 1f)
            repeat((s.w * s.h / 1400f).toInt()) {
                val x = random.nextFloat() * (s.w + 20f)
                val y = random.nextFloat() * s.h
                s.canvas.drawLine(x, y, x - 3f, y + 10f, streak)
            }
        }
        if (sky == Sky.FOG) {
            for (i in 1..4) s.canvas.drawRect(0f, s.h * (0.4f + i * 0.12f), s.w, s.h, s.fill(withAlpha(0xFFFFFFFF.toInt(), 0.05f)))
        }
    }

    private fun temperature(s: Scene, w: WeatherState, r: RectF) {
        val detailH = 30f * s.k
        val area = RectF(r.left, r.top, r.right, r.bottom - detailH)
        val value = WeatherKit.deg(s, w.temperature)
        val size = s.fit("-88°", area.width(), area.height()) * s.hero
        s.canvas.drawText(value, r.left - size * 0.03f, area.bottom - size * 0.08f, s.paint(size, s.text))
        val today = w.daysFrom(s.now.toInstant()).first()
        s.canvas.drawText(w.condition, r.left, r.bottom - 15f * s.k, s.paint(13f * s.k, s.text, weight = 500))
        val range = "H ${WeatherKit.deg(s, today.high)} · L ${WeatherKit.deg(s, today.low)} · "
        s.canvas.drawText("${range}Feels ${WeatherKit.deg(s, w.feelsLike)}", r.left, r.bottom, s.paint(10f * s.k, s.ink(0.78f), weight = 400))
    }

    /** Frosted panel with the next hours and the next days. */
    private fun glass(s: Scene, w: WeatherState, r: RectF) {
        Kit.roundRect(s.canvas, r, 18f, s.fill(withAlpha(0xFFFFFFFF.toInt(), 0.12f)))
        Kit.roundRect(s.canvas, r, 18f, s.stroke(withAlpha(0xFFFFFFFF.toInt(), 0.16f), 1f))
        val inner = RectF(r.left + 12f, r.top + 10f, r.right - 12f, r.bottom - 10f)
        val hours = w.hoursFrom(s.now.toInstant()).take(6)
        val top = RectF(inner.left, inner.top, inner.right, inner.top + inner.height() * 0.52f)
        val colW = top.width() / hours.size
        val glyph = min(colW * 0.5f, top.height() * 0.36f)
        hours.forEachIndexed { i, h ->
            val cx = top.left + colW * i + colW / 2f
            s.canvas.drawText(if (i == 0) "Now" else WeatherKit.hourLabel(s, h.time), cx, top.top + 10f * s.k, s.paint(9f * s.k, s.ink(0.8f), weight = 400, align = Paint.Align.CENTER))
            WeatherKit.glyph(s.canvas, WeatherMath.sky(h.code), WeatherKit.isDayAt(w, h.time), cx, top.centerY() + 2f, glyph, s.ink(0.9f), s.accent, Layer.ALL, 0)
            s.canvas.drawText(WeatherKit.deg(s, h.temperature), cx + 1.5f, top.bottom, s.paint(12f * s.k, s.text, weight = 500, align = Paint.Align.CENTER))
        }
        val line = inner.top + inner.height() * 0.6f
        s.canvas.drawLine(inner.left, line, inner.right, line, s.stroke(withAlpha(0xFFFFFFFF.toInt(), 0.18f), 1f))
        val days = w.daysFrom(s.now.toInstant()).drop(1).take(3)
        val dayRow = RectF(inner.left, line + 6f, inner.right, inner.bottom)
        val dayW = dayRow.width() / days.size
        days.forEachIndexed { i, d ->
            val x = dayRow.left + dayW * i
            val name = d.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
            val g = min(dayRow.height() * 0.6f, 18f)
            WeatherKit.glyph(s.canvas, WeatherMath.sky(d.code), true, x + g / 2f, dayRow.centerY(), g, s.ink(0.9f), s.accent, Layer.ALL, 0)
            s.canvas.drawText(name, x + g + 6f, dayRow.centerY() - 1f, s.paint(9.5f * s.k, s.ink(0.8f), weight = 400))
            s.canvas.drawText("${WeatherKit.deg(s, d.high)} / ${WeatherKit.deg(s, d.low)}", x + g + 6f, dayRow.centerY() + 11f * s.k, s.paint(10.5f * s.k, s.text, weight = 500))
        }
    }
}
