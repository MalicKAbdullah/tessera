package com.malickabdullah.tessera.designs.battery

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.BatteryHistory
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign

object BatteryHistoryChart : WidgetDesign {
    override val id = "battery.history"
    override val category = Category.BATTERY
    override val name = "24h History"
    override val blurb = "Your last day of charge as an area chart, with charging stretches and drain rate."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("sans", 500, text = 0xFFE6EAF2, accent = 0xFF7FB2FF, background = 0xFF10131A, radius = 26f, padding = 16f)
    override val signals = setOf(Signal.BATTERY)

    override fun liveKey(scene: SceneInputs) = BatteryKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val color = BatteryKit.levelColor(s)
        val headH = if (s.h >= 300f) 56f else 34f
        val big = s.paint(s.fit("100%", b.width() * 0.4f, headH) * s.hero, s.text)
        val fm = big.fontMetrics
        val baseline = b.top + headH / 2f - (fm.ascent + fm.descent) / 2f
        s.canvas.drawText("${bat.level}%", b.left, baseline, big)

        val rate = BatteryHistory.drainRate(s.data.batteryHistory)
        val rateText = when {
            bat.charging -> "Charging"
            rate == null -> "Measuring drain"
            rate > -0.2f -> "Steady"
            else -> "%.1f%%/h".format(rate).replace("-", "−")
        }
        val right = s.paint(12f * s.k, color, weight = 500, align = Paint.Align.RIGHT)
        s.canvas.drawText(rateText, b.right, b.top + headH / 2f - 2f, right)
        s.canvas.drawText(
            BatteryKit.estimate(s),
            b.right,
            b.top + headH / 2f + 13f,
            s.paint(10f * s.k, s.ink(0.55f), weight = 400, align = Paint.Align.RIGHT),
        )

        Sparkline.draw(s, RectF(b.left, b.top + headH + 10f, b.right - 4f, b.bottom - 14f), s.data.batteryHistory, color)
    }
}
