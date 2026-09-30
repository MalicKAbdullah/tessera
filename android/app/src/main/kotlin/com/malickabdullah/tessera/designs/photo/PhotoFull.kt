package com.malickabdullah.tessera.designs.photo

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.max

object PhotoFull : WidgetDesign {
    override val id = "photo.full"
    override val category = Category.PHOTO
    override val name = "Full Bleed"
    override val blurb = "Your photo edge to edge, with a caption resting on a soft scrim."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("grotesk", 500, text = 0xFFFFFFFF, accent = 0xFFD4FF3A, background = 0xFF141414, radius = 28f, padding = 14f)
    override val toggles = listOf(PhotoKit.showToggle, PhotoKit.captionToggle("text"), Toggle.Switch("inset", "Inset frame", false))
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("show"))

    override fun draw(s: Scene) {
        val inset = if (s.flag("inset")) 6f else 0f
        val area = RectF(inset, inset, s.w - inset, s.h - inset)
        val clip = PhotoKit.rounded(area, max(0f, s.style.radius - inset))
        val photo = PhotoKit.pick(s, s.choice("show"))
        if (photo == null) {
            PhotoKit.empty(s, area, clip)
            return
        }
        PhotoKit.cover(s, photo, area, clip)
        val caption = PhotoKit.caption(s, photo) ?: return
        val secondary = if (s.choice("caption") == "text" && s.data.photos.caption.isNotBlank()) PhotoKit.date(photo) else null

        val scrimTop = area.bottom - area.height() * 0.5f
        s.canvas.save()
        s.canvas.clipPath(clip)
        s.canvas.drawRect(
            area.left, scrimTop, area.right, area.bottom,
            Paint().apply { shader = LinearGradient(0f, scrimTop, 0f, area.bottom, 0x00000000, 0x8C000000.toInt(), Shader.TileMode.CLAMP) },
        )
        s.canvas.restore()

        val b = s.box
        val size = s.fit(caption, b.width(), 22f).coerceAtMost(15f * s.k).coerceAtLeast(9f)
        val title = s.paint(size, s.text)
        s.canvas.drawText(caption, b.left, b.bottom - title.fontMetrics.descent, title)
        if (secondary != null) {
            val meta = s.paint(max(7f, size * 0.58f), s.ink(0.72f), font = "mono", weight = 500, tracking = 0.1f)
            s.canvas.drawText(secondary, b.left, b.bottom - title.fontMetrics.descent - size * 1.15f, meta)
        }
    }
}
