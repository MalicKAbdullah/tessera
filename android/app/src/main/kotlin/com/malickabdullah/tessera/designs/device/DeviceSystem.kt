package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.PlugType
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign

object DeviceSystem : WidgetDesign {
    override val id = "device.system"
    override val category = Category.DEVICE
    override val name = "System"
    override val blurb = "Battery, storage and memory as three segmented gauges; uptime and network on 4×4."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFFE9EAEC, accent = 0xFFD4FF3A, background = 0xFF141517, radius = 26f, padding = 16f)
    override val signals = setOf(Signal.BATTERY)
    override val motion = "While charging, the battery gauge's next segment breathes."

    override fun liveKey(scene: SceneInputs): String {
        val b = scene.data.battery
        val n = scene.data.network
        return "${b.level}|${b.charging}|${DeviceKit.storageKey(scene)}|${DeviceKit.memoryKey(scene)}|" +
            "${DeviceKit.uptime(scene.data.uptimeMs)}|${n.transport}|${n.validated}"
    }

    private class Gauge(val label: String, val value: String, val detail: String, val fraction: Float, val color: Int)

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val st = s.data.storage
        val m = s.data.memory
        val large = s.h >= 300f
        s.canvas.drawText("SYSTEM", b.left, b.top + 9f, DeviceKit.label(s, 9.5f, s.accent))
        s.canvas.drawText(
            "UP ${DeviceKit.uptime(s.data.uptimeMs).uppercase()}",
            b.right,
            b.top + 9f,
            DeviceKit.label(s, 9f, align = Paint.Align.RIGHT),
        )
        val lowBattery = bat.level <= 20 && bat.plug == PlugType.NONE
        val gauges = listOf(
            Gauge(
                "BATTERY",
                "${bat.level}%",
                when {
                    bat.charging -> bat.chargeTimeRemainingMs?.let { "Full in ${Kit.duration(it)}" } ?: "Charging"
                    else -> "%.1f°C".format(bat.temperatureC)
                },
                bat.level / 100f,
                if (lowBattery) DeviceKit.WARNING else s.accent,
            ),
            Gauge("STORAGE", "${(st.usedFraction * 100).toInt()}%", "${DeviceKit.bytes(st.freeBytes)} free", st.usedFraction, if (st.usedFraction > 0.9f) DeviceKit.WARNING else s.text),
            Gauge("MEMORY", "${(m.usedFraction * 100).toInt()}%", "${DeviceKit.bytes(m.availableBytes)} free", m.usedFraction, if (m.lowMemory) DeviceKit.WARNING else s.text),
        )
        val area = RectF(b.left, b.top + 22f, b.right, if (large) b.top + b.height() * 0.62f else b.bottom)
        val rowH = area.height() / gauges.size
        val segments = if (s.w >= 300f) 20 else 12
        gauges.forEachIndexed { i, g ->
            val top = area.top + rowH * i
            val labelY = top + rowH * 0.36f
            s.canvas.drawText(g.label, b.left, labelY, DeviceKit.label(s, 8.5f))
            s.canvas.drawText(g.detail, b.right, labelY, s.paint(10f * s.k, s.ink(0.6f), weight = 400, align = Paint.Align.RIGHT))
            val valueW = 44f * s.k
            val barH = minOf(rowH * 0.3f, 14f)
            val bar = RectF(b.left, top + rowH * 0.5f, b.right - valueW, top + rowH * 0.5f + barH)
            DeviceKit.segments(s, bar, g.fraction, segments, g.color, 2.5f)
            s.textMid(g.value, b.right, bar.centerY(), s.paint(14f * s.k, s.text, align = Paint.Align.RIGHT))
            if (i == 0 && bat.charging) {
                val w = (bar.width() - 2.5f * (segments - 1)) / segments
                val next = (g.fraction * segments).toInt().coerceAtMost(segments - 1)
                val seg = RectF(bar.left + next * (w + 2.5f), bar.top, bar.left + next * (w + 2.5f) + w, bar.bottom)
                s.flipper(seg, 500, 3) { f -> drawRoundRect(seg, 3f, 3f, s.fill(s.ink(0.2f + 0.3f * f, g.color))) }
            }
        }
        if (large) details(s, RectF(b.left, area.bottom + 14f, b.right, b.bottom))
    }

    private fun details(s: Scene, r: RectF) {
        val n = s.data.network
        val st = s.data.storage
        val m = s.data.memory
        val bat = s.data.battery
        val cells = listOf(
            "NETWORK" to n.transport.label,
            "INTERNET" to if (n.validated) "Online" else "None",
            "DISK" to DeviceKit.bytes(st.totalBytes),
            "RAM" to DeviceKit.bytes(m.totalBytes),
            "HEALTH" to bat.health,
            "VOLTAGE" to "%.2f V".format(bat.voltageMv / 1000f),
        )
        s.canvas.drawLine(r.left, r.top, r.right, r.top, s.stroke(s.ink(0.1f), 1f))
        val cols = 3
        val colW = r.width() / cols
        val rowH = (r.height() - 8f) / 2f
        cells.forEachIndexed { i, (k, v) ->
            val x = r.left + colW * (i % cols)
            val y = r.top + 8f + rowH * (i / cols)
            s.canvas.drawText(k, x, y + rowH * 0.4f, DeviceKit.label(s, 8f))
            s.canvas.drawText(v, x, y + rowH * 0.85f, s.paint(14f * s.k, s.text, font = "sans", weight = 500))
        }
    }
}
