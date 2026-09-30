package com.malickabdullah.tessera.engine

import android.app.PendingIntent
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import com.malickabdullah.tessera.R
import com.malickabdullah.tessera.data.Data
import java.time.ZonedDateTime
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Renderer {
    /**
     * RemoteViews bitmaps cross a binder transaction and count against the
     * host's bitmap budget, so the backing bitmap is capped at this many
     * pixels; above it the ImageView upscales (fitXY) by a small factor.
     */
    const val MAX_PIXELS = 900_000

    /** Bitmap pixels per dp for a widget of wDp x hDp on a screen of [density]. */
    fun bitmapScale(wDp: Float, hDp: Float, density: Float): Float =
        min(density, sqrt(MAX_PIXELS / (wDp * hDp)))

    fun build(
        context: Context,
        design: WidgetDesign,
        style: Style,
        wDp: Float,
        hDp: Float,
        click: PendingIntent?,
        now: ZonedDateTime = ZonedDateTime.now(),
        data: Data = Data(context),
    ): RemoteViews {
        val density = context.resources.displayMetrics.density
        val scale = bitmapScale(wDp, hDp, density)
        val bitmap = Bitmap.createBitmap(ceil(wDp * scale).toInt(), ceil(hDp * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        val scene = Scene(SceneInputs(context, design, style, data, now), canvas, wDp, hDp, scale)
        drawSurface(scene)
        design.draw(scene)

        val views = RemoteViews(context.packageName, R.layout.widget_frame)
        views.setImageViewBitmap(R.id.bg, bitmap)
        views.setContentDescription(R.id.bg, design.name)
        views.removeAllViews(R.id.overlay)
        scene.overlays.forEach { views.addView(R.id.overlay, overlay(context, it, wDp, hDp, density)) }
        if (click != null) views.setOnClickPendingIntent(R.id.root, click)
        return views
    }

    private fun overlay(context: Context, o: Overlay, wDp: Float, hDp: Float, density: Float): RemoteViews {
        val l = (o.rect.left * density).toInt()
        val t = (o.rect.top * density).toInt()
        val r = ((wDp - o.rect.right) * density).toInt()
        val b = ((hDp - o.rect.bottom) * density).toInt()
        return when (o) {
            is Overlay.Clock -> RemoteViews(context.packageName, if (o.caps) o.face.capsClockLayout else o.face.clockLayout).apply {
                setCharSequence(R.id.tc, "setFormat12Hour", o.format12)
                setCharSequence(R.id.tc, "setFormat24Hour", o.format24)
                if (o.zone != null) setString(R.id.tc, "setTimeZone", o.zone)
                setTextViewTextSize(R.id.tc, TypedValue.COMPLEX_UNIT_DIP, o.size)
                setTextColor(R.id.tc, o.color)
                setInt(R.id.tc, "setGravity", o.gravity)
                setViewPadding(R.id.tc, l, t, r, b)
            }
            is Overlay.Analog -> {
                // Hand tint is remotable from API 31; earlier hosts get the
                // prebuilt light or dark hands that best match the colour.
                val tintable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val layout = when {
                    tintable || luminance(o.hour) > 0.5f -> R.layout.ov_analog_light
                    else -> R.layout.ov_analog_dark
                }
                RemoteViews(context.packageName, layout).apply {
                    setViewPadding(R.id.analog_box, l, t, r, b)
                    if (tintable) {
                        setColorStateList(R.id.analog, "setHourHandTintList", ColorStateList.valueOf(o.hour))
                        setColorStateList(R.id.analog, "setMinuteHandTintList", ColorStateList.valueOf(o.minute))
                    }
                }
            }
            is Overlay.Flipper -> RemoteViews(context.packageName, R.layout.ov_flipper).apply {
                setViewPadding(R.id.flipper, l, t, r, b)
                setInt(R.id.flipper, "setFlipInterval", o.intervalMs)
                o.frames.forEach { frame ->
                    addView(R.id.flipper, RemoteViews(context.packageName, R.layout.ov_frame).apply { setImageViewBitmap(R.id.frame, frame) })
                }
            }
        }
    }

    /** Paints the style's surface, then clips so designs never draw outside the rounded corners. */
    private fun drawSurface(s: Scene) {
        val style = s.style
        val radius = min(style.radius, s.minSide / 2f)
        val rect = RectF(0f, 0f, s.w, s.h)
        val path = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
        val bg = style.bg
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(bg.color, style.opacity) }
        when (bg.kind) {
            BgKind.TRANSPARENT -> Unit
            BgKind.SOLID, BgKind.DOTS, BgKind.GRAIN -> s.canvas.drawPath(path, paint)
            BgKind.GRADIENT -> {
                val a = Math.toRadians(bg.angle.toDouble())
                val dx = cos(a).toFloat() * s.w / 2f
                val dy = sin(a).toFloat() * s.h / 2f
                paint.shader = LinearGradient(
                    s.w / 2f - dx, s.h / 2f - dy, s.w / 2f + dx, s.h / 2f + dy,
                    withAlpha(bg.color, style.opacity), withAlpha(bg.color2, style.opacity), Shader.TileMode.CLAMP,
                )
                s.canvas.drawPath(path, paint)
            }
            BgKind.PHOTO -> {
                val file = checkNotNull(bg.photo) { "Photo background without a photo path" }
                val photo = checkNotNull(BitmapFactory.decodeFile(file)) { "Photo background $file is unreadable" }
                s.canvas.save()
                s.canvas.clipPath(path)
                val fit = maxOf(s.w / photo.width, s.h / photo.height)
                val m = Matrix().apply {
                    setScale(fit, fit)
                    postTranslate((s.w - photo.width * fit) / 2f, (s.h - photo.height * fit) / 2f)
                }
                s.canvas.drawBitmap(photo, m, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = (style.opacity * 255).toInt() })
                // A fixed scrim keeps text legible on any photo.
                s.canvas.drawPath(path, Paint().apply { color = 0x33000000 })
                s.canvas.restore()
            }
        }
        s.canvas.clipPath(path)
        when (bg.kind) {
            BgKind.DOTS -> s.dotGrid(rect, 7f, 0.8f, s.ink(0.09f))
            BgKind.GRAIN -> {
                val rng = Random(7)
                val speck = Paint()
                repeat((s.w * s.h / 9f).toInt()) {
                    speck.color = if (rng.nextBoolean()) s.ink(0.05f) else 0x0D000000
                    val x = rng.nextFloat() * s.w
                    val y = rng.nextFloat() * s.h
                    s.canvas.drawRect(x, y, x + 0.6f, y + 0.6f, speck)
                }
            }
            else -> Unit
        }
    }
}
