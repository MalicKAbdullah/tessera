package com.malickabdullah.tessera.designs.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.malickabdullah.tessera.data.Photo
import com.malickabdullah.tessera.data.PhotoMath
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.Toggle
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

internal object PhotoKit {
    val showToggle = Toggle.Choice(
        "show",
        "Show",
        listOf("latest" to "Latest photo", "hourly" to "New each hour", "daily" to "New each day"),
        "latest",
    )

    fun captionToggle(default: String) = Toggle.Choice(
        "caption",
        "Caption",
        listOf("off" to "Off", "text" to "Caption", "date" to "Date"),
        default,
    )

    /** Rotation period number for a show/shuffle choice; 0 for anything that does not rotate on a clock. */
    fun period(s: SceneInputs, choice: String): Long = when (choice) {
        "hourly" -> s.now.toEpochSecond() / 3600
        "daily" -> s.now.toLocalDate().toEpochDay()
        else -> 0
    }

    fun key(s: SceneInputs, choice: String): String? = when (choice) {
        "hourly", "daily" -> "$choice|${period(s, choice)}"
        else -> null
    }

    /** The [offset]-th photo after the one the current period shows, or null for an empty album. */
    fun pick(s: SceneInputs, choice: String, offset: Int = 0): Photo? {
        val photos = s.data.photos.photos
        if (photos.isEmpty()) return null
        return photos[PhotoMath.rotation(period(s, choice) + offset, photos.size)]
    }

    fun date(photo: Photo): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(photo.date).uppercase(Locale.getDefault())

    /** The caption the toggle asks for; "Caption" falls back to the date until the user writes one. */
    fun caption(s: SceneInputs, photo: Photo?): String? {
        if (photo == null) return null
        return when (s.choice("caption")) {
            "text" -> s.data.photos.caption.ifBlank { date(photo) }
            "date" -> date(photo)
            else -> null
        }
    }

