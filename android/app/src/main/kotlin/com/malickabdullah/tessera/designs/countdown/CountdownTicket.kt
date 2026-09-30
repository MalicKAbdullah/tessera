package com.malickabdullah.tessera.designs.countdown

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.min

object CountdownTicket : WidgetDesign {
    override val id = "countdown.ticket"
    override val category = Category.COUNTDOWN
    override val name = "Boarding Pass"
    override val blurb = "A ticket for the trip ahead: event, date, a route line that fills as the day nears, and a tear-off stub with the days left."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("condensed", 500, text = 0xFF1C1A17, accent = 0xFFD9482B, background = 0xFFF2E9D8, radius = 22f, padding = 16f)
    override val toggles = listOf(countModeToggle)
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = scene.now.toLocalDate().toString()

    override fun draw(s: Scene) {
        val v = s.countdown()
        val vertical = s.h > s.w * 0.8f
        val stubFrac = 0.3f
        val notchR = 9f
        val main: RectF
        val stub: RectF
        val b = s.box
        if (vertical) {
            val split = s.h * (1f - stubFrac)
            main = RectF(b.left, b.top, b.right, split - notchR - 4f)
            stub = RectF(b.left, split + notchR + 4f, b.right, b.bottom)
        } else {
            val split = s.w * (1f - stubFrac)
            main = RectF(b.left, b.top, split - notchR - 4f, b.bottom)
            stub = RectF(split + notchR + 4f, b.top, b.right, b.bottom)
        }
        drawMain(s, v, main)
        drawStub(s, v, stub, vertical)

        val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        val dash = s.stroke(s.ink(0.35f), 1.2f, round = false).apply { pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }
        if (vertical) {
            val y = s.h * (1f - stubFrac)
            s.canvas.drawLine(notchR + 6f, y, s.w - notchR - 6f, y, dash)
            s.canvas.drawCircle(0f, y, notchR, clear)
            s.canvas.drawCircle(s.w, y, notchR, clear)
        } else {
            val x = s.w * (1f - stubFrac)
            s.canvas.drawLine(x, notchR + 6f, x, s.h - notchR - 6f, dash)
            s.canvas.drawCircle(x, 0f, notchR, clear)
            s.canvas.drawCircle(x, s.h, notchR, clear)
        }
    }

    private fun drawMain(s: Scene, v: CountdownView, r: RectF) {
        val tag = s.tag(8.5f, s.ink(0.55f))
        s.canvas.drawText(if (v.countUp) "MEMORY PASS" else "BOARDING PASS", r.left, r.top + 8f, tag)
        s.canvas.drawText("ADMIT ONE", r.right, r.top + 8f, s.tag(8.5f, s.accent, Paint.Align.RIGHT))
        s.canvas.drawLine(r.left, r.top + 15f, r.right, r.top + 15f, s.stroke(s.ink(0.18f), 1f))

        val titleH = min(r.height() * 0.3f, 52f)
        val title = v.title.uppercase()
        val titlePaint = s.paint(s.fit(title, r.width(), titleH) * s.hero, s.text)
        s.canvas.drawText(title, r.left, r.top + 22f + s.capHeight(titlePaint) + 2f, titlePaint)

        val routeY = r.top + 22f + titleH + 14f
        route(s, v, RectF(r.left, routeY - 8f, r.right, routeY + 8f))

        val fields = listOf("DATE" to v.target.dayMonth().uppercase(), "DAY" to v.target.weekday(), (if (v.countUp) "SINCE" else "STATUS") to statusWord(v))
        val fieldY = if (r.height() > 150f) routeY + 30f else r.bottom - 26f
        val colW = r.width() / fields.size
        val label = s.tag(8.5f, s.ink(0.5f))
        val value = s.paint(min(20f * s.k, colW / 4.2f), s.text, weight = 700)
        fields.forEachIndexed { i, (l, t) ->
            val x = r.left + i * colW
            s.canvas.drawText(l, x, fieldY, label)
            s.canvas.drawText(TextFit.ellipsize(t, colW - 6f, value.measurer()), x, fieldY + 20f * s.k, value)
        }
        if (r.height() > 150f) {
            val phrase = s.paint(14f * s.k, s.ink(0.7f), font = "grotesk", weight = 400)
            s.canvas.drawText(TextFit.ellipsize(v.target.short(), r.width(), phrase.measurer()), r.left, r.bottom - 2f, phrase)
        }
    }

    private fun statusWord(v: CountdownView) = when {
        v.days == 0L -> "TODAY"
        v.countUp -> "${v.reading.number}D"
        v.days > 0 -> "BOARDING"
        else -> "ARRIVED"
    }

    /** Dashed route from start to target with a marker at the current progress. */
    private fun route(s: Scene, v: CountdownView, r: RectF) {
        val y = r.centerY()
        val dash = s.stroke(s.ink(0.35f), 1.4f, round = false).apply { pathEffect = DashPathEffect(floatArrayOf(3f, 4f), 0f) }
        val x0 = r.left + 4f
        val x1 = r.right - 4f
        val xm = x0 + (x1 - x0) * v.progress
        s.canvas.drawLine(xm, y, x1, y, dash)
        s.canvas.drawLine(x0, y, xm, y, s.stroke(s.accent, 2f))
        s.canvas.drawCircle(x0, y, 3.2f, s.stroke(s.text, 1.4f))
        s.canvas.drawCircle(x1, y, 3.2f, s.fill(s.text))
        s.canvas.drawCircle(xm, y, 5.2f, s.fill(s.accent))
        s.canvas.drawCircle(xm, y, 2f, s.fill(s.style.bg.color))
    }

    private fun drawStub(s: Scene, v: CountdownView, r: RectF, vertical: Boolean) {
        val bars = CountdownMath.barcode("${v.title}${v.target}", 34)
        val unit = if (vertical) (r.width() * 0.5f) / bars.sum() else min(r.width() / bars.sum() * 0.9f, 2.4f)
        val barH = if (vertical) r.height() * 0.24f else r.height() * 0.22f
        val barsW = bars.sum() * unit
        val bx = if (vertical) r.left else r.centerX() - barsW / 2f
        val by = r.bottom - barH
        var x = bx
        val ink = s.fill(s.ink(0.85f))
        bars.forEachIndexed { i, w ->
            if (i % 2 == 0) s.canvas.drawRect(x, by, x + w * unit, by + barH, ink)
            x += w * unit
        }
        val numRect = if (vertical) RectF(r.left, r.top, r.right, by - 8f) else RectF(r.left, r.top + 10f, r.right, by - 10f)
        val big = v.hero
        val np = s.paint(s.fit(big, numRect.width(), numRect.height() * 0.7f) * s.hero, s.accent, align = if (vertical) Paint.Align.LEFT else Paint.Align.CENTER)
        val nx = if (vertical) numRect.left else numRect.centerX()
        s.canvas.drawText(big, nx, numRect.top + s.capHeight(np) + 2f, np)
        val cap = if (v.days == 0L) "DEPARTS" else v.reading.caption.uppercase()
        val capP = s.tag(9f, s.ink(0.6f), if (vertical) Paint.Align.LEFT else Paint.Align.CENTER)
        s.canvas.drawText(TextFit.ellipsize(cap, numRect.width(), capP.measurer()), nx, numRect.top + s.capHeight(np) + 20f, capP)
    }
}
