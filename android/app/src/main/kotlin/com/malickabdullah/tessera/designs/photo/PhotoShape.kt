package com.malickabdullah.tessera.designs.photo

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.withAlpha
import kotlin.math.max
import kotlin.math.min

object PhotoShape : WidgetDesign {
    override val id = "photo.shape"
    override val category = Category.PHOTO
    override val name = "Cutout"
    override val blurb = "A photo cut into a circle, squircle, arch or pebble, with an accent echo behind it."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("serif", 400, text = 0xFF1B1B1B, accent = 0xFF6C4DFF, background = 0xFFEEF0F3, radius = 28f, padding = 14f)
    override val toggles = listOf(
        Toggle.Choice(
            "mask",
            "Shape",
            listOf("arch" to "Arch", "circle" to "Circle", "squircle" to "Squircle", "blob" to "Pebble"),
            "arch",
        ),
        Toggle.Switch("echo", "Accent echo", true),
        PhotoKit.showToggle,
        PhotoKit.captionToggle("text"),
    )
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("show"))

    override fun draw(s: Scene) {
        val b = s.box
        val photo = PhotoKit.pick(s, s.choice("show"))
        val caption = PhotoKit.caption(s, photo)
        val wide = s.w >= s.h * 1.4f
        val mask = s.choice("mask")
        val shapeRect: RectF
        if (wide) {
            val side = b.height()
            val left = if (caption == null) b.centerX() - shapeWidth(mask, side) / 2f else b.left
            shapeRect = RectF(left, b.top, left + shapeWidth(mask, side), b.bottom)
        } else {
            val room = if (caption != null && b.height() >= 120f) 22f * s.k else 0f
            val h = b.height() - room
            val w = min(b.width(), shapeWidth(mask, h))
            shapeRect = RectF(b.centerX() - w / 2f, b.top, b.centerX() + w / 2f, b.top + min(h, if (mask == "arch") h else w))
        }
        if (s.flag("echo")) {
            val d = max(3f, min(shapeRect.width(), shapeRect.height()) * 0.035f)
            s.canvas.drawPath(path(mask, RectF(shapeRect).apply { offset(d, d) }), s.fill(s.accent))
        }
        val clip = path(mask, shapeRect)
        if (photo == null) {
            // The empty landscape is a translucent wash; the opaque surface stops the echo showing through it.
            s.canvas.drawPath(clip, s.fill(withAlpha(s.style.bg.color, 1f)))
            PhotoKit.empty(s, shapeRect, clip, label = !wide, centred = true)
        } else {
            PhotoKit.cover(s, photo, shapeRect, clip)
        }

        if (wide) {
            val text = RectF(shapeRect.right + 16f, b.top, b.right, b.bottom)
            if (photo == null) {
                val size = s.fit("Choose a photo", text.width(), text.height() * 0.3f).coerceAtMost(26f * s.k)
                s.canvas.drawText("Choose a photo", text.left, text.centerY(), s.paint(size, s.text))
                s.canvas.drawText("TAP TO ADD", text.left, text.centerY() + size * 0.9f, s.paint(8.5f * s.k, s.ink(0.55f), font = "mono", weight = 500, tracking = 0.1f))
            } else if (caption != null) {
                val base = s.paint(100f)
                val wrapped = Wrap.fit(caption, 3, text.width(), text.height() * 0.78f, 1.08f) { base.measureText(it) }
                val p = s.paint(wrapped.size.coerceAtMost(30f * s.k), s.text)
                val lineH = p.textSize * 1.08f
                var y = text.centerY() - lineH * (wrapped.lines.size - 1) / 2f + s.capHeight(p) / 2f
                wrapped.lines.forEach {
                    s.canvas.drawText(it, text.left, y, p)
                    y += lineH
                }
                if (s.choice("caption") == "text" && s.data.photos.caption.isNotBlank()) {
                    s.canvas.drawText(PhotoKit.date(photo), text.left, b.bottom - 2f, s.paint(8.5f * s.k, s.ink(0.55f), font = "mono", weight = 500, tracking = 0.1f))
                }
            }
        } else if (caption != null && b.height() >= 120f) {
            val p = s.paint(s.fit(caption, b.width(), 18f * s.k).coerceAtMost(15f * s.k), s.text, align = Paint.Align.CENTER)
            s.canvas.drawText(caption, b.centerX(), b.bottom - p.fontMetrics.descent, p)
        }
    }

    private fun shapeWidth(mask: String, h: Float) = if (mask == "arch") h * 0.78f else h

    private fun path(mask: String, r: RectF): Path = when (mask) {
        "circle" -> Path().apply { addOval(r, Path.Direction.CW) }
        "squircle" -> PhotoKit.squircle(r)
        "blob" -> PhotoKit.blob(r)
        else -> PhotoKit.arch(r)
    }
}
