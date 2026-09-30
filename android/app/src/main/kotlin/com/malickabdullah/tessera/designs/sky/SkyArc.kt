package com.malickabdullah.tessera.designs.sky

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.data.SkyPlace
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import kotlin.math.max
import kotlin.math.min

object SkyArc : WidgetDesign {
    override val id = "sky.arc"
    override val category = Category.SKY
    override val name = "Sun Arc"
    override val blurb = "The sun's real path through today, where it is now, and sunrise and sunset."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.SMALL)
    override val defaults = Style.of(
        "sans", 500, text = 0xFF16171A, accent = 0xFF6C4DFF, background = 0xFFEEF0F3,
        kind = BgKind.GRADIENT, background2 = 0xFFDCE3EE, radius = 30f, padding = 16f,
    )
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = SkyKit.key(scene, 10)

    override fun draw(s: Scene) {
        val place = s.data.skyPlace ?: return SkyKit.noPlace(s)
        val b = s.box
        val day = Astronomy.sunDay(s.now.toLocalDate(), place.latitude, place.longitude)
        val small = s.w < s.h * 1.4f
        s.canvas.drawText(SkyKit.placeName(s), b.left, b.top + 9f, SkyKit.label(s, 9f))
        s.canvas.drawText(
            if (small) SkyKit.hm(day.dayLengthMinutes).uppercase() else "${SkyKit.hm(day.dayLengthMinutes).uppercase()} OF DAYLIGHT",
            b.right,
            b.top + 9f,
            SkyKit.label(s, 9f, s.ink(0.55f), Paint.Align.RIGHT),
        )
        val footer = if (small) 38f * s.k else 30f * s.k
        val chart = RectF(b.left, b.top + 20f, b.right, b.bottom - footer)
        arc(s, chart, place)
        val rise = SkyKit.time(s, day.sunrise)
        val set = SkyKit.time(s, day.sunset)
        if (small) {
            val big = s.paint(s.fit("88:88 pm", b.width() * 0.5f, footer * 0.6f) * s.hero, s.text)
            s.canvas.drawText(rise, b.left, b.bottom, big)
            s.canvas.drawText(set, b.right, b.bottom, Paint(big).apply { textAlign = Paint.Align.RIGHT })
            s.canvas.drawText("RISE", b.left, b.bottom - big.textSize - 2f, SkyKit.label(s, 8.5f))
            s.canvas.drawText("SET", b.right, b.bottom - big.textSize - 2f, SkyKit.label(s, 8.5f, align = Paint.Align.RIGHT))
        } else {
            val size = min(18f * s.k, footer * 0.75f)
            val p = s.paint(size, s.text)
            val l = SkyKit.label(s, 8.5f)
            s.canvas.drawText("SUNRISE", b.left, b.bottom - size - 2f, l)
            s.canvas.drawText(rise, b.left, b.bottom, p)
            s.canvas.drawText("NOON", b.centerX(), b.bottom - size - 2f, SkyKit.label(s, 8.5f, align = Paint.Align.CENTER))
            s.canvas.drawText(SkyKit.time(s, day.noon), b.centerX(), b.bottom, Paint(p).apply { textAlign = Paint.Align.CENTER })
            s.canvas.drawText("SUNSET", b.right, b.bottom - size - 2f, SkyKit.label(s, 8.5f, align = Paint.Align.RIGHT))
            s.canvas.drawText(set, b.right, b.bottom, Paint(p).apply { textAlign = Paint.Align.RIGHT })
        }
    }

    /** Altitude over the local day; the part below the horizon is compressed into a shallow trough. */
    private fun arc(s: Scene, r: RectF, place: SkyPlace) {
        val alts = SkyKit.altitudes(s, place, 10)
        val top = max(alts.max(), 10.0)
        val bottom = min(alts.min(), -10.0)
        val horizon = r.top + r.height() * 0.72f
        fun y(a: Double): Float = if (a >= 0) {
            horizon - (a / top).toFloat() * (horizon - r.top - 6f)
        } else {
            horizon + (a / bottom).toFloat() * (r.bottom - horizon)
        }
        fun x(i: Int): Float = r.left + r.width() * i / (alts.size - 1)
        val curve = Path().apply {
            alts.forEachIndexed { i, a -> if (i == 0) moveTo(x(i), y(a)) else lineTo(x(i), y(a)) }
        }
        val area = Path(curve).apply {
            lineTo(r.right, horizon)
            lineTo(r.left, horizon)
            close()
        }
        s.canvas.save()
        s.canvas.clipRect(r.left, r.top, r.right, horizon)
        s.canvas.drawPath(
            area,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, r.top, 0f, horizon, s.ink(0.28f, s.accent), s.ink(0.02f, s.accent), Shader.TileMode.CLAMP)
            },
        )
        s.canvas.drawPath(curve, s.stroke(s.accent, 2f))
        s.canvas.restore()
        s.canvas.save()
        s.canvas.clipRect(r.left, horizon, r.right, r.bottom + 2f)
        s.canvas.drawPath(curve, s.stroke(s.ink(0.28f), 1.4f).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(2f, 3f), 0f) })
        s.canvas.restore()
        s.canvas.drawLine(r.left, horizon, r.right, horizon, s.stroke(s.ink(0.35f), 1f))
        for (hour in 0..24 step 6) {
            val hx = r.left + r.width() * hour / 24f
            s.canvas.drawLine(hx, horizon - 2.5f, hx, horizon + 2.5f, s.stroke(s.ink(0.35f), 1f))
        }

        val f = SkyKit.nowFraction(s)
        val nowAlt = Astronomy.sunPosition(s.now.toInstant(), place.latitude, place.longitude).altitude
        val sx = r.left + r.width() * f
        val sy = y(nowAlt)
        val up = nowAlt >= 0
        val c = if (up) s.accent else s.ink(0.55f)
        s.canvas.drawCircle(sx, sy, 11f, s.fill(s.ink(if (up) 0.16f else 0.08f, c)))
        s.canvas.drawCircle(sx, sy, 7f, s.fill(s.ink(if (up) 0.3f else 0.12f, c)))
        s.canvas.drawCircle(sx, sy, 4.2f, s.fill(c))
        s.canvas.drawText(
            "%+.0f°".format(nowAlt),
            (sx + 13f).coerceAtMost(r.right - 22f),
            (sy - 8f).coerceIn(r.top + 8f, r.bottom),
            SkyKit.label(s, 8.5f, s.ink(0.7f)),
        )
    }
}
