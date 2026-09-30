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
import kotlin.math.floor
import kotlin.math.sqrt

object CountdownDots : WidgetDesign {
    override val id = "countdown.dots"
    override val category = Category.COUNTDOWN
    override val name = "Day Dots"
    override val blurb = "One dot for every day between the day you set the countdown and the target: elapsed days filled, today lit."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF3F1EC, accent = 0xFF7CF29C, background = 0xFF111413, radius = 30f, padding = 14f)
    override val toggles = listOf(countModeToggle)
    override val signals = setOf(Signal.CONTENT)
    override val motion = "Today's dot breathes slowly."

    private const val MIN_PITCH = 5f

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val v = s.countdown()
        val b = s.box
        val wide = s.w >= s.h * 1.6f
        val gridBox: RectF
        if (wide) {
            val colW = b.width() * 0.34f
            gridBox = RectF(b.left + colW + 14f, b.top, b.right, b.bottom)
            header(s, v, RectF(b.left, b.top, b.left + colW, b.bottom), stacked = true)
        } else {
            val headH = if (s.h >= 300f) 76f else 52f * s.k
            gridBox = RectF(b.left, b.top + headH, b.right, b.bottom - if (s.h >= 300f) 22f else 0f)
            header(s, v, RectF(b.left, b.top, b.right, b.top + headH), stacked = false)
            if (s.h >= 300f) {
                val foot = s.tag(9.5f, s.ink(0.5f))
                s.canvas.drawText("${v.start.dayMonth().uppercase()}  →  ${v.target.dayMonth().uppercase()}", b.left, b.bottom - 4f, foot)
                s.canvas.drawText("${(v.progress * 100).toInt()}%", b.right, b.bottom - 4f, s.tag(9.5f, s.ink(0.5f), Paint.Align.RIGHT))
            }
        }
        grid(s, gridBox, v)
    }

    private fun header(s: Scene, v: CountdownView, r: RectF, stacked: Boolean) {
        val label = s.tag(10f, s.accent)
        s.canvas.drawText(TextFit.ellipsize(v.title.uppercase(), r.width(), label.measurer()), r.left, r.top + 10f * s.k, label)
        val numberH = if (stacked) r.height() * 0.4f else r.height() - 22f * s.k
        val size = s.fit(v.hero, if (stacked) r.width() else r.width() * 0.5f, numberH) * s.hero
        val num = s.paint(size, s.text)
        val baseline = r.top + 18f * s.k + s.capHeight(num)
        s.canvas.drawText(v.hero, r.left, baseline, num)
        val cap = s.tag(10f, s.ink(0.6f))
        if (stacked) {
            s.canvas.drawText(v.reading.caption.uppercase(), r.left, baseline + 18f * s.k, cap)
            val date = s.tag(9.5f, s.ink(0.45f))
            s.canvas.drawText("${v.target.weekday()} ${v.target.short().uppercase()}", r.left, r.bottom - 2f, date)
            s.canvas.drawText("${(v.progress * 100).toInt()}% ELAPSED", r.left, r.bottom - 18f * s.k, s.tag(9.5f, s.ink(0.45f)))
        } else {
            val x = r.left + num.measureText(v.hero) + 8f
            s.canvas.drawText(TextFit.ellipsize(v.reading.caption.uppercase(), r.right - x, cap.measurer()), x, baseline, cap)
        }
    }

    private fun grid(s: Scene, box: RectF, v: CountdownView) {
        val capacity = (floor(box.width() / MIN_PITCH) * floor(box.height() / MIN_PITCH)).toInt().coerceAtLeast(1)
        val perDot = CountdownMath.daysPerDot(v.span, capacity)
        val (count, today) = CountdownMath.dots(v.span, v.elapsed, perDot)
        var pitch = sqrt(box.width() * box.height() / count)
        var cols = floor(box.width() / pitch).toInt().coerceAtLeast(1)
        while (cols * floor(box.height() / pitch).toInt() < count && pitch > 1f) {
            pitch -= 0.25f
            cols = floor(box.width() / pitch).toInt().coerceAtLeast(1)
        }
        val rows = (count + cols - 1) / cols
        val ox = box.left + (box.width() - (cols - 1) * pitch) / 2f
        val oy = box.top + (box.height() - (rows - 1) * pitch) / 2f
        val r = pitch * 0.32f
        val done = s.fill(s.ink(0.85f))
        val todo = s.stroke(s.ink(0.28f), 0.9f)
        for (i in 0 until count) {
            val x = ox + (i % cols) * pitch
            val y = oy + (i / cols) * pitch
            if (i < today) {
                s.canvas.drawCircle(x, y, r, done)
            } else if (i == today) {
                s.canvas.drawCircle(x, y, r * 1.35f, s.fill(s.accent))
                val glow = RectF(x - r * 3f, y - r * 3f, x + r * 3f, y + r * 3f)
                s.flipper(glow, 1500, 2) { f ->
                    drawCircle(x, y, r * (1.35f + 0.9f * f), s.fill(s.ink(0.35f - 0.2f * f, s.accent)))
                    drawCircle(x, y, r * 1.35f, s.fill(s.accent))
                }
            } else {
                s.canvas.drawCircle(x, y, r * 0.85f, todo)
            }
        }
    }
}
