package com.malickabdullah.tessera.designs.battery

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.roundToInt

object BatteryCell : WidgetDesign {
    override val id = "battery.cell"
    override val category = Category.BATTERY
    override val name = "Dot Cell"
    override val blurb = "A battery drawn in dots that light column by column, with a dot-matrix percentage."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE)
    override val defaults = Style.of("dot", 700, text = 0xFFF0F0F0, accent = 0xFFC6F432, background = 0xFF0D0D0D, radius = 30f)
    override val signals = setOf(Signal.BATTERY)
    override val motion = "While charging, the next columns of cells fill in a loop."

    private const val PITCH = 6f

    override fun liveKey(scene: SceneInputs) = BatteryKit.key(scene)

    override fun draw(s: Scene) {
        val b = s.box
        val bat = s.data.battery
        val color = BatteryKit.levelColor(s)
        val status = s.paint(9.5f * s.k, s.ink(0.65f), font = "mono", weight = 500, tracking = 0.12f)
        s.canvas.drawText(BatteryKit.status(s).uppercase(), b.left, b.top + 9f, status)

        val wide = s.w >= s.h * 1.4f
        val cell: RectF
        val number: RectF
        if (wide) {
            cell = RectF(b.left, b.top + 22f, b.left + b.width() * 0.56f, b.bottom - 4f)
            number = RectF(cell.right + 12f, b.top + 16f, b.right, b.bottom - 14f)
        } else {
            number = RectF(b.left, b.top + 14f, b.right, b.top + 14f + b.height() * 0.42f)
            cell = RectF(b.left, number.bottom + 8f, b.right - 6f, b.bottom)
        }
        val inner = drawShell(s, cell)
        val cols = (inner.width() / PITCH).toInt()
        val rows = (inner.height() / PITCH).toInt()
        val litCols = (bat.level / 100f * cols).roundToInt()
        drawCells(s, inner, cols, rows) { c -> if (c < litCols) color else s.ink(0.1f) }
        if (bat.charging && litCols < cols) {
            s.flipper(inner, 450, 3) { frame ->
                val scene = s
                val ox = inner.left + (inner.width() - (cols - 1) * PITCH) / 2f
                val oy = inner.top + (inner.height() - (rows - 1) * PITCH) / 2f
                for (c in litCols until minOf(cols, litCols + frame + 1)) {
                    for (r in 0 until rows) drawCircle(ox + c * PITCH, oy + r * PITCH, 1.9f, scene.fill(scene.ink(0.9f - (c - litCols) * 0.25f, color)))
                }
            }
        }

        val pct = "${bat.level}"
        val size = s.fit("100", number.width() * 0.82f, number.height()) * s.hero
        val big = s.paint(size, s.text)
        val fm = big.fontMetrics
        val baseline = number.centerY() - (fm.ascent + fm.descent) / 2f
        s.canvas.drawText(pct, number.left, baseline, big)
        s.canvas.drawText("%", number.left + big.measureText(pct) + 2f, baseline, s.paint(size * 0.36f, color))
        if (wide) {
            val est = s.paint(9.5f * s.k, s.ink(0.5f), font = "mono", weight = 400, tracking = 0.08f)
            s.canvas.drawText(BatteryKit.estimate(s).uppercase(), number.left, b.bottom - 2f, est)
        }
    }

    /** Dotted outline and terminal; returns the rect available for cells. */
    private fun drawShell(s: Scene, r: RectF): RectF {
        val nub = 5f
        val body = RectF(r.left, r.top, r.right - nub - 2f, r.bottom)
        val dots = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.ink(0.55f)
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            strokeCap = Paint.Cap.ROUND
            pathEffect = DashPathEffect(floatArrayOf(0f, 4.5f), 0f)
        }
        s.canvas.drawPath(Path().apply { addRoundRect(body, 7f, 7f, Path.Direction.CW) }, dots)
        val nubRect = RectF(body.right + 2f, body.centerY() - body.height() * 0.18f, r.right, body.centerY() + body.height() * 0.18f)
        s.canvas.drawRoundRect(nubRect, 2f, 2f, s.fill(s.ink(0.55f)))
        return RectF(body.left + 6f, body.top + 6f, body.right - 6f, body.bottom - 6f)
    }

    private fun drawCells(s: Scene, inner: RectF, cols: Int, rows: Int, color: (Int) -> Int) {
        val ox = inner.left + (inner.width() - (cols - 1) * PITCH) / 2f
        val oy = inner.top + (inner.height() - (rows - 1) * PITCH) / 2f
        for (c in 0 until cols) {
            val p = s.fill(color(c))
            for (r in 0 until rows) s.canvas.drawCircle(ox + c * PITCH, oy + r * PITCH, 1.9f, p)
        }
    }
}
