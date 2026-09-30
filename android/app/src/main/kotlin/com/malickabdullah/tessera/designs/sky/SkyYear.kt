package com.malickabdullah.tessera.designs.sky

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.Astronomy
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object SkyYear : WidgetDesign {
    override val id = "sky.year"
    override val category = Category.SKY
    override val name = "Day Length"
    override val blurb = "Today's daylight on the curve of the whole year, and how much it changed since yesterday."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("condensed", 500, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF15171B, radius = 28f, padding = 16f)
    override val signals = setOf(Signal.CONTENT)

    private val month = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

    override fun liveKey(scene: SceneInputs): String {
        val p = scene.data.skyPlace
        return "${p?.latitude},${p?.longitude}|${scene.now.toLocalDate()}"
    }

    override fun draw(s: Scene) {
        val place = s.data.skyPlace ?: return SkyKit.noPlace(s)
        val b = s.box
        val today = s.now.toLocalDate()
        val lengths = Astronomy.dayLengths(today.year, place.latitude, place.longitude)
        val index = today.dayOfYear - 1
        val now = lengths[index]
        val yesterday = Astronomy.sunDay(today.minusDays(1), place.latitude, place.longitude).dayLengthMinutes
        val deltaSeconds = ((now - yesterday) * 60).roundToInt()
        val large = s.h >= 300f

        s.canvas.drawText("DAYLIGHT · ${SkyKit.placeName(place)}", b.left, b.top + 9f, SkyKit.label(s, 9f))
        val heroH = if (large) b.height() * 0.2f else b.height() * 0.34f
        val heroSize = s.fit("88h 88m", b.width() * 0.6f, heroH) * s.hero
        val hero = s.paint(heroSize, s.text)
        val baseline = b.top + 16f + heroSize * 0.82f
        s.canvas.drawText(SkyKit.hm(now), b.left - 1f, baseline, hero)
        val sign = if (deltaSeconds >= 0) "+" else "−"
        val d = abs(deltaSeconds)
        s.canvas.drawText(
            "$sign${d / 60}m ${"%02d".format(d % 60)}s",
            b.right,
            baseline - heroSize * 0.42f,
            s.paint(15f * s.k, s.accent, align = Paint.Align.RIGHT),
        )
        s.canvas.drawText("VS YESTERDAY", b.right, baseline, SkyKit.label(s, 8.5f, align = Paint.Align.RIGHT))

        val chart = RectF(b.left, baseline + 12f, b.right, b.bottom - 14f)
        curve(s, chart, lengths, index)
        val longest = lengths.indices.maxBy { lengths[it] }
        val shortest = lengths.indices.minBy { lengths[it] }
        val first = LocalDate.of(today.year, 1, 1)
        val l = SkyKit.label(s, 8.5f)
        s.canvas.drawText("LONGEST ${first.plusDays(longest.toLong()).format(month).uppercase()} · ${SkyKit.hm(lengths[longest])}", b.left, b.bottom, l)
        s.canvas.drawText(
            "SHORTEST ${first.plusDays(shortest.toLong()).format(month).uppercase()} · ${SkyKit.hm(lengths[shortest])}",
            b.right,
            b.bottom,
            SkyKit.label(s, 8.5f, align = Paint.Align.RIGHT),
        )
        if (large) months(s, chart)
    }

    private fun curve(s: Scene, r: RectF, lengths: List<Double>, today: Int) {
        val hi = lengths.max()
        val lo = lengths.min()
        val span = (hi - lo).coerceAtLeast(60.0)
        val mid = (hi + lo) / 2
        fun x(i: Int) = r.left + r.width() * i / (lengths.size - 1)
        fun y(v: Double) = r.centerY() - ((v - mid) / span).toFloat() * r.height() * 0.8f
        s.canvas.drawLine(r.left, y(720.0).coerceIn(r.top, r.bottom), r.right, y(720.0).coerceIn(r.top, r.bottom), s.stroke(s.ink(0.1f), 1f))
        val line = Path().apply { lengths.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) } }
        val fill = Path(line).apply {
            lineTo(r.right, r.bottom)
            lineTo(r.left, r.bottom)
            close()
        }
        s.canvas.drawPath(
            fill,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, r.top, 0f, r.bottom, s.ink(0.22f, s.accent), s.ink(0f, s.accent), Shader.TileMode.CLAMP)
            },
        )
        s.canvas.drawPath(line, s.stroke(s.ink(0.35f), 1.5f))
        val past = Path().apply { for (i in 0..today) if (i == 0) moveTo(x(i), y(lengths[i])) else lineTo(x(i), y(lengths[i])) }
        s.canvas.drawPath(past, s.stroke(s.accent, 2.2f))
        val tx = x(today)
        val ty = y(lengths[today])
        s.canvas.drawLine(tx, ty, tx, r.bottom, s.stroke(s.ink(0.4f, s.accent), 1f))
        s.canvas.drawCircle(tx, ty, 7f, s.fill(s.ink(0.25f, s.accent)))
        s.canvas.drawCircle(tx, ty, 3.8f, s.fill(s.accent))
    }

    private fun months(s: Scene, r: RectF) {
        val p = SkyKit.label(s, 7.5f, s.ink(0.35f), Paint.Align.CENTER)
        "JFMAMJJASOND".forEachIndexed { i, c ->
            s.canvas.drawText(c.toString(), r.left + r.width() * (i + 0.5f) / 12f, r.bottom - 2f, p)
        }
    }
}
