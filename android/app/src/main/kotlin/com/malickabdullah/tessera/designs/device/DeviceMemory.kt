package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object DeviceMemory : WidgetDesign {
    override val id = "device.memory"
    override val category = Category.DEVICE
    override val name = "Memory"
    override val blurb = "RAM in use as a ticked ring, with what is free and the system's low-memory line."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of(
        "grotesk", 500, text = 0xFF15171A, accent = 0xFF3A7BFF, background = 0xFFF2F3F5,
        kind = BgKind.DOTS, background2 = 0xFFE3E6EA, radius = 28f, padding = 14f,
    )

    override fun liveKey(scene: SceneInputs) = DeviceKit.memoryKey(scene)

    override fun draw(s: Scene) {
        val m = s.data.memory
        val b = s.box
        val wide = s.w >= s.h * 1.4f
        val ringBox = if (wide) RectF(b.left, b.top, b.left + b.height(), b.bottom) else b
        val side = min(ringBox.width(), ringBox.height())
        val cx = ringBox.centerX()
        val cy = ringBox.centerY()
        val stroke = side * 0.085f
        val r = side / 2f - stroke
        val color = if (m.lowMemory) DeviceKit.WARNING else s.accent
        val arc = RectF(cx - r, cy - r, cx + r, cy + r)
        s.canvas.drawArc(arc, 0f, 360f, false, s.stroke(s.ink(0.08f), stroke))
        s.canvas.drawArc(arc, -90f, 360f * m.usedFraction, false, s.stroke(color, stroke))
        // The platform's low-memory threshold, as a notch on the ring.
        val th = 1f - m.thresholdBytes.toFloat() / m.totalBytes
        val a = Math.toRadians((-90f + 360f * th).toDouble())
        s.canvas.drawLine(
            cx + cos(a).toFloat() * (r - stroke * 0.8f), cy + sin(a).toFloat() * (r - stroke * 0.8f),
            cx + cos(a).toFloat() * (r + stroke * 0.8f), cy + sin(a).toFloat() * (r + stroke * 0.8f),
            s.stroke(s.ink(0.6f), 1.4f),
        )
        val pct = "${(m.usedFraction * 100).roundToInt()}"
        val size = s.fit("100", r * 1.1f, r * 0.7f) * s.hero
        val num = s.paint(size, s.text, align = Paint.Align.CENTER)
        s.textMid(pct, cx, cy - r * 0.08f, num)
        s.canvas.drawText("% RAM", cx, cy + r * 0.45f, DeviceKit.label(s, side / 170f * 9f, s.ink(0.55f), Paint.Align.CENTER))

        if (wide) {
            val x = ringBox.right + 18f
            val rows = listOf(
                "FREE" to DeviceKit.bytes(m.availableBytes),
                "IN USE" to DeviceKit.bytes(m.totalBytes - m.availableBytes),
                "TOTAL" to DeviceKit.bytes(m.totalBytes),
                "STATE" to if (m.lowMemory) "Low" else "Healthy",
            )
            val rowH = b.height() / rows.size
            rows.forEachIndexed { i, (k, v) ->
                val cyRow = b.top + rowH * i + rowH / 2f
                if (i > 0) s.canvas.drawLine(x, b.top + rowH * i, b.right, b.top + rowH * i, s.stroke(s.ink(0.1f), 1f))
                s.textMid(k, x, cyRow, DeviceKit.label(s, 9f))
                s.textMid(v, b.right, cyRow, s.paint(15f * s.k, if (i == 3 && m.lowMemory) DeviceKit.WARNING else s.text, align = Paint.Align.RIGHT))
            }
        }
    }
}
