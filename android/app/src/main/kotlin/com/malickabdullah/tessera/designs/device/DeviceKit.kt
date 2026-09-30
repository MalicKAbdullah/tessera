package com.malickabdullah.tessera.designs.device

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs

internal object DeviceKit {
    const val WARNING = 0xFFFF5A4F.toInt()

    /** Decimal units, as Settings reports storage since Android 8. */
    fun bytes(b: Long): String {
        val gb = b / 1e9
        return when {
            gb >= 100 -> "%.0f GB".format(gb)
            gb >= 1 -> "%.1f GB".format(gb)
            else -> "%.0f MB".format(b / 1e6)
        }
    }

    /** Number and unit separately, for hero text with a smaller unit. */
    fun bytesParts(b: Long): Pair<String, String> = bytes(b).split(' ').let { it[0] to it[1] }

    fun uptime(ms: Long): String {
        val minutes = ms / 60_000
        val days = minutes / 1440
        val hours = minutes % 1440 / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${"%02d".format(minutes % 60)}m"
            else -> "${minutes}m"
        }
    }

    fun storageKey(s: SceneInputs) = "${s.data.storage.freeBytes / 200_000_000}"

    fun memoryKey(s: SceneInputs) = "${(s.data.memory.usedFraction * 50).toInt()}|${s.data.memory.lowMemory}"

    fun label(s: Scene, size: Float = 9.5f, color: Int = s.ink(0.55f), align: Paint.Align = Paint.Align.LEFT) =
        s.paint(size * s.k, color, font = "mono", weight = 500, align = align, tracking = 0.14f)

    /** A row of rounded segments filled to [fraction]; the partial segment is filled proportionally. */
    fun segments(s: Scene, r: RectF, fraction: Float, count: Int, color: Int, gap: Float = 3f) {
        val w = (r.width() - gap * (count - 1)) / count
        val filled = fraction * count
        val rad = minOf(3f, r.height() / 2f)
        for (i in 0 until count) {
            val seg = RectF(r.left + i * (w + gap), r.top, r.left + i * (w + gap) + w, r.bottom)
            s.canvas.drawRoundRect(seg, rad, rad, s.fill(s.ink(0.09f)))
            val part = (filled - i).coerceIn(0f, 1f)
            if (part > 0f) s.canvas.drawRoundRect(RectF(seg.left, seg.top, seg.left + w * part, seg.bottom), rad, rad, s.fill(color))
        }
    }
}
