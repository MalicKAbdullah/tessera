package com.malickabdullah.tessera.designs.note

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign

object NoteQuote : WidgetDesign {
    override val id = "note.quote"
    override val category = Category.NOTE
    override val name = "Big Quote"
    override val blurb = "Large serif type with a hanging opening quotation mark and a ruled attribution."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("serif", 400, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF121212, radius = 30f, padding = 18f)
    override val signals = setOf(Signal.CONTENT)

    override fun draw(s: Scene) {
        val content = s.data.content
        val b = s.box
        val author = content.noteAuthor.trim()
        val small = s.w < 240f
        val hang = if (small) 20f else 30f
        val footer = if (author.isEmpty()) 0f else 26f * s.k
        val text = RectF(b.left + hang, b.top + 4f, b.right, b.bottom - footer)

        val body = content.note.trim().ifEmpty { "Say it plainly." }
        val fit = s.fitText(body, text.width() - 6f, text.height(), 1.12f, 12f, 44f)
        val p = s.paint(fit.size, s.text)
        val lh = fit.size * 1.12f
        val blockH = fit.lines.size * lh
        val y0 = text.top + (text.height() - blockH) / 2f + fit.size * 0.86f
        fit.lines.forEachIndexed { i, line -> s.canvas.drawText(line, text.left, y0 + i * lh, p) }
        // The opening mark hangs in the margin with its top level with the first line's capitals.
        val mark = s.paint(hang * 2.4f, s.accent)
        s.canvas.drawText("“", b.left - 1f, y0 - s.capHeight(p) + s.capHeight(mark), mark)
        val last = fit.lines.last()
        s.canvas.drawText("”", text.left + p.measureText(last) + 2f, y0 + (fit.lines.size - 1) * lh, s.paint(fit.size, s.accent))

        if (author.isNotEmpty()) {
            val y = b.bottom - 6f
            s.canvas.drawLine(text.left, y - 4f * s.k, text.left + 18f, y - 4f * s.k, s.stroke(s.accent, 1.6f))
            val name = s.tag(10.5f, s.ink(0.75f))
            s.canvas.drawText(TextFit.ellipsize(author.uppercase(), text.width() - 26f, { name.measureText(it) }), text.left + 26f, y, name)
        }
    }

    private fun Scene.tag(size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT): Paint =
        paint(size * k, color, font = "mono", weight = 500, align = align, tracking = 0.14f)
}
