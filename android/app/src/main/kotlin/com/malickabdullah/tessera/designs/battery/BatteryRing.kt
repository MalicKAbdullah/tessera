package com.malickabdullah.tessera.designs.battery

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object BatteryRing : WidgetDesign {
    override val id = "battery.ring"
    override val category = Category.BATTERY
    override val name = "Ring Gauge"
    override val blurb = "A 270° gauge with a tick scale, charging bolt and time to full or time left."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFF16171A, accent = 0xFF2F6BFF, background = 0xFFEEF0F3, radius = 30f, padding = 12f)
    override val signals = setOf(Signal.BATTERY)
    override val motion = "While charging, the tip of the gauge glows in a slow pulse."

    private const val START = 135f
    private const val SWEEP = 270f

    override fun liveKey(scene: SceneInputs) = BatteryKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val color = BatteryKit.levelColor(s)
        val stats = s.h >= 300f
        val ringBox = if (stats) RectF(b.left, b.top, b.right, b.bottom - 62f) else b
        val side = min(ringBox.width(), ringBox.height())
        val cx = ringBox.centerX()
        val cy = ringBox.centerY() + side * 0.04f
        val stroke = side * 0.075f
        val r = side / 2f - stroke * 1.9f
        val arc = RectF(cx - r, cy - r, cx + r, cy + r)

        s.canvas.drawArc(arc, START, SWEEP, false, s.stroke(s.ink(0.09f), stroke))
        val sweep = SWEEP * bat.level / 100f
        s.canvas.drawArc(arc, START, sweep, false, s.stroke(color, stroke))

        for (i in 0..50) {
            val a = Math.toRadians((START + SWEEP * i / 50f).toDouble())
            val major = i % 5 == 0
            val r0 = r + stroke * 0.95f
            val r1 = r0 + if (major) stroke * 0.8f else stroke * 0.4f
            s.canvas.drawLine(
                cx + cos(a).toFloat() * r0, cy + sin(a).toFloat() * r0,
                cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1,
                s.stroke(if (i * 2 <= bat.level) s.ink(0.55f) else s.ink(0.2f), if (major) 1.3f else 0.8f),
            )
        }

        val numSize = s.fit("100", r * 1.15f, r * 0.75f) * s.hero
        val num = s.paint(numSize, s.text, align = Paint.Align.CENTER)
        val pct = s.paint(numSize * 0.32f, color, align = Paint.Align.LEFT)
        val w = num.measureText("${bat.level}")
        s.textMid("${bat.level}", cx, cy - r * 0.05f, num)
        s.canvas.drawText("%", cx + w / 2f + 1.5f, cy - r * 0.05f - s.capHeight(num) / 2f + s.capHeight(pct), pct)

        val lineY = cy + r * 0.42f
        val caption = s.paint(max(8.5f, r * 0.13f) * s.k, s.ink(0.62f), font = "mono", weight = 500, align = Paint.Align.CENTER, tracking = 0.04f)
        if (bat.charging) {
            Kit.bolt(s.canvas, cx, cy - r * 0.58f, r * 0.26f, s.fill(color))
        }
        s.textMid(BatteryKit.estimate(s), cx, lineY, caption)
        // Below 7dp the status is unreadable; small rings leave it to the caption.
        if (r * 0.1f >= 7f) {
            s.textMid(BatteryKit.status(s).uppercase(), cx, cy + r * 0.86f, s.paint(r * 0.1f * s.k, s.ink(0.45f), font = "mono", weight = 500, align = Paint.Align.CENTER, tracking = 0.12f))
        }

        if (bat.charging) {
            val a = Math.toRadians((START + sweep).toDouble())
            val tx = cx + cos(a).toFloat() * r
            val ty = cy + sin(a).toFloat() * r
            val glow = RectF(tx - stroke * 1.6f, ty - stroke * 1.6f, tx + stroke * 1.6f, ty + stroke * 1.6f)
            s.flipper(glow, 600, 3) { i ->
                drawCircle(tx, ty, stroke * (0.9f + 0.3f * i), s.fill(s.ink(0.35f - 0.1f * i, color)))
                drawCircle(tx, ty, stroke * 0.32f, s.fill(s.ink(0.9f, s.style.bg.color)))
            }
        }
        if (stats) drawStats(s, RectF(b.left, b.bottom - 48f, b.right, b.bottom))
    }

    private fun drawStats(s: Scene, r: RectF) {
        val bat = s.data.battery
        val items = listOf(
            "TEMP" to "%.1f°".format(bat.temperatureC),
            "HEALTH" to bat.health,
            "VOLTAGE" to "%.2f V".format(bat.voltageMv / 1000f),
        )
        val colW = r.width() / items.size
        s.canvas.drawLine(r.left, r.top, r.right, r.top, s.stroke(s.ink(0.1f), 1f))
        items.forEachIndexed { i, (label, value) ->
            val x = r.left + colW * i + colW / 2f
            s.canvas.drawText(label, x, r.top + 16f, s.paint(8.5f * s.k, s.ink(0.5f), font = "mono", weight = 500, align = Paint.Align.CENTER, tracking = 0.14f))
            s.canvas.drawText(value, x, r.top + 38f, s.paint(16f * s.k, s.text, align = Paint.Align.CENTER))
        }
    }
}
