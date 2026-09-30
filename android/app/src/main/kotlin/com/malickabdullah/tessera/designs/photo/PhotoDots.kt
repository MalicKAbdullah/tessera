package com.malickabdullah.tessera.designs.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.malickabdullah.tessera.data.Photo
import com.malickabdullah.tessera.data.PhotoMath
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PhotoDots : WidgetDesign {
    override val id = "photo.dots"
    override val category = Category.PHOTO
    override val name = "Dot Matrix"
    override val blurb = "Your photo re-drawn as a grid of LEDs or a newsprint halftone."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("dot", 700, text = 0xFFF2F2F2, accent = 0xFFD4FF3A, background = 0xFF0B0B0B, radius = 30f, padding = 10f)
    override val toggles = listOf(
        Toggle.Choice("render", "Render", listOf("led" to "LED grid", "halftone" to "Halftone"), "led"),
        Toggle.Choice("pitch", "Dots", listOf("fine" to "Fine", "coarse" to "Coarse"), "fine"),
        Toggle.Choice("ink", "Ink", listOf("text" to "Text colour", "accent" to "Accent"), "text"),
        PhotoKit.showToggle,
    )
    override val signals = setOf(Signal.CONTENT)

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("show"))

    override fun draw(s: Scene) {
        val area = s.box
        val photo = PhotoKit.pick(s, s.choice("show"))
        if (photo == null) {
            PhotoKit.empty(s, area, PhotoKit.rounded(area, 0f))
            return
        }
        val pitch = if (s.choice("pitch") == "coarse") 6.5f else 4.2f
        val ink = if (s.choice("ink") == "accent") s.accent else s.text
        val on = s.fill(ink)
        val off = s.fill(s.ink(0.08f, ink))
        if (s.choice("render") == "halftone") halftone(s, photo, area, pitch * 1.15f, on) else led(s, photo, area, pitch, on, off)
        // One accent LED in the corner: the matrix is "on".
        if (s.choice("ink") == "text") s.canvas.drawCircle(s.w - s.pad * 0.55f - 2f, s.pad * 0.55f + 2f, 2.2f, s.fill(s.accent))
    }

    private fun led(s: Scene, photo: Photo, area: RectF, pitch: Float, on: android.graphics.Paint, off: android.graphics.Paint) {
        val cols = (area.width() / pitch).toInt()
        val rows = (area.height() / pitch).toInt()
        val lum = sample(photo, cols, rows)
        val ox = area.left + (area.width() - (cols - 1) * pitch) / 2f
        val oy = area.top + (area.height() - (rows - 1) * pitch) / 2f
        for (r in 0 until rows) for (c in 0 until cols) {
            val l = lum[r * cols + c]
            val x = ox + c * pitch
            val y = oy + r * pitch
            s.canvas.drawCircle(x, y, pitch * 0.2f, off)
            // Dot area, not radius, tracks brightness, so midtones read true.
            if (l > 0.04f) s.canvas.drawCircle(x, y, pitch * 0.48f * sqrt(l), on)
        }
    }

    /** A 45° screen: dots on a rotated lattice, sampled from a finer luminance grid. */
    private fun halftone(s: Scene, photo: Photo, area: RectF, pitch: Float, on: android.graphics.Paint) {
        val cols = (area.width() / pitch * 2).toInt()
        val rows = (area.height() / pitch * 2).toInt()
        val lum = sample(photo, cols, rows)
        val cos45 = cos(Math.PI / 4).toFloat()
        val sin45 = sin(Math.PI / 4).toFloat()
        val half = (area.width() + area.height()) / pitch
        s.canvas.save()
        s.canvas.clipRect(area)
        for (i in -half.toInt()..half.toInt()) for (j in -half.toInt()..half.toInt()) {
            val u = i * pitch
            val v = j * pitch
            val x = area.centerX() + u * cos45 - v * sin45
            val y = area.centerY() + u * sin45 + v * cos45
            if (x < area.left - pitch || x > area.right + pitch || y < area.top - pitch || y > area.bottom + pitch) continue
            val c = ((x - area.left) / area.width() * cols).toInt().coerceIn(0, cols - 1)
            val r = ((y - area.top) / area.height() * rows).toInt().coerceIn(0, rows - 1)
            val l = lum[r * cols + c]
            if (l > 0.03f) s.canvas.drawCircle(x, y, pitch * 0.62f * sqrt(l), on)
        }
        s.canvas.restore()
    }

    /** Luminance of [photo] cover-cropped to cols x rows, contrast-stretched. */
    private fun sample(photo: Photo, cols: Int, rows: Int): FloatArray {
        val grid = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        PhotoKit.decode(photo, cols * 2f, rows * 2f) { bmp -> PhotoKit.drawCover(Canvas(grid), bmp, RectF(0f, 0f, cols.toFloat(), rows.toFloat())) }
        val px = IntArray(cols * rows)
        grid.getPixels(px, 0, cols, 0, 0, cols, rows)
        grid.recycle()
        return PhotoMath.normalize(
            FloatArray(px.size) {
                val p = px[it]
                (0.2126f * (p shr 16 and 0xFF) + 0.7152f * (p shr 8 and 0xFF) + 0.0722f * (p and 0xFF)) / 255f
            },
        )
    }
}
