package com.malickabdullah.tessera.designs.note

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.malickabdullah.tessera.designs.classic.caption
import com.malickabdullah.tessera.designs.classic.classicStyle
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.WidgetDesign

object NoteClassic : WidgetDesign {
    override val id = "note.classic"
    override val category = Category.NOTE
    override val name = "Classic Note"
    override val blurb = "A line worth keeping in view, with optional attribution."
    override val sizes = listOf(SizeClass.WIDE)
    override val defaults = classicStyle
    override val signals = setOf(Signal.CONTENT)

    override fun draw(s: Scene) {
        val content = s.data.content
        val b = s.box
        val author = content.noteAuthor
        val bottom = if (author.isEmpty()) b.bottom else b.bottom - 20f * s.k
        val note = content.note.ifBlank { "Write something." }
        val paint = TextPaint(s.paint(20f * s.k, if (content.note.isBlank()) s.ink(0.45f) else s.text))
        val layout = StaticLayout.Builder.obtain(note, 0, note.length, paint, b.width().toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .setMaxLines(((bottom - b.top) / (paint.fontSpacing * 1.15f)).toInt().coerceAtLeast(1))
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        s.canvas.save()
        s.canvas.translate(b.left, b.top + (bottom - b.top - layout.height) / 2f)
        layout.draw(s.canvas)
        s.canvas.restore()
        if (author.isNotEmpty()) s.caption("— $author")
    }
}
