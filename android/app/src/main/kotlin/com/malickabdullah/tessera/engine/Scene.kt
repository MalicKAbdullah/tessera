package com.malickabdullah.tessera.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.format.DateFormat
import com.malickabdullah.tessera.data.Data
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.min

/** Everything a design reads, without a canvas, so [WidgetDesign.liveKey] can run cheaply. */
open class SceneInputs(
    val context: Context,
    val design: WidgetDesign,
    val style: Style,
    val data: Data,
    val now: ZonedDateTime,
) {
    fun flag(key: String): Boolean = when {
        style.toggles.has(key) -> style.toggles.getBoolean(key)
        else -> (design.toggles.first { it.key == key } as Toggle.Switch).default
    }

    fun choice(key: String): String = when {
        style.toggles.has(key) -> style.toggles.getString(key)
        else -> (design.toggles.first { it.key == key } as Toggle.Choice).default
    }

    /** Whether time reads as 24-hour, honouring the design's [hourFormatToggle]. */
    val use24h: Boolean
        get() = when (choice(hourFormatToggle.key)) {
            "24" -> true
            "12" -> false
            else -> DateFormat.is24HourFormat(context)
        }

    /** (12-hour, 24-hour) TextClock patterns; forcing a format sets both to it. */
    fun timeFormats(h12: String, h24: String): Pair<String, String> = when (choice(hourFormatToggle.key)) {
        "24" -> h24 to h24
        "12" -> h12 to h12
        else -> h12 to h24
    }
}

/**
 * A design draws in dp: [canvas] is pre-scaled so (0,0)-([w],[h]) is the
 * widget in dp regardless of the bitmap's pixel density. Live parts that the
 * bitmap cannot keep current are declared as overlays.
 */
class Scene(
    inputs: SceneInputs,
    val canvas: Canvas,
    val w: Float,
    val h: Float,
    /** Bitmap pixels per dp. */
    val bitmapScale: Float,
) : SceneInputs(inputs.context, inputs.design, inputs.style, inputs.data, inputs.now) {
    val overlays = mutableListOf<Overlay>()

    val text: Int get() = style.text
    val accent: Int get() = style.accent
    val k: Float get() = style.scale

    /** Hero text fills its region; the size slider can shrink it but not overflow it. */
    val hero: Float get() = min(1f, style.scale)
    val pad: Float get() = style.padding
    val box: RectF get() = RectF(pad, pad, w - pad, h - pad)
    val minSide: Float get() = min(w, h)

    fun ink(alpha: Float, color: Int = text): Int = withAlpha(color, alpha)

    fun paint(
        size: Float,
        color: Int = text,
        font: String = style.font,
        weight: Int = style.weight,
        align: Paint.Align = Paint.Align.LEFT,
        tracking: Float = style.tracking,
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        typeface = Fonts.typeface(context, font, weight)
        textSize = size
        this.color = color
        textAlign = align
        letterSpacing = tracking
    }

    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    fun stroke(color: Int, width: Float, round: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        if (round) strokeCap = Paint.Cap.ROUND
    }

    /** Largest text size at which [sample] fits in maxW x maxH (height measured as the line box). */
    fun fit(sample: String, maxW: Float, maxH: Float, font: String = style.font, weight: Int = style.weight): Float {
        val p = paint(100f, font = font, weight = weight)
        val fm = p.fontMetrics
        return min(maxW / p.measureText(sample) * 100f, maxH / (fm.descent - fm.ascent) * 100f)
    }

    /** Largest size at which [sample]'s cap height is at most maxH and width at most maxW. */
    fun fitCaps(sample: String, maxW: Float, maxH: Float, font: String = style.font, weight: Int = style.weight): Float {
        val p = paint(100f, font = font, weight = weight)
        return min(maxW / p.measureText(sample) * 100f, maxH / capHeight(p) * 100f)
    }

    fun capHeight(p: Paint): Float {
        val r = Rect()
        p.getTextBounds("H", 0, 1, r)
        return r.height().toFloat()
    }

    /** Draws [s] with its cap height centred on [cy]. */
    fun textMid(s: String, x: Float, cy: Float, p: Paint) = canvas.drawText(s, x, cy + capHeight(p) / 2f, p)

    fun dotGrid(rect: RectF, pitch: Float, radius: Float, color: Int) {
        val p = fill(color)
        val cols = ((rect.width()) / pitch).toInt()
        val rows = ((rect.height()) / pitch).toInt()
        val ox = rect.left + (rect.width() - (cols - 1) * pitch) / 2f
        val oy = rect.top + (rect.height() - (rows - 1) * pitch) / 2f
        for (r in 0 until rows) for (c in 0 until cols) canvas.drawCircle(ox + c * pitch, oy + r * pitch, radius, p)
    }

    /** Live time/date text drawn by a TextClock over the bitmap, in any bundled face. */
    fun textClock(
        rect: RectF,
        formats: Pair<String, String>,
        size: Float,
        color: Int = text,
        font: String = style.font,
        weight: Int = style.weight,
        gravity: Int,
        caps: Boolean = false,
        zone: String? = null,
    ) {
        overlays += Overlay.Clock(RectF(rect), formats.first, formats.second, Fonts.face(font, weight), size, color, gravity, caps, zone)
    }

    /** Live analog hands (platform AnalogClock) centred in [rect]; the dial is drawn in the bitmap. */
    fun analogHands(rect: RectF, hour: Int, minute: Int) {
        overlays += Overlay.Analog(RectF(rect), hour, minute)
    }

    /**
     * A looping ViewFlipper of [count] frames over [rect]; each frame draws in
     * widget dp. [pxPerDp] is the frames' pixels per dp; large flippers pass a
     * lower one to stay inside the RemoteViews bitmap budget.
     */
    fun flipper(rect: RectF, intervalMs: Int, count: Int, pxPerDp: Float = bitmapScale, drawFrame: Canvas.(Int) -> Unit) {
        val frames = (0 until count).map { i ->
            val bmp = Bitmap.createBitmap(
                ceil(rect.width() * pxPerDp).toInt().coerceAtLeast(1),
                ceil(rect.height() * pxPerDp).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            Canvas(bmp).apply {
                scale(pxPerDp, pxPerDp)
                translate(-rect.left, -rect.top)
                drawFrame(i)
            }
            bmp
        }
        overlays += Overlay.Flipper(RectF(rect), intervalMs, frames)
    }
}

sealed class Overlay(val rect: RectF) {
    class Clock(
        rect: RectF,
        val format12: String,
        val format24: String,
        val face: FontFace,
        val size: Float,
        val color: Int,
        val gravity: Int,
        val caps: Boolean,
        val zone: String?,
    ) : Overlay(rect)

    class Analog(rect: RectF, val hour: Int, val minute: Int) : Overlay(rect)

    class Flipper(rect: RectF, val intervalMs: Int, val frames: List<Bitmap>) : Overlay(rect)
}

fun withAlpha(color: Int, alpha: Float): Int =
    ((((color ushr 24) and 0xFF) * alpha).toInt().coerceIn(0, 255) shl 24) or (color and 0x00FFFFFF)

fun luminance(color: Int): Float {
    val r = (color shr 16 and 0xFF) / 255f
    val g = (color shr 8 and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
