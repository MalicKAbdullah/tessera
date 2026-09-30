package com.malickabdullah.tessera.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateFormat
import android.view.Gravity
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Overlay
import com.malickabdullah.tessera.engine.Registry
import com.malickabdullah.tessera.engine.Renderer
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import com.malickabdullah.tessera.engine.luminance
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Surface family a screenshot is taken in. */
enum class Theme(val id: String) { LIGHT("light"), DARK("dark") }

/** One screenshot: a design at a size, in a theme, showing one data state. */
data class Shot(val design: WidgetDesign, val size: SizeClass, val theme: Theme, val state: DataState) {
    val name: String get() = "${design.id}_${size.id}_${theme.id}_${state.id}"
}

object Shots {
    /** A 420 dpi phone (2.625 px per dp), the density previews are judged at. */
    const val DENSITY = 2.625f

    /** Data states that change what a category draws. */
    fun states(category: Category): List<DataState> = when (category) {
        Category.CLOCK -> listOf(DataState.FULL, DataState.EMPTY)
        Category.BATTERY -> listOf(DataState.FULL, DataState.CHARGING, DataState.EMPTY)
        Category.CALENDAR -> listOf(DataState.FULL, DataState.EMPTY, DataState.DENIED)
        Category.WEATHER -> listOf(DataState.FULL, DataState.STALE, DataState.DENIED, DataState.EMPTY)
        Category.COUNTDOWN, Category.NOTE, Category.PHOTO, Category.SKY -> listOf(DataState.FULL, DataState.EMPTY)
        Category.DEVICE -> listOf(DataState.FULL, DataState.STALE)
    }

    fun of(category: Category): List<Shot> = Registry.designs.filter { it.category == category }.flatMap { d ->
        d.sizes.flatMap { size -> Theme.entries.flatMap { theme -> states(category).map { Shot(d, size, theme, it) } } }
    }

    fun isLight(style: Style) = style.bg.kind != BgKind.TRANSPARENT && luminance(style.bg.color) > 0.5f

    /**
     * The design's shipped defaults in the theme they were made for; the
     * brand palette of the other theme otherwise, keeping font, surface kind
     * and toggles, which is what a user gets by picking those swatches.
     */
    fun style(design: WidgetDesign, theme: Theme): Style {
        val d = design.defaults
        if (isLight(d) == (theme == Theme.LIGHT)) return d
        return when (theme) {
            Theme.DARK -> d.copy(
                text = 0xFFF2F3F5.toInt(),
                accent = 0xFFD4FF3A.toInt(),
                bg = d.bg.copy(color = 0xFF0B0C0E.toInt(), color2 = 0xFF1F2125.toInt()),
            )
            Theme.LIGHT -> d.copy(
                text = 0xFF16171A.toInt(),
                accent = 0xFF2F6BFF.toInt(),
                bg = d.bg.copy(color = 0xFFF2F3F5.toInt(), color2 = 0xFFE2E5EA.toInt()),
            )
        }
    }

    /** Problems a render reveals that a picture might hide, e.g. clock text wider than its overlay. */
    val warnings = mutableListOf<String>()

    /** The widget exactly as the launcher shows it, overlays included, at [pxPerDp]. */
    fun render(context: Context, shot: Shot, photoDir: File, pxPerDp: Float? = null): Bitmap {
        val w = shot.size.widthDp.toFloat()
        val h = shot.size.heightDp.toFloat()
        val scale = pxPerDp ?: Renderer.bitmapScale(w, h, DENSITY)
        val (bitmap, scene) = Renderer.draw(
            context, shot.design, style(shot.design, shot.theme), w, h, scale, Fixed.now, FixedData(context, shot.state, photoDir),
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        val use24h = DateFormat.is24HourFormat(context)
        scene.overlays.forEach { composite(context, canvas, it, use24h, shot) }
        return bitmap
    }

    /**
     * Draws an overlay the way its RemoteViews view would: a TextClock shows
     * the fixed time in its face and size with `includeFontPadding=false`
     * line metrics, AnalogClock hands point at the fixed time, and a flipper
     * shows its first frame.
     */
    private fun composite(context: Context, c: Canvas, o: Overlay, use24h: Boolean, shot: Shot) {
        when (o) {
            is Overlay.Clock -> {
                val zone = TimeZone.getTimeZone(o.zone ?: Fixed.zone.id)
                val cal = Calendar.getInstance(zone).apply { timeInMillis = Fixed.now.toInstant().toEpochMilli() }
                var text = DateFormat.format(if (use24h) o.format24 else o.format12, cal).toString()
                if (o.caps) text = text.uppercase()
                val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                    typeface = context.resources.getFont(o.face.font)
                    textSize = o.size
                    color = o.color
                }
                val fm = p.fontMetrics
                val tw = p.measureText(text)
                val r = o.rect
                val x = when (o.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                    Gravity.CENTER_HORIZONTAL -> r.left + (r.width() - tw) / 2f
                    Gravity.RIGHT, Gravity.END -> r.right - tw
                    else -> r.left
                }
                val lineH = fm.descent - fm.ascent
                val baseline = when (o.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                    Gravity.CENTER_VERTICAL -> r.top + (r.height() - lineH) / 2f - fm.ascent
                    Gravity.BOTTOM -> r.bottom - fm.descent
                    else -> r.top - fm.ascent
                }
                if (tw > r.width() + 0.5f) warnings += "${shot.name}: clock \"$text\" is ${tw.toInt()}dp wide in a ${r.width().toInt()}dp box"
                if (lineH > r.height() + 0.5f) warnings += "${shot.name}: clock \"$text\" is ${lineH.toInt()}dp tall in a ${r.height().toInt()}dp box"
                c.save()
                c.clipRect(r)
                c.drawText(text, x, baseline, p)
                c.restore()
            }
            is Overlay.Analog -> {
                val cx = o.rect.centerX()
                val cy = o.rect.centerY()
                val r = min(o.rect.width(), o.rect.height()) / 2f
                val minute = Fixed.now.minute
                val hour = Fixed.now.hour % 12 + minute / 60f
                hand(c, cx, cy, hour / 12f, r * 0.5f, r * 0.07f, o.hour)
                hand(c, cx, cy, minute / 60f, r * 0.78f, r * 0.045f, o.minute)
            }
            is Overlay.Flipper -> c.drawBitmap(o.frames.first(), null, RectF(o.rect), Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }

    private fun hand(c: Canvas, cx: Float, cy: Float, turn: Float, length: Float, width: Float, color: Int) {
        val a = turn * 2 * Math.PI
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(cx, cy, cx + (sin(a) * length).toFloat(), cy - (cos(a) * length).toFloat(), p)
    }
}