    /**
     * Decodes [photo] just large enough to cover wPx x hPx and recycles it
     * after [block]: a render holds at most one full photo at a time.
     */
    fun <T> decode(photo: Photo, wPx: Float, hPx: Float, block: (Bitmap) -> T): T {
        val k = max(wPx / photo.width, hPx / photo.height)
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoMath.sampleSize(photo.width, photo.height, ceil(photo.width * k).toInt(), ceil(photo.height * k).toInt())
        }
        val bitmap = checkNotNull(BitmapFactory.decodeFile(photo.file.path, options)) { "Photo ${photo.id} is unreadable" }
        try {
            return block(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun drawCover(c: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint = photoPaint()) {
        val crop = PhotoMath.cover(bitmap.width, bitmap.height, dst.width(), dst.height())
        c.drawBitmap(bitmap, Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height), dst, paint)
    }

    fun photoPaint() = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

    /** Draws [photo] covering [dst], clipped to [clip], on [c] (the scene canvas unless a flipper frame). */
    fun cover(s: Scene, photo: Photo, dst: RectF, clip: Path, c: Canvas = s.canvas, paint: Paint = photoPaint()) {
        decode(photo, dst.width() * s.bitmapScale, dst.height() * s.bitmapScale) { bmp ->
            c.save()
            c.clipPath(clip)
            drawCover(c, bmp, dst, paint)
            c.restore()
        }
    }

    fun rounded(r: RectF, radius: Float) = Path().apply { addRoundRect(r, radius, radius, Path.Direction.CW) }

    /** Superellipse |x|^4 + |y|^4 = 1: the continuous-corner squircle. */
    fun squircle(r: RectF): Path = Path().apply {
        val steps = 96
        for (i in 0..steps) {
            val t = i * 2 * Math.PI / steps
            val c = cos(t)
            val sn = sin(t)
            val x = r.centerX() + r.width() / 2f * (sign(c) * abs(c).pow(0.5)).toFloat()
            val y = r.centerY() + r.height() / 2f * (sign(sn) * abs(sn).pow(0.5)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** A window arch: semicircular top over straight sides, softly rounded at the foot. */
    fun arch(r: RectF): Path = Path().apply {
        val half = r.width() / 2f
        val foot = min(r.width() * 0.08f, 10f)
        moveTo(r.left, r.top + half)
        arcTo(RectF(r.left, r.top, r.right, r.top + r.width()), 180f, 180f)
        lineTo(r.right, r.bottom - foot)
        quadTo(r.right, r.bottom, r.right - foot, r.bottom)
        lineTo(r.left + foot, r.bottom)
        quadTo(r.left, r.bottom, r.left, r.bottom - foot)
        close()
    }

    /** An organic pebble: a smooth closed curve through gently varied radii. */
    fun blob(r: RectF): Path = Path().apply {
        val n = 9
        val pts = (0 until n).map { i ->
            val a = i * 2 * Math.PI / n - Math.PI / 2
            val k = 0.9 + 0.07 * sin(3 * a + 0.8) + 0.04 * cos(5 * a + 0.3)
            (r.centerX() + r.width() / 2f * (k * cos(a)).toFloat()) to (r.centerY() + r.height() / 2f * (k * sin(a)).toFloat())
        }
        val mid = { i: Int ->
            val (ax, ay) = pts[i % n]
            val (bx, by) = pts[(i + 1) % n]
            (ax + bx) / 2f to (ay + by) / 2f
        }
        val (sx, sy) = mid(0)
        moveTo(sx, sy)
        for (i in 1..n) {
            val (cx, cy) = pts[i % n]
            val (mx, my) = mid(i)
            quadTo(cx, cy, mx, my)
        }
        close()
    }

    /** Film grain over [clip]; deterministic so redraws do not shimmer. */
    fun grain(s: Scene, rect: RectF, clip: Path, amount: Float) {
        val rng = Random(11)
        val speck = Paint()
        s.canvas.save()
        s.canvas.clipPath(clip)
        repeat((rect.width() * rect.height() / 5f).toInt()) {
            speck.color = if (rng.nextBoolean()) (((amount * 255).toInt()) shl 24) or 0xFFFFFF else ((amount * 255).toInt()) shl 24
            val x = rect.left + rng.nextFloat() * rect.width()
            val y = rect.top + rng.nextFloat() * rect.height()
            s.canvas.drawRect(x, y, x + 0.7f, y + 0.7f, speck)
        }
        s.canvas.restore()
    }

    /**
     * The empty album: a dot-matrix landscape with an accent sun inside
     * [clip], and an invitation when there is room. Tapping any widget opens
     * its editor, where photos are chosen.
     */
    fun empty(s: Scene, rect: RectF, clip: Path, label: Boolean = true, c: Canvas = s.canvas) {
        c.save()
        c.clipPath(clip)
        c.drawRect(rect, s.fill(s.ink(0.05f)))
        val pitch = (min(rect.width(), rect.height()) / 26f).coerceIn(3.6f, 6.5f)
        val cols = (rect.width() / pitch).toInt() + 1
        val rows = (rect.height() / pitch).toInt() + 1
        val ox = rect.left + (rect.width() - (cols - 1) * pitch) / 2f
        val oy = rect.top + (rect.height() - (rows - 1) * pitch) / 2f
        val sunX = rect.left + rect.width() * 0.72f
        val sunY = rect.top + rect.height() * 0.36f
        val sunR = min(rect.width(), rect.height()) * 0.14f
        val sky = s.fill(s.ink(0.1f))
        val far = s.fill(s.ink(0.3f))
        val near = s.fill(s.ink(0.62f))
        val sun = s.fill(s.accent)
        for (col in 0 until cols) {
            val x = ox + col * pitch
            val u = (x - rect.left) / rect.width()
            val farRidge = rect.top + rect.height() * (0.56f + 0.09f * sin(u * 5.3f + 0.4f) + 0.035f * sin(u * 13f))
            val nearRidge = rect.top + rect.height() * (0.72f + 0.07f * sin(u * 3.1f + 2.2f))
            for (row in 0 until rows) {
                val y = oy + row * pitch
                val dx = x - sunX
                val dy = y - sunY
                val (paint, radius) = when {
                    y >= nearRidge -> near to pitch * 0.36f
                    y >= farRidge -> far to pitch * 0.33f
                    dx * dx + dy * dy <= sunR * sunR -> sun to pitch * 0.38f
                    else -> sky to pitch * 0.2f
                }
                c.drawCircle(x, y, radius, paint)
            }
        }
        if (label && rect.height() >= 64f && rect.width() >= 90f) {
            val size = s.fit("Choose a photo", rect.width() * 0.62f, rect.height() * 0.12f).coerceAtMost(17f * s.k)
            val title = s.paint(size, s.text)
            val left = rect.left + min(14f, rect.width() * 0.08f)
            val top = rect.top + min(14f, rect.height() * 0.08f)
            val fm = title.fontMetrics
            c.drawText("Choose a photo", left, top - fm.ascent, title)
            val hint = s.paint(max(7f, size * 0.52f), s.ink(0.55f), font = "mono", weight = 500, tracking = 0.1f)
            c.drawText("TAP TO ADD", left, top - fm.ascent + fm.descent + hint.textSize * 1.3f, hint)
        }
        c.restore()
    }
}
