package com.malickabdullah.tessera.designs.note

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.format.DateTimeFormatter
import java.util.Locale

object NoteDaily : WidgetDesign {
    override val id = "note.daily"
    override val category = Category.NOTE
    override val name = "Daily Quote"
    override val blurb = "A new public-domain quotation every day, with author and source."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("serif", 400, text = 0xFFEFE9DA, accent = 0xFFD6B26A, background = 0xFF14231D, radius = 30f, padding = 18f)
    override val motion = null

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val today = s.now.toLocalDate()
        val quote = Quotes.of(today)
        val b = s.box
        val tag = { size: Float, color: Int, align: Paint.Align -> s.paint(size * s.k, color, font = "mono", weight = 500, align = align, tracking = 0.14f) }

        val date = today.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())).uppercase()
        s.canvas.drawText(if (s.w < 240f) date else "QUOTE OF THE DAY", b.left, b.top + 9f * s.k, tag(9.5f, s.accent, Paint.Align.LEFT))
        s.canvas.drawText(if (s.w < 240f) "" else date, b.right, b.top + 9f * s.k, tag(9.5f, s.ink(0.5f), Paint.Align.RIGHT))
        s.canvas.drawLine(b.left, b.top + 16f * s.k, b.right, b.top + 16f * s.k, s.stroke(s.ink(0.14f), 1f))

        val showSource = s.h >= 150f
        val footer = (if (showSource) 34f else 18f) * s.k
        val text = RectF(b.left, b.top + 24f * s.k, b.right, b.bottom - footer)
        val fit = s.fitText(quote.text, text.width(), text.height(), 1.14f, 11f, 40f)
        val p = s.paint(fit.size, s.text)
        val lh = fit.size * 1.14f
        val y0 = text.top + (text.height() - fit.lines.size * lh) / 2f + fit.size * 0.84f
        fit.lines.forEachIndexed { i, line -> s.canvas.drawText(line, text.left, y0 + i * lh, p) }

        val author = s.paint(12f * s.k, s.accent, font = "grotesk", weight = 500, tracking = 0.02f)
        val measure = { str: String -> author.measureText(str) }
        s.canvas.drawText(TextFit.ellipsize(quote.author, b.width(), measure), b.left, b.bottom - if (showSource) 14f * s.k else 2f, author)
        if (showSource) {
            val src = s.paint(10.5f * s.k, s.ink(0.5f), font = "grotesk", weight = 300)
            s.canvas.drawText(TextFit.ellipsize(quote.source, b.width(), { src.measureText(it) }), b.left, b.bottom, src)
        }
    }
}
