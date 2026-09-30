package com.malickabdullah.tessera.designs.weather

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.DayForecast
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.min

object WeatherSun : WidgetDesign {
    override val id = "weather.sun"
    override val category = Category.WEATHER
    override val name = "Sun Arc"
    override val blurb = "The sun's path from sunrise to sunset with where it is now, daylight length and the time to the next turn."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("grotesk", 500, text = 0xFF15161A, accent = 0xFF6C4DFF, background = 0xFFEEF0F3, radius = 30f)
    override val toggles = listOf(hourFormatToggle, WeatherKit.unitsToggle)
    override val signals = setOf(Signal.WEATHER, Signal.CONTENT)

    /** The sun moves about a pixel in ten minutes on a 4×2 arc, so redraw at that pace. */
    override fun liveKey(scene: SceneInputs) = WeatherKit.key(scene, "${scene.now.toEpochSecond() / 600}")

    private data class SunPath(val rise: LocalDateTime, val set: LocalDateTime, val nextRise: LocalDateTime, val up: Float?, val down: Float)

    private fun sky(now: LocalDateTime, days: List<DayForecast>): SunPath {
        val today = days[0]
        return when {
            now.isBefore(today.sunrise) -> {
                val prevSet = today.sunset.minusDays(1)
                SunPath(today.sunrise, today.sunset, today.sunrise, null, WeatherMath.nightProgress(now, prevSet, today.sunrise))
            }
            now.isAfter(today.sunset) -> {
                val next = days.getOrNull(1)?.sunrise ?: today.sunrise.plusDays(1)
                SunPath(today.sunrise, today.sunset, next, null, WeatherMath.nightProgress(now, today.sunset, next))
            }
            else -> SunPath(today.sunrise, today.sunset, today.sunrise.plusDays(1), WeatherMath.sunProgress(now, today.sunrise, today.sunset), 0f)
        }
    }

    override fun draw(s: Scene) {
        val w = WeatherKit.ready(s) ?: return
        val days = w.daysFrom(s.now.toInstant())
        val b = s.box
        val now = w.localTime(s.now.toInstant())
        val sun = sky(now, days)
        WeatherKit.header(s, RectF(b.left, b.top, b.right, b.top + 12f), w.city, w)

        val wide = s.w >= 280f
        val footH = 30f * s.k
        val arcBox = if (wide) RectF(b.left + b.width() * 0.4f, b.top + 22f, b.right, b.bottom - 16f) else RectF(b.left, b.top + 22f, b.right, b.bottom - footH - 6f)
        val baseline = arcBox.bottom - arcBox.height() * 0.22f
        val height = min(arcBox.height() * 0.7f, arcBox.width() * 0.5f)
        val left = arcBox.left + 6f
        val right = arcBox.right - 6f

        s.canvas.drawRect(RectF(arcBox.left, baseline, arcBox.right, arcBox.bottom), s.fill(s.ink(0.04f)))
        s.canvas.drawLine(arcBox.left, baseline, arcBox.right, baseline, s.stroke(s.ink(0.3f), 1f))
        val path = Path()
        val lit = Path()
        val t = sun.up ?: 0f
        for (i in 0..60) {
            val (x, y) = WeatherMath.arcPoint(i / 60f, left, right, baseline, height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            if (sun.up != null && i / 60f <= t) if (i == 0) lit.moveTo(x, y) else lit.lineTo(x, y)
        }
        s.canvas.drawPath(path, Paint(s.stroke(s.ink(0.28f), 1.2f)).apply { pathEffect = DashPathEffect(floatArrayOf(2.5f, 3.5f), 0f) })
        val sunR = min(9f, height * 0.1f)
        if (sun.up != null) {
            val (x, y) = WeatherMath.arcPoint(t, left, right, baseline, height)
            lit.lineTo(x, y)
            s.canvas.drawPath(lit, s.stroke(s.accent, 2f))
            s.canvas.drawCircle(x, y, sunR * 2.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x, y, sunR * 2.6f, s.ink(0.35f, s.accent), s.ink(0f, s.accent), Shader.TileMode.CLAMP)
            })
            s.canvas.drawCircle(x, y, sunR, s.fill(s.accent))
        } else {
            // Below the horizon the sun travels a shallow mirrored arc from sunset (right) back to sunrise (left).
            val (x, yUp) = WeatherMath.arcPoint(1f - sun.down, left, right, baseline, (arcBox.bottom - baseline) * 0.7f)
            val y = baseline + (baseline - yUp)
            s.canvas.drawCircle(x, y, sunR * 0.8f, s.stroke(s.ink(0.45f, s.accent), 1.4f))
            Kit.moon(s.canvas, (left + right) / 2f, baseline - height * 0.62f, sunR * 1.6f, s.fill(s.ink(0.55f)))
        }
        for (x in listOf(left, right)) s.canvas.drawCircle(x, baseline, 2.4f, s.fill(s.ink(0.5f)))

        val next = when {
            sun.up != null -> "SUNSET IN ${Kit.duration(Duration.between(now, sun.set).toMillis())}"
            else -> "SUNRISE IN ${Kit.duration(Duration.between(now, if (now.isBefore(sun.rise)) sun.rise else sun.nextRise).toMillis())}"
        }
        val daylight = "${Kit.duration(Duration.between(sun.rise, sun.set).toMillis())} DAYLIGHT"
        val lbl = WeatherKit.mono(s, 7.5f, s.ink(0.5f))
        val time = s.paint(13f * s.k, s.text, weight = 500)
        if (wide) {
            val col = RectF(b.left, b.top + 24f, arcBox.left - 12f, b.bottom)
            val big = s.paint(min(30f * s.k, s.fit(if (s.use24h) "88:88" else "88:88 PM", col.width(), 40f)), s.text)
            val focus = if (sun.up != null) sun.set else if (now.isBefore(sun.rise)) sun.rise else sun.nextRise
            s.canvas.drawText(if (sun.up != null) "SUNSET" else "SUNRISE", col.left, col.top + 10f, lbl)
            s.canvas.drawText(WeatherKit.clockLabel(s, focus), col.left, col.top + 12f + big.textSize, big)
            s.canvas.drawText(next, col.left, col.top + 26f + big.textSize, WeatherKit.mono(s, 8f, s.accent))
            s.canvas.drawText("RISE ${WeatherKit.clockLabel(s, sun.rise)}", col.left, col.bottom - 28f * s.k, lbl)
            s.canvas.drawText("SET ${WeatherKit.clockLabel(s, sun.set)}", col.left, col.bottom - 16f * s.k, lbl)
            s.canvas.drawText("$daylight · UV ${"%.0f".format(days[0].uvMax)}", col.left, col.bottom - 4f, lbl)
        } else {
            val foot = RectF(b.left, b.bottom - footH, b.right, b.bottom)
            s.canvas.drawText(WeatherKit.clockLabel(s, sun.rise), foot.left, foot.top + 12f * s.k, time)
            s.canvas.drawText(WeatherKit.clockLabel(s, sun.set), foot.right, foot.top + 12f * s.k, Paint(time).apply { textAlign = Paint.Align.RIGHT })
            s.canvas.drawText(next, foot.left, foot.bottom, WeatherKit.mono(s, 7.5f, s.accent, tracking = 0.06f))
        }
    }
}
