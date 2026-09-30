package com.malickabdullah.tessera.designs.note

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.abs
import kotlin.math.min

object NoteSticky : WidgetDesign {
    override val id = "note.sticky"
    override val category = Category.NOTE
    override val name = "Sticky Note"
    override val blurb = "Grainy paper, a strip of tape, ruled lines and slightly uneven handwriting-style serif."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of(
        "serif", 400, text = 0xFF1C2010, accent = 0xFF4B5A14, background = 0xFFE3F59A,
        kind = BgKind.GRAIN, radius = 10f, padding = 18f,
    )
    override val signals = setOf(Signal.CONTENT)

    private const val LEAN = -0.16f

    override fun draw(s: Scene) {
        val content = s.data.content
        val b = s.box
        val hasAuthor = content.noteAuthor.isNotBlank()
        val top = b.top + 8f
        val bottom = b.bottom - if (hasAuthor) 22f * s.k else 0f
        // The hand grows with the note so a short one fills a large sheet instead of sitting at the top.
        val fit = s.fitText(content.note.ifBlank { "Write something." }, b.width() - 4f, bottom - top, 1.22f, 12f, (s.minSide * 0.2f).coerceAtLeast(34f))
        val lh = fit.size * 1.22f

        // Ruled lines, one per text line and continuing to the bottom edge.
        val rule = s.stroke(s.ink(0.1f), 0.8f, round = false)
        var y = top + lh
        while (y < s.h - 6f) {
            s.canvas.drawLine(6f, y + 3f, s.w - 6f, y + 3f, rule)
            y += lh
        }

        val ink = s.paint(fit.size, s.text).apply { textSkewX = LEAN }
        fit.lines.forEachIndexed { i, line ->
            // A fixed per-line wobble so the hand looks uneven but never changes between renders.
            val wobble = ((line.hashCode() ushr 3) % 7 - 3) * 0.12f
            s.canvas.save()
            s.canvas.rotate(wobble * 0.4f, b.left, top + lh * (i + 1))
            s.canvas.drawText(line, b.left + abs(wobble), top + lh * (i + 1) + wobble, ink)
            s.canvas.restore()
        }
        if (hasAuthor) {
            val sign = s.paint(fit.size.coerceAtMost(15f * s.k), s.accent, align = Paint.Align.RIGHT).apply { textSkewX = LEAN }
            // The signature ends clear of the curled corner.
            val right = min(b.right, s.w - foldSize(s) - 4f)
            s.canvas.drawText(TextFit.ellipsize("— ${content.noteAuthor}", right - b.left, sign.measurer()), right, b.bottom - 2f, sign)
        }
        tape(s)
        fold(s)
    }

    private fun Paint.measurer(): (String) -> Float = { measureText(it) }

    private fun tape(s: Scene) {
        val w = s.w * 0.28f
        s.canvas.save()
        s.canvas.rotate(-2.5f, s.w / 2f, 0f)
        val r = RectF(s.w / 2f - w / 2f, -6f, s.w / 2f + w / 2f, 13f)
        s.canvas.drawRect(r, s.fill(0x66FFFFFF))
        s.canvas.drawRect(r, s.stroke(0x22000000, 0.8f, round = false))
        s.canvas.restore()
    }

    private fun foldSize(s: Scene) = (s.minSide * 0.13f).coerceIn(14f, 30f)

    /** Curled bottom-right corner: a lit triangle over a soft shadow. */
    private fun fold(s: Scene) {
        val f = foldSize(s)
        val shadow = Path().apply {
            moveTo(s.w - f, s.h)
            lineTo(s.w, s.h - f)
            lineTo(s.w, s.h)
            close()
        }
        val lift = Path().apply {
            moveTo(s.w - f, s.h)
            lineTo(s.w, s.h - f)
            lineTo(s.w - f, s.h - f)
            close()
        }
        val p = s.fill(0).apply {
            shader = LinearGradient(s.w - f, s.h - f, s.w, s.h, 0x33000000, 0x00000000, Shader.TileMode.CLAMP)
        }
        s.canvas.drawPath(shadow, p)
        s.canvas.drawPath(lift, s.fill(0x44FFFFFF))
        s.canvas.drawPath(lift, s.stroke(0x22000000, 0.8f, round = false))
    }
}
