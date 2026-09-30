package com.malickabdullah.tessera.designs.note

import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.floor

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

    override fun draw(s: Scene) {
        val b = s.box
        s.dotGrid(RectF(0f, 0f, s.w, s.h), 6.5f, 0.85f, s.ink(0.07f, s.text))
        val message = s.data.content.note.trim().ifEmpty { "HELLO" }.uppercase()

        val footerH = 12f
        val area = RectF(b.left, b.top, b.right, b.bottom - footerH)
        val rows = floor(area.height() / (21f * s.k)).toInt().coerceIn(1, 7)
        val size = area.height() / rows / 1.12f * s.hero
        val p = s.paint(size, s.text)
        val lines = TextFit.wrap(message, area.width()) { p.measureText(it) }

        val framePx = area.width() * s.bitmapScale * area.height() * s.bitmapScale
        val maxPages = floor(FRAME_PIXEL_BUDGET / framePx).toInt().coerceIn(1, 6)
        val pages = TextFit.paginate(lines, rows, maxPages, area.width()) { p.measureText(it) }

        val frame = RectF(b.left, b.top, b.right, b.bottom)
        if (pages.size == 1) {
            drawPageOn(s, s.canvas, pages[0], 0, 1, area, rows, size, frame)
        } else {
            s.flipper(frame, PAGE_MS, pages.size) { i -> drawPageOn(s, this, pages[i], i, pages.size, area, rows, size, frame) }
        }
    }

    private fun drawPageOn(s: Scene, canvas: android.graphics.Canvas, lines: List<String>, index: Int, count: Int, area: RectF, rows: Int, size: Float, frame: RectF) {
        val p = s.paint(size, s.text)
        val lh = area.height() / rows
        lines.forEachIndexed { i, line -> canvas.drawText(line, area.left, area.top + lh * i + (lh + s.capHeight(p)) / 2f, p) }
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
