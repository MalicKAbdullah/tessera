package com.malickabdullah.tessera.designs.photo

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import java.util.Locale
import kotlin.math.max

object PhotoDuotone : WidgetDesign {
    override val id = "photo.duotone"
    override val category = Category.PHOTO
    override val name = "Duotone"
    override val blurb = "A two-ink poster: shadows in the surface colour, highlights in your accent."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("condensed", 700, text = 0xFFFFF1E6, accent = 0xFFFF6A3D, background = 0xFF1B1035, radius = 28f, padding = 14f)
    override val toggles = listOf(PhotoKit.showToggle, PhotoKit.captionToggle("date"), Toggle.Switch("grain", "Film grain", true))
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("show"))

    /** Luminance mapped linearly from [shadow] (black) to [highlight] (white). */
    fun matrix(shadow: Int, highlight: Int): ColorMatrix {
        val row = { shift: Int ->
            val lo = (shadow shr shift and 0xFF).toFloat()
            val hi = (highlight shr shift and 0xFF).toFloat()
            val span = (hi - lo) / 255f
            floatArrayOf(0.2126f * span, 0.7152f * span, 0.0722f * span, 0f, lo)
        }
        return ColorMatrix(row(16) + row(8) + row(0) + floatArrayOf(0f, 0f, 0f, 1f, 0f))
    }

    override fun draw(s: Scene) {
        val area = RectF(0f, 0f, s.w, s.h)
        val clip = PhotoKit.rounded(area, s.style.radius)
        val photo = PhotoKit.pick(s, s.choice("show"))
        if (photo == null) {
            PhotoKit.empty(s, area, clip)
        } else {
            val paint = PhotoKit.photoPaint().apply {
                colorFilter = ColorMatrixColorFilter(matrix(s.style.bg.color, s.accent))
            }
            PhotoKit.cover(s, photo, area, clip, paint = paint)
        }
        if (s.flag("grain")) PhotoKit.grain(s, area, clip, 0.07f)
        val caption = PhotoKit.caption(s, photo)?.uppercase(Locale.getDefault()) ?: return
        val b = s.box
        val base = s.paint(100f)
        val wrapped = Wrap.fit(caption, 2, b.width(), b.height() * 0.42f, 0.98f) { base.measureText(it) }
        val p = s.paint(wrapped.size * s.hero, s.text)
        val lineH = p.textSize * 0.98f
        var y = b.top + s.capHeight(p)
        wrapped.lines.forEach {
            s.canvas.drawText(it, b.left, y, p)
            y += lineH
        }
        if (photo != null && s.choice("caption") == "text" && s.data.photos.caption.isNotBlank()) {
            val meta = s.paint(max(7f, 9f * s.k), s.ink(0.8f), font = "mono", weight = 500, tracking = 0.12f)
            s.canvas.drawText(PhotoKit.date(photo), b.left, b.bottom - meta.fontMetrics.descent, meta)
        }
    }
}
