package com.malickabdullah.tessera.designs.battery

import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.data.BatterySample
import com.malickabdullah.tessera.engine.Scene

/** 24-hour battery level chart shared by battery designs. */
internal object Sparkline {
    private const val WINDOW = 24 * 60f

    fun draw(s: Scene, r: RectF, samples: List<BatterySample>, color: Int, labels: Boolean = true) {
        val nowMinute = s.now.toEpochSecond() / 60
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = s.ink(0.1f)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            pathEffect = DashPathEffect(floatArrayOf(2f, 3f), 0f)
        }
        for (level in listOf(0, 50, 100)) {
            val y = r.bottom - r.height() * level / 100f
            s.canvas.drawLine(r.left, y, r.right, y, grid)
        }
        fun x(minute: Long) = r.right - r.width() * ((nowMinute - minute) / WINDOW).coerceIn(0f, 1f)
        fun y(level: Int) = r.bottom - r.height() * level / 100f

        val points = samples.filter { nowMinute - it.epochMinute <= WINDOW } +
            BatterySample(nowMinute, s.data.battery.level, s.data.battery.charging)
        if (points.size < 3) {
            val dashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = s.ink(0.6f, color)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
            val yy = y(s.data.battery.level)
            s.canvas.drawLine(r.left, yy, r.right, yy, dashed)
            s.canvas.drawText("Recording — the day fills in as you go", r.left, yy - 6f, s.paint(9f * s.k, s.ink(0.5f), weight = 400))
        } else {
            points.zipWithNext().forEach { (a, b) ->
                if (a.charging) s.canvas.drawRect(x(a.epochMinute), r.top, x(b.epochMinute), r.bottom, s.fill(s.ink(0.07f, color)))
            }
            val line = Path()
            points.forEachIndexed { i, p -> if (i == 0) line.moveTo(x(p.epochMinute), y(p.level)) else line.lineTo(x(p.epochMinute), y(p.level)) }
            val area = Path(line).apply {
                lineTo(x(points.last().epochMinute), r.bottom)
                lineTo(x(points.first().epochMinute), r.bottom)
                close()
            }
            s.canvas.drawPath(area, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, r.top, 0f, r.bottom, s.ink(0.35f, color), s.ink(0f, color), Shader.TileMode.CLAMP)
            })
            s.canvas.drawPath(line, s.stroke(color, 1.6f).apply { strokeJoin = Paint.Join.ROUND })
        }
        s.canvas.drawCircle(r.right, y(s.data.battery.level), 3f, s.fill(color))
        if (labels) {
            val p = s.paint(8f * s.k, s.ink(0.45f), font = "mono", weight = 500, tracking = 0.1f)
            s.canvas.drawText("−24H", r.left, r.bottom + 11f, p)
            s.canvas.drawText("−12H", r.centerX(), r.bottom + 11f, p.apply { textAlign = Paint.Align.CENTER })
            s.canvas.drawText("NOW", r.right, r.bottom + 11f, Paint(p).apply { textAlign = Paint.Align.RIGHT })
        }
    }
}
