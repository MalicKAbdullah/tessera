package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.format.DateTimeFormatter
import java.util.Locale

object DeviceUptime : WidgetDesign {
    override val id = "device.uptime"
    override val category = Category.DEVICE
    override val name = "Uptime"
    override val blurb = "How long since the phone last restarted, with the boot time and a 30-day strip."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("condensed", 500, text = 0xFFF2F3F5, accent = 0xFF9BE15D, background = 0xFF161816, radius = 28f, padding = 16f)

    private val since = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.getDefault())

    override fun liveKey(scene: SceneInputs) = DeviceKit.uptime(scene.data.uptimeMs)

    override fun draw(s: Scene) {
        val up = s.data.uptimeMs
        val b = s.box
        val boot = s.now.minusNanos(up * 1_000_000)
        s.canvas.drawText("UPTIME", b.left, b.top + 9f, DeviceKit.label(s, 9.5f, s.accent))
        val text = DeviceKit.uptime(up)
        val wide = s.w >= s.h * 1.4f
        val heroH = b.height() * if (wide) 0.45f else 0.38f
        val size = s.fit(text, b.width(), heroH) * s.hero
        val baseline = b.top + 20f + size * 0.8f
        s.canvas.drawText(text, b.left - 1f, baseline, s.paint(size, s.text))
        s.canvas.drawText(
            "SINCE ${boot.format(since).uppercase()}",
            b.left,
            baseline + 16f * s.k,
            DeviceKit.label(s, 8.5f),
        )
        // One cell per day for 30 days: days elapsed since boot lit, today's cell half-lit.
        val days = (up / 86_400_000L).toInt()
        val strip = RectF(b.left, b.bottom - 10f, b.right, b.bottom)
        val cells = if (wide) 30 else 15
        val gap = 2.5f
        val w = (strip.width() - gap * (cells - 1)) / cells
        for (i in 0 until cells) {
            val x = strip.left + i * (w + gap)
            val c = when {
                i < days -> s.accent
                i == days -> s.ink(0.45f, s.accent)
                else -> s.ink(0.1f)
            }
            s.canvas.drawRoundRect(RectF(x, strip.top, x + w, strip.bottom), 2f, 2f, s.fill(c))
        }
        if (days >= cells) {
            s.canvas.drawText("$days DAYS", b.right, strip.top - 6f, DeviceKit.label(s, 8.5f, s.ink(0.7f), Paint.Align.RIGHT))
        }
    }
}
