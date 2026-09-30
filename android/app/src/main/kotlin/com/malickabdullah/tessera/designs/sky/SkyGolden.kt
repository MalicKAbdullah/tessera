package com.malickabdullah.tessera.designs.sky

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.data.SunDay
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.Instant

object SkyGolden : WidgetDesign {
    override val id = "sky.golden"
    override val category = Category.SKY
    override val name = "Golden Hour"
    override val blurb = "Today's night, blue hour, golden hour and daylight on one timeline, with the next golden hour."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("serif", 400, text = 0xFFF2F3F5, accent = 0xFFB8C4FF, background = 0xFF14161B, radius = 28f, padding = 16f)
    override val toggles = listOf(hourFormatToggle)
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = SkyKit.key(scene, 5)

    private data class Window(val label: String, val start: Instant?, val end: Instant?)

    override fun draw(s: Scene) {
        val place = s.data.skyPlace ?: return SkyKit.noPlace(s)
        val b = s.box
        val date = s.now.toLocalDate()
        val day = Astronomy.sunDay(date, place.latitude, place.longitude)
        val tomorrow = Astronomy.sunDay(date.plusDays(1), place.latitude, place.longitude)
        val large = s.h >= 300f
        val now = s.now.toInstant()

        val next = listOf(
            day.goldenMorningStart to day.goldenMorningEnd,
            day.goldenEveningStart to day.goldenEveningEnd,
            tomorrow.goldenMorningStart to tomorrow.goldenMorningEnd,
        ).firstOrNull { (a, e) -> a != null && e != null && e.isAfter(now) }
        val live = next != null && !next.first!!.isAfter(now)
        s.canvas.drawText(
            if (live) "GOLDEN HOUR · NOW" else "NEXT GOLDEN HOUR",
            b.left,
            b.top + 9f,
            SkyKit.label(s, 9f, if (live) SkyKit.GOLD else s.ink(0.55f)),
        )
        s.canvas.drawText(SkyKit.placeName(s), b.right, b.top + 9f, SkyKit.label(s, 9f, align = Paint.Align.RIGHT))
        val heroText = next?.let { (a, e) -> "${SkyKit.time(s, a)} – ${SkyKit.time(s, e)}" } ?: "Not today"
        val heroH = if (large) b.height() * 0.2f else b.height() * 0.3f
        val hero = s.paint(s.fit(heroText, b.width(), heroH) * s.hero, s.text)
        val baseline = b.top + 18f + hero.textSize * 0.8f
        s.canvas.drawText(heroText, b.left - 1f, baseline, hero)

        val barTop = baseline + 12f
        val bar = RectF(b.left, barTop, b.right, barTop + if (large) 30f else 14f)
        timeline(s, bar, place)

        val windows = listOf(
            Window("BLUE", day.blueStart, day.goldenMorningStart),
            Window("GOLDEN", day.goldenMorningStart, day.goldenMorningEnd),
            Window("GOLDEN", day.goldenEveningStart, day.goldenEveningEnd),
            Window("BLUE", day.goldenEveningEnd, day.blueEnd),
        )
        if (large) {
            rows(s, RectF(b.left, bar.bottom + 22f, b.right, b.bottom), windows, day)
        } else {
            val colW = b.width() / windows.size
            windows.forEachIndexed { i, w ->
                val x = b.left + colW * i
                val c = if (w.label == "BLUE") SkyKit.BLUE else SkyKit.GOLD
                s.canvas.drawCircle(x + 3f, b.bottom - 19f, 3f, s.fill(c))
                s.canvas.drawText(if (i < 2) "AM ${w.label}" else "PM ${w.label}", x + 10f, b.bottom - 16f, SkyKit.label(s, 7.5f))
                s.canvas.drawText(SkyKit.time(s, w.start), x, b.bottom, s.paint(12f * s.k, s.ink(0.9f)))
            }
        }
    }

    private fun timeline(s: Scene, r: RectF, place: com.malickabdullah.tessera.data.SkyPlace) {
        val alts = SkyKit.altitudes(s, place, 5)
        val segW = r.width() / (alts.size - 1)
        val rad = r.height() / 2f
        s.canvas.save()
        s.canvas.clipPath(android.graphics.Path().apply { addRoundRect(r, rad, rad, android.graphics.Path.Direction.CW) })
        for (i in 0 until alts.size - 1) {
            val x = r.left + segW * i
            s.canvas.drawRect(x, r.top, x + segW + 0.5f, r.bottom, s.fill(SkyKit.color(s, SkyKit.light((alts[i] + alts[i + 1]) / 2))))
        }
        s.canvas.restore()
        val tickP = s.stroke(s.ink(0.3f), 1f)
        for (h in 0..24 step 3) {
            val x = r.left + r.width() * h / 24f
            s.canvas.drawLine(x, r.bottom + 3f, x, r.bottom + if (h % 6 == 0) 7f else 5f, tickP)
        }
        val nx = r.left + r.width() * SkyKit.nowFraction(s)
        s.canvas.drawLine(nx, r.top - 4f, nx, r.bottom + 4f, s.stroke(s.text, 2f))
        s.canvas.drawCircle(nx, r.top - 5f, 2.6f, s.fill(s.text))
    }

    private fun rows(s: Scene, r: RectF, windows: List<Window>, day: SunDay) {
        val all = listOf(
            Triple("Blue hour", windows[0], SkyKit.BLUE),
            Triple("Golden hour", windows[1], SkyKit.GOLD),
            Triple("Sunrise · sunset", Window("", day.sunrise, day.sunset), s.accent),
            Triple("Golden hour", windows[2], SkyKit.GOLD),
            Triple("Blue hour", windows[3], SkyKit.BLUE),
        )
        val rowH = r.height() / all.size
        all.forEachIndexed { i, (label, w, c) ->
            val cy = r.top + rowH * i + rowH / 2f
            if (i > 0) s.canvas.drawLine(r.left, r.top + rowH * i, r.right, r.top + rowH * i, s.stroke(s.ink(0.08f), 1f))
            s.canvas.drawCircle(r.left + 4f, cy, 4f, s.fill(c))
            s.textMid(label, r.left + 16f, cy, s.paint(14f * s.k, s.ink(0.85f)))
            s.textMid(
                "${SkyKit.time(s, w.start)}  –  ${SkyKit.time(s, w.end)}",
                r.right,
                cy,
                s.paint(12f * s.k, s.ink(0.75f), font = "mono", weight = 400, align = Paint.Align.RIGHT),
            )
        }
    }
}
