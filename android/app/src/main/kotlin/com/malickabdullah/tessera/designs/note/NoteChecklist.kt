package com.malickabdullah.tessera.designs.note

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.data.ChecklistItem
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign

object NoteChecklist : WidgetDesign {
    override val id = "note.checklist"
    override val category = Category.NOTE
    override val name = "Checklist"
    override val blurb = "Your to-do items with checkboxes and a progress bar; tick them off in the app."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFF1D1D1F, accent = 0xFF2F8F5B, background = 0xFFEEF0F3, radius = 28f, padding = 16f)
    override val signals = setOf(Signal.CONTENT)

    override fun draw(s: Scene) {
        val b = s.box
        val real = s.data.content.checklist
        val items = real.ifEmpty {
            listOf(ChecklistItem("Add items in the editor", false), ChecklistItem("Tick them off in the app", false), ChecklistItem("Progress shows here", false))
        }
        val done = items.count { it.done }

        val head = s.paint(10f * s.k, s.accent, font = "mono", weight = 500, tracking = 0.14f)
        s.canvas.drawText("TO DO", b.left, b.top + 9f * s.k, head)
        val count = s.paint(10f * s.k, s.ink(0.5f), font = "mono", weight = 500, align = Paint.Align.RIGHT, tracking = 0.1f)
        s.canvas.drawText(if (real.isEmpty()) "0 / 0" else "$done / ${items.size}", b.right, b.top + 9f * s.k, count)
        val bar = RectF(b.left, b.top + 16f * s.k, b.right, b.top + 16f * s.k + 4f)
        s.canvas.drawRoundRect(bar, 2f, 2f, s.fill(s.ink(0.1f)))
        if (done > 0) s.canvas.drawRoundRect(RectF(bar.left, bar.top, bar.left + bar.width() * done / items.size, bar.bottom), 2f, 2f, s.fill(s.accent))

        val list = RectF(b.left, bar.bottom + 8f, b.right, b.bottom)
        val rowH = 26f * s.k
        val capacity = (list.height() / rowH).toInt().coerceAtLeast(1)
        val overflow = items.size > capacity
        val shown = if (overflow) capacity - 1 else items.size
        val text = s.paint(14f * s.k, s.text, weight = 400)
        val struck = s.paint(14f * s.k, s.ink(0.4f), weight = 400)
        val box = 15f * s.k
        for (i in 0 until shown) {
            val item = items[i]
            val cy = list.top + rowH * i + rowH / 2f
            val cb = RectF(list.left, cy - box / 2f, list.left + box, cy + box / 2f)
            checkbox(s, cb, item.done && real.isNotEmpty())
            val x = cb.right + 10f
            val p = if (item.done) struck else text
            val line = TextFit.ellipsize(item.text, list.right - x, { p.measureText(it) })
            s.canvas.drawText(line, x, cy + s.capHeight(p) / 2f, p)
            if (item.done) s.canvas.drawLine(x, cy, x + p.measureText(line), cy, s.stroke(s.ink(0.4f), 1.2f))
        }
        if (overflow) {
            val more = s.paint(12f * s.k, s.ink(0.5f), font = "mono", weight = 500, tracking = 0.06f)
            s.canvas.drawText("+ ${items.size - shown} more", list.left, list.top + rowH * shown + rowH / 2f + s.capHeight(more) / 2f, more)
        }
    }

    private fun checkbox(s: Scene, r: RectF, checked: Boolean) {
        val radius = r.width() * 0.3f
        if (checked) {
            s.canvas.drawRoundRect(r, radius, radius, s.fill(s.accent))
            val tick = Path().apply {
                moveTo(r.left + r.width() * 0.26f, r.centerY() + r.height() * 0.02f)
                lineTo(r.left + r.width() * 0.44f, r.bottom - r.height() * 0.28f)
                lineTo(r.right - r.width() * 0.24f, r.top + r.height() * 0.3f)
            }
            s.canvas.drawPath(tick, s.stroke(s.style.bg.color, 1.8f))
        } else {
            s.canvas.drawRoundRect(r, radius, radius, s.stroke(s.ink(0.4f), 1.4f))
        }
    }
}
