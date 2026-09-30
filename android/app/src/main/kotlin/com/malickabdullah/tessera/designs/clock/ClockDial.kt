package com.malickabdullah.tessera.designs.clock

import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Hands are the platform AnalogClock (live, no alarms); everything under
 * them — face, ticks, numerals, date window — is the bitmap.
 */
object ClockDial : WidgetDesign {
    override val id = "clock.dial"
    override val category = Category.CLOCK
    override val name = "Chronograph"
    override val blurb = "Live analog hands over a finely ticked dial with a date window."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFFF3F1EC, accent = 0xFFC9A77C, background = 0xFF141518, radius = 48f, padding = 10f)
    override val toggles = listOf(
        Toggle.Choice("numerals", "Numerals", listOf("none" to "None", "quarters" to "12 · 3 · 6 · 9", "all" to "All"), "quarters"),
        Toggle.Switch("date", "Date window", true),
    )

    override fun draw(s: Scene) {
        val b = s.box
        val r = min(b.width(), b.height()) / 2f
        val cx = b.centerX()
        val cy = b.centerY()
        val c = s.canvas
        c.drawCircle(cx, cy, r, s.fill(s.ink(0.045f)))
        c.drawCircle(cx, cy, r - 0.5f, s.stroke(s.ink(0.12f), 1f))
        c.drawCircle(cx, cy, r * 0.62f, s.stroke(s.ink(0.35f, s.accent), 0.8f))

        val minor = s.stroke(s.ink(0.32f), 0.8f)
        val major = s.stroke(s.ink(0.9f), 1.8f)
        for (i in 0 until 60) {
            val a = Math.toRadians(i * 6.0 - 90)
            val isMajor = i % 5 == 0
            val outer = r - 3f
            val inner = outer - if (isMajor) r * 0.1f else r * 0.045f
            c.drawLine(
                cx + cos(a).toFloat() * inner, cy + sin(a).toFloat() * inner,
                cx + cos(a).toFloat() * outer, cy + sin(a).toFloat() * outer,
                if (isMajor) major else minor,
            )
        }

        val numerals = s.choice("numerals")
        val date = s.flag("date")
        if (numerals != "none") {
            val p = s.paint(r * 0.15f * s.k, s.ink(0.85f), align = Paint.Align.CENTER)
            val hours = if (numerals == "all") 1..12 else listOf(3, 6, 9, 12)
            for (h in hours) {
                if (h == 3 && date) continue
                val a = Math.toRadians(h * 30.0 - 90)
                val d = r * 0.72f
                s.textMid("$h", cx + cos(a).toFloat() * d, cy + sin(a).toFloat() * d, p)
            }
        }
        val brand = s.paint(r * 0.075f, s.ink(0.42f), align = Paint.Align.CENTER, tracking = 0.25f)
        c.drawText("TESSERA", cx, cy + r * 0.38f, brand)

        if (date) {
            val win = RectF(cx + r * 0.5f, cy - r * 0.1f, cx + r * 0.8f, cy + r * 0.1f)
            c.drawRoundRect(win, 2f, 2f, s.fill(s.ink(0.08f)))
            c.drawRoundRect(win, 2f, 2f, s.stroke(s.ink(0.3f, s.accent), 0.8f))
            s.textClock(win, "d" to "d", s.fit("28", win.width() * 0.8f, win.height() * 0.95f), s.accent, gravity = Gravity.CENTER)
        }
        s.analogHands(RectF(cx - r, cy - r, cx + r, cy + r), s.text, s.accent)
    }
}
