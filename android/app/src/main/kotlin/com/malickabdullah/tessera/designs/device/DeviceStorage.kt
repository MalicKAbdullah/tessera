package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.min
import kotlin.math.roundToInt

object DeviceStorage : WidgetDesign {
    override val id = "device.storage"
    override val category = Category.DEVICE
    override val name = "Storage"
    override val blurb = "Free space on the phone, as a dot field on 2×2 and a segmented bar on 4×2."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("dot", 700, text = 0xFFECEDEE, accent = 0xFFE8402F, background = 0xFF121212, radius = 28f, padding = 16f)

    override fun liveKey(scene: SceneInputs) = DeviceKit.storageKey(scene)

    override fun draw(s: Scene) {
        val st = s.data.storage
        val b = s.box
        val color = if (st.usedFraction > 0.9f) DeviceKit.WARNING else s.accent
        val (num, unit) = DeviceKit.bytesParts(st.freeBytes)
        s.canvas.drawText("STORAGE", b.left, b.top + 9f, DeviceKit.label(s, 9.5f, color))
        s.canvas.drawText(
            "${(st.usedFraction * 100).roundToInt()}% USED",
            b.right,
            b.top + 9f,
            DeviceKit.label(s, 9.5f, align = Paint.Align.RIGHT),
        )
        if (s.w >= s.h * 1.4f) {
            val heroH = b.height() * 0.42f
            val size = s.fit("888.8", b.width() * 0.5f, heroH) * s.hero
            val hero = s.paint(size, s.text)
            val baseline = b.top + 18f + size * 0.78f
            s.canvas.drawText(num, b.left - 1f, baseline, hero)
            val unitX = b.left + hero.measureText(num) + 6f
            s.canvas.drawText(unit, unitX, baseline, s.paint(size * 0.32f, s.ink(0.6f), font = "mono", weight = 500))
            s.canvas.drawText("FREE", unitX, baseline - size * 0.4f, DeviceKit.label(s, 9f))
            s.canvas.drawText(
                "of ${DeviceKit.bytes(st.totalBytes)}",
                b.right,
                baseline,
                s.paint(12f * s.k, s.ink(0.6f), font = "mono", weight = 400, align = Paint.Align.RIGHT),
            )
            DeviceKit.segments(s, RectF(b.left, b.bottom - 30f, b.right, b.bottom - 14f), st.usedFraction, 24, color)
            val l = DeviceKit.label(s, 8.5f)
            s.canvas.drawText("${DeviceKit.bytes(st.usedBytes)} USED", b.left, b.bottom, l)
            s.canvas.drawText("${DeviceKit.bytes(st.freeBytes)} FREE", b.right, b.bottom, DeviceKit.label(s, 8.5f, align = Paint.Align.RIGHT))
        } else {
            // 10 × 10 dots, one per percent, filling from the bottom row up.
            val field = RectF(b.left, b.top + 20f, b.right, b.bottom - 26f * s.k)
            val pitch = min(field.width(), field.height()) / 10f
            val ox = field.centerX() - pitch * 4.5f
            val oy = field.top + (field.height() - pitch * 10f) / 2f + pitch / 2f
            val used = (st.usedFraction * 100).roundToInt()
            for (i in 0 until 100) {
                val x = ox + (i % 10) * pitch
                val y = oy + (i / 10) * pitch
                s.canvas.drawCircle(x, y, pitch * 0.33f, s.fill(if ((9 - i / 10) * 10 + i % 10 < used) color else s.ink(0.12f)))
            }
            s.canvas.drawText("$num $unit", b.left, b.bottom, s.paint(18f * s.k, s.text, font = "mono", weight = 500))
            s.canvas.drawText("FREE", b.right, b.bottom, DeviceKit.label(s, 9f, align = Paint.Align.RIGHT))
        }
    }
}
