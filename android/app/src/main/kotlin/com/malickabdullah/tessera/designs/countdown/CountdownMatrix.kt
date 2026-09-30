package com.malickabdullah.tessera.designs.countdown

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.DotText
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.min

object CountdownMatrix : WidgetDesign {
    override val id = "countdown.matrix"
    override val category = Category.COUNTDOWN
    override val name = "Dot Matrix"
    override val blurb = "Days and hours to go in LED dot-matrix digits over a lattice of unlit dots."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("dot", 700, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF0C0C0D, radius = 30f)
    override val toggles = listOf(countModeToggle)
    override val signals = setOf(Signal.CONTENT)
    override val motion = "On 4×2, the two-dot separator between days and hours blinks once a second."

    override fun liveKey(scene: SceneInputs) = "${scene.now.toLocalDate()}|${scene.now.hour}"

    override fun draw(s: Scene) {
        val v = s.countdown()
        val b = s.box
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.07f))

        val head = s.tag(10.5f, s.ink(0.72f))
        val title = TextFit.ellipsize(v.title.uppercase(), b.width() - 12f, head.measurer())
        s.canvas.drawText(title, b.left, b.top + 10f * s.k, head)
        s.canvas.drawCircle(b.right - 3.2f, b.top + 6f * s.k, 3.2f, s.fill(s.accent))

        val footer = if (s.h >= 120f) 14f else 0f
        val area = RectF(b.left, b.top + 18f * s.k, b.right, b.bottom - footer - if (footer > 0f) 4f else 0f)
        val split = if (v.countUp) null else CountdownMath.remaining(s.now, v.target)
        if (split == null) {
            val text = if (v.days == 0L) "TODAY" else v.reading.number.toString()
            val line = RectF(area.left, area.top, area.right, area.bottom - if (v.days == 0L) 0f else area.height() * 0.22f)
            readout(s, line, text, if (v.days == 0L) "" else "D", s.text)
            if (v.days != 0L) {
                val cap = s.tag(11f, s.accent)
                s.canvas.drawText(v.reading.caption.uppercase(), area.left, area.bottom - 2f, cap)
            }
        } else if (s.w >= s.h * 1.6f) {
            val half = (area.width() - 18f) / 2f
            val days = readout(s, RectF(area.left, area.top, area.left + half, area.bottom), split.days.toString(), "D", s.text)
            readout(s, RectF(area.right - half, area.top, area.right, area.bottom), "%02d".format(split.hours), "H", s.accent)
            // The separator is a colon on the digits' dot grid (rows 2 and 4), midway between the readouts.
            val sx = (days.right + area.right - half) / 2f
            val pitch = DotText.pitch(days.paint)
            val ys = listOf(2, 4).map { DotText.rowY(days.baseline, pitch, it) }
            val dot = DotText.dotRadius(pitch)
            val sep = RectF(sx - dot - 1f, ys[0] - dot - 1f, sx + dot + 1f, ys[1] + dot + 1f)
            s.flipper(sep, 1000, 2) { i ->
                if (i == 0) ys.forEach { drawCircle(sx, it, dot, s.fill(s.accent)) }
            }
        } else {
            val rowH = area.height() * 0.62f
            readout(s, RectF(area.left, area.top, area.right, area.top + rowH), split.days.toString(), "D", s.text)
            readout(s, RectF(area.left, area.top + rowH + 6f, area.right, area.bottom), "%02d".format(split.hours), "H", s.accent)
        }
        if (footer > 0f) {
            val foot = s.tag(9.5f, s.ink(0.5f))
            val until = "${v.target.weekday()} ${v.target.short().uppercase()}"
            s.canvas.drawText(until, b.left, b.bottom - 2f, foot)
            val pct = s.tag(9.5f, s.ink(0.5f), Paint.Align.RIGHT)
            s.canvas.drawText("${(v.progress * 100).toInt()}%", b.right, b.bottom - 2f, pct)
        }
    }

    /** A drawn readout: the digits' paint, their baseline and where the readout ends. */
    private class Readout(val paint: Paint, val baseline: Float, val right: Float)

    /** Digits with a small unit letter on the same baseline, sized to fill [r]. */
    private fun readout(s: Scene, r: RectF, digits: String, unit: String, color: Int): Readout {
        val u = 0.3f
        val gap = 0.08f
        val pd = s.paint(100f, color)
        val pu = s.paint(100f * u, s.ink(0.6f, color))
        val widthAt100 = pd.measureText(digits) + if (unit.isEmpty()) 0f else 100f * gap + pu.measureText(unit)
        val fm = pd.fontMetrics
        val size = min(r.width() / widthAt100 * 100f, r.height() / (fm.descent - fm.ascent) * 100f) * s.hero
        val d = s.paint(size, color)
        val baseline = r.centerY() + s.capHeight(d) / 2f
        s.canvas.drawText(digits, r.left, baseline, d)
        val unitPaint = s.paint(size * u, s.ink(0.6f, color))
        val unitX = r.left + d.measureText(digits) + size * gap
        if (unit.isNotEmpty()) s.canvas.drawText(unit, unitX, baseline, unitPaint)
        return Readout(d, baseline, if (unit.isEmpty()) r.left + d.measureText(digits) else unitX + unitPaint.measureText(unit))
    }
}
