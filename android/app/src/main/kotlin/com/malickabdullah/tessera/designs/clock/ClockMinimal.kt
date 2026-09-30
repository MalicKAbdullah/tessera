package com.malickabdullah.tessera.designs.clock

import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.format.DateTimeFormatter

object ClockMinimal : WidgetDesign {
    override val id = "clock.minimal"
    override val category = Category.CLOCK
    override val name = "Minimal"
    override val blurb = "A quiet, large time with the full date and your next alarm."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.SMALL)
    override val defaults = Style.of(
        "sans", 300, text = 0xFFF2F3F5, accent = 0xFFD4FF3A, background = 0xFF1B1C20,
        kind = BgKind.GRADIENT, background2 = 0xFF262A31, radius = 28f, padding = 18f, tracking = -0.02f,
    )
    override val toggles = listOf(hourFormatToggle, Toggle.Switch("seconds", "Seconds", false), Toggle.Switch("alarm", "Next alarm", true))
    override val signals = setOf(Signal.ALARM)
    override val motion = "Optional ticking seconds."

    override fun liveKey(scene: SceneInputs) =
        if (scene.flag("alarm")) "${scene.data.nextAlarm?.at}|${scene.now.toLocalDate()}" else null

    override fun draw(s: Scene) {
        val b = s.box
        val alarm = s.flag("alarm")
        val seconds = s.flag("seconds")
        val dateH = 16f * s.k
        val alarmH = if (alarm) 16f * s.k else 0f
        s.textClock(
            RectF(b.left + 10f, b.top, b.right, b.top + dateH),
            "EEEE, d MMMM" to "EEEE, d MMMM",
            12f * s.k,
            s.ink(0.7f),
            weight = 400,
            gravity = Gravity.START or Gravity.CENTER_VERTICAL,
        )
        val area = RectF(b.left + 10f, b.top + dateH + 2f, b.right, b.bottom - alarmH - 4f)
        s.canvas.drawRoundRect(RectF(b.left, area.top + area.height() * 0.18f, b.left + 3f, area.bottom - area.height() * 0.18f), 1.5f, 1.5f, s.fill(s.accent))
        val secW = if (seconds) area.width() * 0.16f else 0f
        val timeArea = RectF(area.left, area.top, area.right - secW, area.bottom)
        val size = s.fit(Kit.widest(s, "88:88"), timeArea.width(), timeArea.height()) * s.hero
        s.textClock(timeArea, s.timeFormats("h:mm", "HH:mm"), size, gravity = Gravity.START or Gravity.CENTER_VERTICAL)
        if (seconds) {
            val secArea = RectF(area.right - secW, area.top, area.right, area.centerY() + size * 0.32f)
            s.textClock(secArea, "ss" to "ss", size * 0.28f, s.accent, weight = 500, gravity = Gravity.END or Gravity.BOTTOM)
        }
        if (alarm) {
            val y = b.bottom - alarmH / 2f
            Kit.bell(s.canvas, b.left + 16f, y, 10f * s.k, s.fill(s.accent))
            val next = s.data.nextAlarm
            val label = if (next == null) {
                "No alarm set"
            } else {
                val at = next.at.atZone(s.now.zone)
                at.format(DateTimeFormatter.ofPattern(if (s.use24h) "HH:mm" else "h:mm a")) + "  ·  " + at.format(DateTimeFormatter.ofPattern("EEE"))
            }
            s.textMid(label, b.left + 26f, y, s.paint(11.5f * s.k, s.ink(if (next == null) 0.45f else 0.8f), weight = 500))
        }
    }
}
