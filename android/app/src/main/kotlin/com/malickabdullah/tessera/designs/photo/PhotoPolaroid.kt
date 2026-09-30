package com.malickabdullah.tessera.designs.photo

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object PhotoPolaroid : WidgetDesign {
    override val id = "photo.polaroid"
    override val category = Category.PHOTO
    override val name = "Polaroid"
    override val blurb = "Instant-film prints with a dated caption; the wide size fans out three."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of(
        "serif", 400, text = 0xFF1E2024, accent = 0xFF2F5BD8, background = 0xFF1C1D21, kind = BgKind.GRAIN, radius = 28f, padding = 12f,
    )
    override val toggles = listOf(Toggle.Switch("tilt", "Tilt", true), PhotoKit.showToggle, PhotoKit.captionToggle("date"))
    override val signals = setOf(Signal.CONTENT)

    private const val PAPER = 0xFFF6F3EC.toInt()

    /** Print proportions: photo window is square, the chin 3.4x the side margin. */
    private const val ASPECT = 0.84f

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("show"))

    override fun draw(s: Scene) {
        val b = s.box
        val tilt = s.flag("tilt")
        val wide = s.w >= s.h * 1.4f
        val count = if (wide) s.data.photos.photos.size.coerceIn(1, 3) else 1
        val angles = when {
            !tilt -> List(count) { 0f }
            count == 1 -> listOf(-3f)
            count == 2 -> listOf(-5f, 4f)
            else -> listOf(-8f, 1.5f, 7f)
        }
        // Largest card whose rotated bounds still fit the box.
        val maxAngle = Math.toRadians(angles.maxOf { abs(it) }.toDouble())
        val c = cos(maxAngle).toFloat()
        val sn = sin(maxAngle).toFloat()
        val cardH = min(b.height() / (c + ASPECT * sn), (b.width() / (1f + 0.62f * (count - 1))) / (ASPECT * c + sn))
        val cardW = cardH * ASPECT
        val step = if (count == 1) 0f else (b.width() - cardW * c - cardH * sn) / (count - 1)
        val firstX = b.centerX() - step * (count - 1) / 2f
        // Back to front: the first photo lands on top.
        for (i in count - 1 downTo 0) {
            drawCard(s, firstX + step * i, b.centerY(), cardW, cardH, angles[i], i)
        }
    }

    private fun drawCard(s: Scene, cx: Float, cy: Float, w: Float, h: Float, angle: Float, offset: Int) {
        val c = s.canvas
        c.save()
        c.translate(cx, cy)
        c.rotate(angle)
        val card = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PAPER
            setShadowLayer(w * 0.05f, 0f, w * 0.02f, 0x73000000)
        }
        c.drawRoundRect(card, 2.5f, 2.5f, shadow)
        val m = w * 0.065f
        val window = RectF(card.left + m, card.top + m, card.right - m, card.top + m + (w - 2 * m))
        val clip = PhotoKit.rounded(window, 1f)
        val photo = PhotoKit.pick(s, s.choice("show"), offset)
        if (photo == null) {
            PhotoKit.empty(s, window, clip, label = false)
        } else {
            PhotoKit.cover(s, photo, window, clip)
        }
        c.drawRect(window, s.stroke(0x14000000, 0.6f, round = false))
        val chin = RectF(card.left + m, window.bottom, card.right - m, card.bottom)
        val caption = if (photo == null) "Choose a photo" else PhotoKit.caption(s, photo)
        if (caption != null) {
            val size = s.fit(caption, chin.width() * 0.92f, chin.height() * 0.52f).coerceAtMost(18f * s.k)
            val p = s.paint(size, s.text, align = Paint.Align.CENTER)
            s.textMid(caption, chin.centerX(), chin.centerY(), p)
        }
        c.restore()
    }
}
