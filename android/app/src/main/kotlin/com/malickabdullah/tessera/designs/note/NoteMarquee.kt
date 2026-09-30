package com.malickabdullah.tessera.designs.note

import android.graphics.RectF
import com.malickabdullah.tessera.designs.DotText
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object NoteMarquee : WidgetDesign {
    override val id = "note.marquee"
    override val category = Category.NOTE
    override val name = "Dot Matrix Message"
    override val blurb = "Your message in LED dot-matrix type, paged automatically when it is too long to show at once."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("dot", 700, text = 0xFFD4FF3A, accent = 0xFFF2F3F5, background = 0xFF0B0B0C, radius = 30f, padding = 16f)
    override val signals = setOf(Signal.CONTENT)
    override val motion = "Long messages flip to the next page every four seconds; short ones stay still."

    private const val PAGE_MS = 4000
    private const val FRAME_PIXEL_BUDGET = 700_000f
    private const val LINE = 1.12f

    override fun draw(s: Scene) {
        val b = s.box
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.07f, s.text))
        val message = s.data.content.note.trim().ifEmpty { "HELLO" }.uppercase()

        val footerH = 12f
        val area = RectF(b.left, b.top, b.right, b.bottom - footerH)
        // As large as the message allows without breaking a word; below one 21dp row it pages instead.
        val minSize = min(21f * s.k, area.height()) / LINE
        val measure = s.paint(100f, s.text)
        val longestWord = message.split(' ', '\n').maxOf { measure.measureText(it) }
        // Just under the exact fit, so rounding in the wrap never breaks the longest word.
        val maxSize = max(minSize, min(area.width() / longestWord * 99.5f, area.height() / LINE))
        val size = TextFit.fitSize(message, area.width(), area.height(), LINE, minSize, maxSize) { t, px ->
            measure.textSize = px
            measure.measureText(t)
        } * s.hero
        val p = s.paint(size, s.text)
        val rows = floor(area.height() / (size * LINE)).toInt().coerceAtLeast(1)
        val lines = TextFit.wrap(message, area.width()) { p.measureText(it) }

        val framePx = area.width() * s.bitmapScale * area.height() * s.bitmapScale
        val maxPages = floor(FRAME_PIXEL_BUDGET / framePx).toInt().coerceIn(1, 6)
        val pages = TextFit.paginate(lines, rows, maxPages, area.width()) { p.measureText(it) }

        val frame = RectF(b.left, b.top, b.right, b.bottom)
        if (pages.size == 1) {
            // A message that fits sits centred in the box rather than under empty rows.
            val block = pages[0].size * size * LINE
            val top = area.top + (area.height() - block) / 2f
            drawPageOn(s, s.canvas, pages[0], 0, 1, RectF(area.left, top, area.right, top + block), pages[0].size, size, frame)
        } else {
            s.flipper(frame, PAGE_MS, pages.size) { i -> drawPageOn(s, this, pages[i], i, pages.size, area, rows, size, frame) }
        }
    }

    private fun drawPageOn(s: Scene, canvas: android.graphics.Canvas, lines: List<String>, index: Int, count: Int, area: RectF, rows: Int, size: Float, frame: RectF) {
        val p = s.paint(size, s.text)
        val lh = area.height() / rows
        lines.forEachIndexed { i, line -> DotText.draw(canvas, line, area.left, area.top + lh * i + (lh + s.capHeight(p)) / 2f, p, s.style.font) }
        // Page indicator: one dot per page, the current one lit.
        val gap = 9f
        val cy = frame.bottom - 3f
        for (d in 0 until count) {
            val x = frame.right - (count - 1 - d) * gap - 2.5f
            canvas.drawCircle(x, cy, if (d == index) 2.4f else 1.6f, s.fill(if (d == index) s.accent else s.ink(0.3f)))
        }
        if (count == 1) canvas.drawCircle(frame.right - 2.5f, cy, 2f, s.fill(s.ink(0.35f)))
    }
}
