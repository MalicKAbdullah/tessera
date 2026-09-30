package com.malickabdullah.tessera.designs.clock

import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.hourFormatToggle
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

object ClockDual : WidgetDesign {
    private val zones = listOf(
        "Europe/London" to "London",
        "America/New_York" to "New York",
        "America/Los_Angeles" to "Los Angeles",
        "America/Toronto" to "Toronto",
        "America/Sao_Paulo" to "São Paulo",
        "Europe/Berlin" to "Berlin",
        "Europe/Istanbul" to "Istanbul",
        "Asia/Dubai" to "Dubai",
        "Asia/Karachi" to "Karachi",
        "Asia/Kolkata" to "Delhi",
        "Asia/Singapore" to "Singapore",
        "Asia/Tokyo" to "Tokyo",
        "Australia/Sydney" to "Sydney",
    )

    override val id = "clock.dual"
    override val category = Category.CLOCK
    override val name = "Dual Time"
    override val blurb = "Home and a second city side by side, with day or night and the hour offset."
    override val sizes = listOf(SizeClass.WIDE, SizeClass.SMALL)
    override val defaults = Style.of(
        "sans", 300, text = 0xFFF4F4F2, accent = 0xFF9AD0C2, background = 0xFF17191D,
        kind = BgKind.GRADIENT, background2 = 0xFF252A30, radius = 30f, padding = 16f,
    )
    override val toggles = listOf(hourFormatToggle, Toggle.Choice("zone", "Second city", zones, "Europe/London"))

    override fun liveKey(scene: SceneInputs): String {
        val remote = scene.now.withZoneSameInstant(ZoneId.of(scene.choice("zone")))
        return "${scene.now.toLocalDate()}|${scene.now.hour}|${remote.hour}|${scene.now.zone.id}"
    }

    override fun draw(s: Scene) {
        val b = s.box
        val zone = s.choice("zone")
        val home = s.now.zone.id
        val remote = s.now.withZoneSameInstant(ZoneId.of(zone))
        val homeCity = home.substringAfterLast('/').replace('_', ' ')
        val remoteCity = zones.first { it.first == zone }.second
        if (s.w >= s.h * 1.4f) {
            val mid = b.centerX()
            s.canvas.drawLine(mid, b.top + 4f, mid, b.bottom - 4f, s.stroke(s.ink(0.12f), 1f))
            column(s, RectF(b.left, b.top, mid - 10f, b.bottom), homeCity, null, s.now, "HOME")
            column(s, RectF(mid + 10f, b.top, b.right, b.bottom), remoteCity, zone, remote, offset(s.now, remote))
        } else {
            val mid = b.centerY()
            s.canvas.drawLine(b.left, mid, b.right, mid, s.stroke(s.ink(0.12f), 1f))
            row(s, RectF(b.left, b.top, b.right, mid - 6f), homeCity, null, s.now, "HOME")
            row(s, RectF(b.left, mid + 6f, b.right, b.bottom), remoteCity, zone, remote, offset(s.now, remote))
        }
    }

    private fun column(s: Scene, r: RectF, city: String, zone: String?, t: ZonedDateTime, tag: String) {
        val label = s.paint(10f * s.k, s.ink(0.7f), font = "mono", weight = 500, tracking = 0.12f)
        s.canvas.drawText(city.uppercase(), r.left, r.top + 10f, label)
        daylight(s, r.right - 6f, r.top + 6.5f, t)
        val timeArea = RectF(r.left, r.top + 18f, r.right, r.bottom - 30f)
        val size = s.fit(Kit.widest(s, "88:88"), timeArea.width(), timeArea.height()) * s.hero
        s.textClock(timeArea, s.timeFormats("h:mm", "HH:mm"), size, gravity = Gravity.START or Gravity.CENTER_VERTICAL, zone = zone)
        s.textClock(
            RectF(r.left, r.bottom - 28f, r.right, r.bottom - 14f),
            "EEE d MMM" to "EEE d MMM",
            10f * s.k,
            s.ink(0.55f),
            font = "mono",
            weight = 400,
            gravity = Gravity.START or Gravity.CENTER_VERTICAL,
            caps = true,
            zone = zone,
        )
        dayBar(s, RectF(r.left, r.bottom - 6f, r.right - 34f, r.bottom), t)
        val tagPaint = s.paint(9f * s.k, s.accent, font = "mono", weight = 700, align = Paint.Align.RIGHT, tracking = 0.1f)
        s.canvas.drawText(tag, r.right, r.bottom - 0.5f, tagPaint)
    }

    private fun row(s: Scene, r: RectF, city: String, zone: String?, t: ZonedDateTime, tag: String) {
        val label = s.paint(9f * s.k, s.ink(0.7f), font = "mono", weight = 500, tracking = 0.1f)
        s.canvas.drawText(city.uppercase(), r.left, r.top + 9f, label)
        val tagPaint = s.paint(8.5f * s.k, s.accent, font = "mono", weight = 700, align = Paint.Align.RIGHT, tracking = 0.1f)
        s.canvas.drawText(tag, r.right - 14f, r.top + 9f, tagPaint)
        daylight(s, r.right - 5f, r.top + 6f, t)
        val timeArea = RectF(r.left, r.top + 13f, r.right, r.bottom)
        val size = s.fit(Kit.widest(s, "88:88"), timeArea.width(), timeArea.height()) * s.hero
        s.textClock(timeArea, s.timeFormats("h:mm", "HH:mm"), size, gravity = Gravity.START or Gravity.CENTER_VERTICAL, zone = zone)
    }

    private fun daylight(s: Scene, cx: Float, cy: Float, t: ZonedDateTime) {
        if (t.hour in 6..17) Kit.sun(s.canvas, cx, cy, 5f, s.fill(s.accent)) else Kit.moon(s.canvas, cx, cy, 5.5f, s.fill(s.accent))
    }

    private fun dayBar(s: Scene, r: RectF, t: ZonedDateTime) {
        val cy = r.centerY()
        s.canvas.drawLine(r.left, cy, r.right, cy, s.stroke(s.ink(0.15f), 2f))
        val x = r.left + r.width() * (t.hour + t.minute / 60f) / 24f
        s.canvas.drawLine(r.left, cy, x, cy, s.stroke(s.ink(0.55f), 2f))
        s.canvas.drawCircle(x, cy, 2.8f, s.fill(s.accent))
    }

    private fun offset(home: ZonedDateTime, remote: ZonedDateTime): String {
        val minutes = (remote.offset.totalSeconds - home.offset.totalSeconds) / 60
        if (minutes == 0) return "SAME"
        val sign = if (minutes > 0) "+" else "−"
        val m = abs(minutes)
        return if (m % 60 == 0) "$sign${m / 60}H" else "$sign${m / 60}:${"%02d".format(m % 60)}"
    }
}
