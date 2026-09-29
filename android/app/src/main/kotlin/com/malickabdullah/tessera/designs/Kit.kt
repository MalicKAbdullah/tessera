package com.malickabdullah.tessera.designs

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Scene
import kotlin.math.max

/** Small shared drawing vocabulary for designs. */
object Kit {
    /** Sample string of a numeric pattern at its widest, for sizing live TextClock text. */
    fun widest(s: Scene, pattern: String, font: String = s.style.font, weight: Int = s.style.weight): String {
        val p = s.paint(100f, font = font, weight = weight)
        val digit = ('0'..'9').maxBy { p.measureText(it.toString()) }
        return pattern.map { if (it.isLetter()) digit else it }.joinToString("")
    }

    fun bolt(canvas: Canvas, cx: Float, cy: Float, height: Float, paint: Paint) {
        val u = height / 12f
        val path = Path().apply {
            moveTo(cx + 1.2f * u, cy - 6f * u)
            lineTo(cx - 3.6f * u, cy + 1f * u)
            lineTo(cx - 0.2f * u, cy + 1f * u)
            lineTo(cx - 1.2f * u, cy + 6f * u)
            lineTo(cx + 3.6f * u, cy - 1f * u)
            lineTo(cx + 0.2f * u, cy - 1f * u)
            close()
        }
        canvas.drawPath(path, paint)
    }

    fun bell(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val u = size / 10f
        val body = Path().apply {
            moveTo(cx - 4f * u, cy + 2.5f * u)
            cubicTo(cx - 4f * u, cy - 5.5f * u, cx + 4f * u, cy - 5.5f * u, cx + 4f * u, cy + 2.5f * u)
            lineTo(cx + 5f * u, cy + 3.5f * u)
            lineTo(cx - 5f * u, cy + 3.5f * u)
            close()
        }
        canvas.drawPath(body, paint)
        canvas.drawCircle(cx, cy + 4.8f * u, 1.3f * u, paint)
    }

    fun sun(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        canvas.drawCircle(cx, cy, r * 0.5f, paint)
        val ray = Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1f, r * 0.16f)
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = kotlin.math.cos(a).toFloat()
            val sn = kotlin.math.sin(a).toFloat()
            canvas.drawLine(cx + c * r * 0.72f, cy + sn * r * 0.72f, cx + c * r, cy + sn * r, ray)
        }
    }

    fun moon(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        val outer = Path().apply { addCircle(cx, cy, r * 0.8f, Path.Direction.CW) }
        val bite = Path().apply { addCircle(cx + r * 0.42f, cy - r * 0.3f, r * 0.66f, Path.Direction.CW) }
        outer.op(bite, Path.Op.DIFFERENCE)
        canvas.drawPath(outer, paint)
    }

    /** "1h 05m" / "42m". */
    fun duration(ms: Long): String {
        val minutes = ms / 60_000
        return if (minutes >= 60) "${minutes / 60}h ${"%02d".format(minutes % 60)}m" else "${minutes}m"
    }

    fun roundRect(canvas: Canvas, rect: RectF, r: Float, paint: Paint) = canvas.drawRoundRect(rect, r, r, paint)
}
