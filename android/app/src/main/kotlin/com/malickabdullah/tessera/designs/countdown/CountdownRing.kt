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
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object CountdownRing : WidgetDesign {
    override val id = "countdown.ring"
    override val category = Category.COUNTDOWN
    override val name = "Progress Ring"
    override val blurb = "A big day count inside a ring that fills from the day you set the countdown to the target date."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFF2F3F5, accent = 0xFF8F7BFF, background = 0xFF141416, radius = 30f, padding = 14f)
    override val toggles = listOf(countModeToggle, Toggle.Switch("pulse", "Pulsing ring tip", true))
    override val signals = setOf(Signal.CONTENT)
    override val motion = "The tip of the ring glows in a slow pulse."

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val v = s.countdown()
        val b = s.box
        val wide = s.w >= s.h * 1.6f
        val footer = if (s.h >= 300f) 44f else 0f
        val ringBox: RectF
        val textBox: RectF?
        if (wide) {
            ringBox = RectF(b.left, b.top, b.left + b.height(), b.bottom)
            textBox = RectF(ringBox.right + 16f, b.top, b.right, b.bottom)
        } else {
            ringBox = RectF(b.left, b.top + 18f * s.k, b.right, b.bottom - footer - 4f)
            textBox = null
            val label = s.tag(10f, s.accent)
            s.canvas.drawText(TextFit.ellipsize(v.title.uppercase(), b.width(), label.measurer()), b.left, b.top + 10f * s.k, label)
        }
        val (cx, cy, r) = drawRing(s, ringBox, v)
        val bigSize = s.fit(v.hero, r * 1.5f, r * 0.85f) * s.hero
        val big = s.paint(bigSize, s.text, align = Paint.Align.CENTER)
        s.textMid(v.hero, cx, cy - r * 0.1f, big)
        val cap = if (v.days == 0L) v.target.short() else v.reading.caption
        val capPaint = s.tag(min(r * 0.13f / s.k, 12f), s.ink(0.6f), Paint.Align.CENTER)
        s.canvas.drawText(TextFit.ellipsize(cap.uppercase(), r * 1.7f, capPaint.measurer()), cx, cy + r * 0.5f, capPaint)

        if (textBox != null) drawSide(s, textBox, v)
        if (footer > 0f) {
            val items = if (v.countUp) {
                listOf("SINCE" to v.target.short(), "YEARS" to "${if (v.days < 0) v.reading.number / CountdownMath.YEAR else 0}", "THIS YEAR" to "${(v.progress * 100).toInt()}%")
            } else {
                listOf("START" to v.start.dayMonth(), "TARGET" to v.target.dayMonth(), "DONE" to "${(v.progress * 100).toInt()}%")
            }
            s.statRow(RectF(b.left, b.bottom - 40f, b.right, b.bottom), items)
        }
    }

    /** Draws the ring and returns (cx, cy, inner radius). */
    private fun drawRing(s: Scene, box: RectF, v: CountdownView): Triple<Float, Float, Float> {
        val side = min(box.width(), box.height())
        val cx = box.centerX()
        val cy = box.centerY()
        val stroke = side * 0.07f
        val r = side / 2f - stroke * 2.2f
        val arc = RectF(cx - r, cy - r, cx + r, cy + r)
        s.canvas.drawArc(arc, 0f, 360f, false, s.stroke(s.ink(0.09f), stroke))
        val sweep = 360f * v.progress
        if (sweep >= 1f) s.canvas.drawArc(arc, -90f, sweep, false, s.stroke(s.accent, stroke))

        val ticks = 60
        for (i in 0 until ticks) {
            val a = Math.toRadians(-90.0 + 360.0 * i / ticks)
            val major = i % 5 == 0
            val r0 = r + stroke * 0.95f
            val r1 = r0 + if (major) stroke * 0.85f else stroke * 0.42f
            val passed = i.toFloat() / ticks < v.progress
            s.canvas.drawLine(
                cx + cos(a).toFloat() * r0, cy + sin(a).toFloat() * r0,
                cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1,
                s.stroke(if (passed) s.ink(0.6f) else s.ink(0.2f), if (major) 1.3f else 0.8f),
            )
        }

        if (sweep >= 1f && sweep < 359f) {
            val a = Math.toRadians(-90.0 + sweep)
            val tx = cx + cos(a).toFloat() * r
            val ty = cy + sin(a).toFloat() * r
            s.canvas.drawCircle(tx, ty, stroke * 0.32f, s.fill(s.style.bg.color))
            if (s.flag("pulse")) {
                val glow = RectF(tx - stroke * 1.6f, ty - stroke * 1.6f, tx + stroke * 1.6f, ty + stroke * 1.6f)
                s.flipper(glow, 900, 3) { i ->
                    drawCircle(tx, ty, stroke * (0.9f + 0.3f * i), s.fill(s.ink(0.35f - 0.1f * i, s.accent)))
                    drawCircle(tx, ty, stroke * 0.32f, s.fill(s.style.bg.color))
                }
            }
        }
        return Triple(cx, cy, r)
    }

    private fun drawSide(s: Scene, box: RectF, v: CountdownView) {
        val label = s.tag(10f, s.accent)
        s.canvas.drawText(TextFit.ellipsize(v.title.uppercase(), box.width(), label.measurer()), box.left, box.top + 12f * s.k, label)
        val phrase = s.paint(22f * s.k, s.text, weight = 500)
        s.canvas.drawText(TextFit.ellipsize(v.reading.phrase, box.width(), phrase.measurer()), box.left, box.centerY() + 4f, phrase)
        val date = s.tag(10.5f, s.ink(0.6f))
        s.canvas.drawText("${v.target.weekday()} ${v.target.short().uppercase()}", box.left, box.centerY() + 24f * s.k, date)
        val bar = RectF(box.left, box.bottom - 6f, box.right, box.bottom)
        s.canvas.drawRoundRect(bar, 3f, 3f, s.fill(s.ink(0.1f)))
        if (v.progress > 0f) {
            s.canvas.drawRoundRect(RectF(bar.left, bar.top, bar.left + bar.width() * v.progress, bar.bottom), 3f, 3f, s.fill(s.accent))
        }
        val pct = s.tag(9f, s.ink(0.5f), Paint.Align.RIGHT)
        s.canvas.drawText("${(v.progress * 100).toInt()}%", box.right, box.bottom - 12f, pct)
    }
}
