package com.malickabdullah.tessera.designs.countdown

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import java.time.LocalDate

object CountdownList : WidgetDesign {
    override val id = "countdown.list"
    override val category = Category.COUNTDOWN
    override val name = "Up Next"
    override val blurb = "Your next few events in one list, soonest first, each with days to go; a 90-day timeline on the large size."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF2F3F5, accent = 0xFF6FB1FF, background = 0xFF121417, radius = 30f, padding = 14f)
    override val signals = setOf(Signal.CONTENT)

    private const val TIMELINE_DAYS = 90

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val today = s.now.toLocalDate()
        val b = s.box
        val large = s.h >= 300f
        val rows = if (s.h < 200f && s.w < 240f) 2 else if (large) 4 else 3
        val items = CountdownMath.upcoming(s.countdownItems(), today, rows)
        val total = s.countdownItems().size

        val head = s.tag(10f, s.accent)
        s.canvas.drawText("UP NEXT", b.left, b.top + 10f * s.k, head)
        s.canvas.drawText("${items.size} OF $total", b.right, b.top + 10f * s.k, s.tag(9f, s.ink(0.45f), Paint.Align.RIGHT))

        val timelineH = if (large) 56f else 0f
        val list = RectF(b.left, b.top + 20f * s.k, b.right, b.bottom - timelineH)
        val rowH = list.height() / items.size
        items.forEachIndexed { i, item -> row(s, RectF(list.left, list.top + i * rowH, list.right, list.top + (i + 1) * rowH), item, today, i == 0, i > 0) }
        if (large) timeline(s, RectF(b.left, b.bottom - timelineH + 10f, b.right, b.bottom), items, today)
    }

    private fun row(s: Scene, r: RectF, item: CountdownMath.Item, today: LocalDate, first: Boolean, rule: Boolean) {
        if (rule) s.canvas.drawLine(r.left, r.top, r.right, r.top, s.stroke(s.ink(0.1f), 1f))
        val days = CountdownMath.days(today, item.date)
        val past = days < 0
        val numW = r.width() * 0.26f
        val numText = if (days == 0L) "0" else kotlin.math.abs(days).toString()
        val color = if (first && !past) s.accent else if (past) s.ink(0.45f) else s.text
        val np = s.paint(s.fit(numText, numW, r.height() * 0.5f) * s.hero, color)
        val cy = r.centerY()
        s.textMid(numText, r.left, cy - 4f, np)
        val unit = s.tag(8f, s.ink(0.5f))
        val unitText = when {
            days == 0L -> "TODAY"
            past -> if (days == -1L) "DAY AGO" else "DAYS AGO"
            days == 1L -> "DAY"
            else -> "DAYS"
        }
        s.canvas.drawText(unitText, r.left, cy + s.capHeight(np) / 2f + 10f, unit)
        val x = r.left + numW + 12f
        val w = r.right - x
        val title = s.paint(15f * s.k, if (past) s.ink(0.6f) else s.text, weight = 500)
        s.canvas.drawText(TextFit.ellipsize(item.title, w, title.measurer()), x, cy - 1f, title)
        val date = s.tag(9f, s.ink(0.5f))
        s.canvas.drawText("${item.date.weekday()} ${item.date.short().uppercase()}", x, cy + 15f * s.k, date)
    }

    private fun timeline(s: Scene, r: RectF, items: List<CountdownMath.Item>, today: LocalDate) {
        val y = r.top + 14f
        s.canvas.drawLine(r.left, y, r.right, y, s.stroke(s.ink(0.2f), 1.2f))
        for (d in 0..TIMELINE_DAYS step 30) {
            val x = r.left + r.width() * d / TIMELINE_DAYS
            s.canvas.drawLine(x, y - 3f, x, y + 3f, s.stroke(s.ink(0.35f), 1f))
            val t = if (d == 0) "TODAY" else "+${d}D"
            val align = when (d) { 0 -> Paint.Align.LEFT; TIMELINE_DAYS -> Paint.Align.RIGHT; else -> Paint.Align.CENTER }
            s.canvas.drawText(t, x, y + 18f, s.tag(8f, s.ink(0.4f), align))
        }
        items.forEachIndexed { i, item ->
            val d = CountdownMath.days(today, item.date)
            if (d in 0..TIMELINE_DAYS) {
                val x = r.left + r.width() * d / TIMELINE_DAYS
                s.canvas.drawCircle(x, y, 4.4f, s.fill(if (i == 0) s.accent else s.text))
            }
        }
    }
}
