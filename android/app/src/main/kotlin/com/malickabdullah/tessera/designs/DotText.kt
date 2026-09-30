package com.malickabdullah.tessera.designs

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

/**
 * Text in the Doto dot-matrix face with its punctuation redrawn. Doto's full
 * stop and colon are crosses of dots that read as "+" and "‡", so here they
 * are round dots on the glyphs' 5×7 grid: the cap height spans the seven
 * rows and a column is the same pitch. Any other face draws unchanged.
 */
object DotText {
    const val FACE = "dot"
    private const val ROWS = 7

    /**
     * A redrawn mark: the rows (0 = top) lit in one column, and its advance in
     * grid pitches, or null to keep the face's monospaced cell so clock-style
     * layouts line up with the digits.
     */
    private class Mark(val rows: IntArray, val pitches: Float?)

    private val marks = mapOf(
        '.' to Mark(intArrayOf(6), 2f),
        ':' to Mark(intArrayOf(2, 4), null),
        '·' to Mark(intArrayOf(3), 2f),
    )

    /** Grid pitch of [p]'s dot-matrix glyphs. */
    fun pitch(p: Paint): Float {
        val r = Rect()
        p.getTextBounds("H", 0, 1, r)
        return r.height() / ROWS.toFloat()
    }

    /** Radius of a round dot with the visual weight of the face's square dots. */
    fun dotRadius(pitch: Float) = pitch * 0.4f

    /** Centre y of grid [row] for text sitting on [baseline]. */
    fun rowY(baseline: Float, pitch: Float, row: Int) = baseline - pitch * (ROWS - row - 0.5f)

    /** Width of [text] as [draw] lays it out. */
    fun measure(text: String, p: Paint, font: String): Float {
        if (font != FACE) return p.measureText(text)
        val pitch = pitch(p)
        var width = 0f
        segments(text) { run, mark -> width += advance(run, mark, p, pitch) }
        return width
    }

    /** Draws [text] at [x] (honouring [p]'s alignment) on [baseline]; [font] is the face [p] was made with. */
    fun draw(canvas: Canvas, text: String, x: Float, baseline: Float, p: Paint, font: String) {
        if (font != FACE || text.none { it in marks }) {
            canvas.drawText(text, x, baseline, p)
            return
        }
        val run = Paint(p).apply { textAlign = Paint.Align.LEFT }
        var cursor = when (p.textAlign) {
            Paint.Align.CENTER -> x - measure(text, run, font) / 2f
            Paint.Align.RIGHT -> x - measure(text, run, font)
            else -> x
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.color }
        val pitch = pitch(run)
        // A face-width mark sits in a digit's centre column, which Doto does not centre in its advance.
        val column = Rect().also { run.getTextBounds("8", 0, 1, it) }.exactCenterX()
        segments(text) { s, mark ->
            if (mark == null) {
                canvas.drawText(s, cursor, baseline, run)
            } else {
                val cx = cursor + if (mark.pitches == null) column else pitch * 0.5f
                mark.rows.forEach { canvas.drawCircle(cx, rowY(baseline, pitch, it), dotRadius(pitch), dot) }
            }
            cursor += advance(s, mark, run, pitch)
        }
    }

    private fun advance(s: String, mark: Mark?, p: Paint, pitch: Float): Float =
        if (mark?.pitches != null) mark.pitches * pitch else p.measureText(s)

    /** Splits [text] into plain runs (mark null) and single redrawn marks, in order. */
    private fun segments(text: String, each: (String, Mark?) -> Unit) {
        var start = 0
        text.forEachIndexed { i, ch ->
            val mark = marks[ch] ?: return@forEachIndexed
            if (i > start) each(text.substring(start, i), null)
            each(ch.toString(), mark)
            start = i + 1
        }
        if (start < text.length) each(text.substring(start), null)
    }
}
