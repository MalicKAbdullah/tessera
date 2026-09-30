package com.malickabdullah.tessera.designs.photo

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.data.PhotoMath
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Toggle
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.max
import kotlin.math.min

object PhotoShuffle : WidgetDesign {
    override val id = "photo.shuffle"
    override val category = Category.PHOTO
    override val name = "Shuffle"
    override val blurb = "An album as a stack of prints; the top one changes on its own."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.WIDE, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFFF4F4F4, accent = 0xFFD4FF3A, background = 0xFF151515, radius = 28f, padding = 16f)
    override val toggles = listOf(
        Toggle.Choice(
            "shuffle",
            "Shuffle",
            listOf("live" to "Every few seconds", "hourly" to "New each hour", "daily" to "New each day"),
            "live",
        ),
        Toggle.Switch("counter", "Page dots", true),
    )
    override val signals = setOf(Signal.CONTENT)
    override val motion = "Set to every few seconds, the top print cross-fades through the album while the home screen is visible."

    /** Frames the flipper may carry at most; the memory plan can lower it. */
    private const val MAX_FRAMES = 6

    override fun liveKey(scene: SceneInputs) = PhotoKit.key(scene, scene.choice("shuffle"))

    override fun draw(s: Scene) {
        val b = s.box
        val photos = s.data.photos.photos
        val choice = s.choice("shuffle")
        val card = RectF(b.left + 4f, b.top + 2f, b.right - 4f, b.bottom - 6f)
        val radius = max(8f, s.style.radius - s.pad * 0.6f)

        // Two prints behind, turned a little, darker the deeper they sit.
        listOf(-4.5f to 2, 3.5f to 1).forEach { (angle, depth) ->
            s.canvas.save()
            s.canvas.rotate(angle, card.centerX(), card.centerY())
            val back = RectF(card).apply { inset(card.width() * 0.035f * depth, card.height() * 0.02f * depth) }
            val clip = PhotoKit.rounded(back, radius)
            val photo = PhotoKit.pick(s, choice, depth).takeIf { photos.size > depth }
            if (photo == null) {
                s.canvas.drawPath(clip, s.fill(s.ink(0.08f + 0.05f * (2 - depth))))
            } else {
                PhotoKit.cover(s, photo, back, clip)
                s.canvas.drawPath(clip, s.fill(withShade(0.55f - 0.15f * (2 - depth))))
            }
            s.canvas.restore()
        }

        val clip = PhotoKit.rounded(card, radius)
        if (photos.isEmpty()) {
            PhotoKit.empty(s, card, clip)
            return
        }
        val start = PhotoMath.rotation(PhotoKit.period(s, choice), photos.size)
        drawFront(s, s.canvas, card, radius, start)
        if (choice != "live" || photos.size < 2) return

        val display = s.context.resources.displayMetrics
        val plan = PhotoMath.flipperPlan(
            budget = PhotoMath.remoteViewsBudget(display.widthPixels, display.heightPixels),
            mainBytes = PhotoMath.bitmapBytes(s.w, s.h, s.bitmapScale),
            wDp = card.width(),
            hDp = card.height(),
            scale = s.bitmapScale,
            minScale = min(s.bitmapScale, display.density * 0.6f),
            want = min(photos.size, MAX_FRAMES),
        )
        if (plan.frames < 2) return
        s.flipper(card, 6000, plan.frames, plan.scale) { i -> drawFront(s, this, card, radius, (start + i) % photos.size) }
    }

    private fun drawFront(s: Scene, c: Canvas, card: RectF, radius: Float, index: Int) {
        val photos = s.data.photos.photos
        val clip = PhotoKit.rounded(card, radius)
        c.drawPath(clip, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.style.bg.color })
        PhotoKit.cover(s, photos[index], card, clip, c)
        c.drawPath(clip, s.stroke(0x26FFFFFF, 0.8f, round = false))
        if (!s.flag("counter") || photos.size < 2) return
        val pitch = 7f
        val shown = min(photos.size, 12)
        val width = (shown - 1) * pitch
        val cy = card.bottom - 10f
        val pill = RectF(card.centerX() - width / 2f - 7f, cy - 6f, card.centerX() + width / 2f + 7f, cy + 6f)
        c.drawRoundRect(pill, 6f, 6f, s.fill(0x66000000))
        for (i in 0 until shown) {
            val x = card.centerX() - width / 2f + i * pitch
            c.drawCircle(x, cy, if (i == index) 2.3f else 1.6f, s.fill(if (i == index) s.accent else s.ink(0.55f)))
        }
    }

    private fun withShade(alpha: Float): Int = ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
}
