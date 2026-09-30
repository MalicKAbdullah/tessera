package com.malickabdullah.tessera.designs.sky

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import kotlin.math.min
import kotlin.math.roundToInt

object SkyMoon : WidgetDesign {
    override val id = "sky.moon"
    override val category = Category.SKY
    override val name = "Moon Phase"
    override val blurb = "Tonight's moon with its true terminator, how much is lit, its age and the next full and new moon."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 400, text = 0xFFEDEBE6, accent = 0xFFB8C4FF, background = 0xFF0E0F13, radius = 30f, padding = 16f)
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CONTENT)

    /** The lit fraction moves about 1% per 2 hours near the quarters. */
    override fun liveKey(scene: SceneInputs): String =
        "${scene.now.toLocalDate()}|${scene.now.hour / 2}|${(scene.data.skyPlace?.latitude ?: 1.0) < 0}"

    override fun draw(s: Scene) {
        val b = s.box
        val phase = Astronomy.moonPhase(s.now.toInstant())
        val pct = "${(phase.illumination * 100).roundToInt()}%"
        when {
            s.h >= 300f && s.w >= 300f -> large(s, b, pct)
            s.w >= s.h * 1.4f -> wide(s, b, pct)
            else -> {
                val r = min(b.width(), b.height() - 34f * s.k) / 2f
                SkyKit.moonAt(s, b.centerX(), b.top + r, r, phase)
                s.canvas.drawText(phase.name.uppercase(), b.centerX(), b.bottom - 16f * s.k, SkyKit.label(s, 9.5f, s.ink(0.7f), Paint.Align.CENTER))
                s.canvas.drawText(pct, b.centerX(), b.bottom, s.paint(14f * s.k, s.text, align = Paint.Align.CENTER))
            }
        }
    }

    private fun wide(s: Scene, b: RectF, pct: String) {
        val phase = Astronomy.moonPhase(s.now.toInstant())
        val r = b.height() / 2f
        SkyKit.moonAt(s, b.left + r, b.centerY(), r, phase)
        val x = b.left + r * 2 + 20f
        s.canvas.drawText(phase.name.uppercase(), x, b.top + 10f, SkyKit.label(s, 9.5f, s.accent))
        val size = s.fit("100%", (b.right - x) * 0.7f, b.height() * 0.42f) * s.hero
        s.canvas.drawText(pct, x - 1f, b.top + 14f + size * 0.8f, s.paint(size, s.text))
        s.canvas.drawText("LIT · DAY ${phase.ageDays.toInt()}", x, b.top + 28f + size * 0.8f, SkyKit.label(s, 9f))
        val now = s.now.toInstant()
        nextRow(s, x, b.bottom - 16f, b.right, "FULL", Astronomy.nextPhase(now, full = true))
        nextRow(s, x, b.bottom, b.right, "NEW", Astronomy.nextPhase(now, full = false))
    }

    private fun nextRow(s: Scene, x: Float, y: Float, right: Float, tag: String, at: java.time.Instant) {
        s.canvas.drawText(tag, x, y, SkyKit.label(s, 9f))
        val date = at.atZone(s.now.zone)
        s.canvas.drawText(
            "${date.format(SkyKit.dayMonth)}  ${SkyKit.time(s, at)}",
            right,
            y,
            s.paint(11f * s.k, s.ink(0.85f), weight = 400, align = Paint.Align.RIGHT),
        )
    }

    private fun large(s: Scene, b: RectF, pct: String) {
        val now = s.now.toInstant()
        val phase = Astronomy.moonPhase(now)
        s.canvas.drawText(phase.name.uppercase(), b.left, b.top + 10f, SkyKit.label(s, 10f, s.accent))
        s.canvas.drawText("$pct LIT", b.right, b.top + 10f, SkyKit.label(s, 10f, s.ink(0.75f), Paint.Align.RIGHT))
        val stripH = 44f
        val statsH = 40f
        val discArea = RectF(b.left, b.top + 22f, b.right, b.bottom - stripH - statsH - 20f)
        val r = min(discArea.width(), discArea.height()) / 2f
        SkyKit.moonAt(s, discArea.centerX(), discArea.centerY(), r, phase)

        val stats = listOf(
            "AGE" to "%.1f d".format(phase.ageDays),
            "FULL" to Astronomy.nextPhase(now, full = true).atZone(s.now.zone).format(SkyKit.dayMonth),
            "NEW" to Astronomy.nextPhase(now, full = false).atZone(s.now.zone).format(SkyKit.dayMonth),
        )
        val statsTop = discArea.bottom + 12f
        val colW = b.width() / stats.size
        stats.forEachIndexed { i, (k, v) ->
            val cx = b.left + colW * i + colW / 2f
            s.canvas.drawText(k, cx, statsTop + 10f, SkyKit.label(s, 8.5f, align = Paint.Align.CENTER))
            s.canvas.drawText(v, cx, statsTop + 30f, s.paint(13f * s.k, s.text, align = Paint.Align.CENTER))
        }

        // The coming week, one small disc per night.
        val strip = RectF(b.left, b.bottom - stripH, b.right, b.bottom)
        s.canvas.drawLine(strip.left, strip.top - 6f, strip.right, strip.top - 6f, s.stroke(s.ink(0.1f), 1f))
        val days = 7
        val cell = strip.width() / days
        val mr = min(cell * 0.3f, 10f)
        for (d in 1..days) {
            val at = s.now.plusDays(d.toLong()).withHour(22).withMinute(0)
            val cx = strip.left + cell * (d - 1) + cell / 2f
            SkyKit.moonAt(s, cx, strip.top + 6f + mr, mr, Astronomy.moonPhase(at.toInstant()))
            s.canvas.drawText(
                at.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                cx,
                strip.bottom,
                SkyKit.label(s, 8.5f, s.ink(0.5f), Paint.Align.CENTER),
            )
        }
    }
}
