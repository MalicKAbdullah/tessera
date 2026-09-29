package com.malickabdullah.tessera.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.malickabdullah.tessera.MainActivity
import com.malickabdullah.tessera.R
import es.antonborri.home_widget.HomeWidgetPlugin

/** Text for the three slots every tile layout has. */
data class TileText(val label: String?, val value: String, val sub: String?, val level: Float? = null)

/**
 * Shared renderer. RemoteViews cannot set a corner radius or alpha on a
 * background, so the surface is drawn into a bitmap sized to the widget's
 * current bounds; text colour, size and weight are applied directly so text
 * stays crisp at any size.
 */
abstract class TesseraWidget : AppWidgetProvider() {
    abstract val kind: String
    open val layout: Int = R.layout.widget_tile
    open val valueSizeSp: Float = 40f

    /** Null when the value slot is a TextClock that ticks natively. */
    abstract fun text(context: Context, prefs: SharedPreferences): TileText?

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { render(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        render(context, manager, id)
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val prefs = HomeWidgetPlugin.getData(context)
        val style = TileStyle.read(prefs, kind)
        val text = text(context, prefs)
        val views = RemoteViews(context.packageName, layout)

        val options = manager.getAppWidgetOptions(id)
        val wDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).coerceAtLeast(80)
        val hDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 110).coerceAtLeast(40)
        views.setImageViewBitmap(R.id.bg, surface(context, style, wDp, hDp, text?.level))

        views.setTextColor(R.id.label, style.accent)
        views.setTextViewTextSize(R.id.label, TypedValue.COMPLEX_UNIT_SP, 11f * style.scale)
        views.setTextColor(R.id.sub, (style.text and 0x00FFFFFF) or (0xA8 shl 24))
        views.setTextViewTextSize(R.id.sub, TypedValue.COMPLEX_UNIT_SP, 13f * style.scale)

        val valueIds = mapOf("light" to R.id.value_light, "regular" to R.id.value_regular, "medium" to R.id.value_medium)
        valueIds.forEach { (weight, viewId) ->
            views.setViewVisibility(viewId, if (weight == style.weight) View.VISIBLE else View.GONE)
            views.setTextColor(viewId, style.text)
            views.setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, valueSizeSp * style.scale)
            if (text != null) views.setTextViewText(viewId, text.value)
        }
        if (text != null) {
            views.setTextViewText(R.id.label, text.label ?: "")
            views.setViewVisibility(R.id.label, if (text.label == null) View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.sub, text.sub ?: "")
            views.setViewVisibility(R.id.sub, if (text.sub == null) View.GONE else View.VISIBLE)
        }

        val launch = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.root,
            PendingIntent.getActivity(context, kind.hashCode(), launch, PendingIntent.FLAG_IMMUTABLE),
        )
        manager.updateAppWidget(id, views)
    }

    private fun surface(context: Context, style: TileStyle, wDp: Int, hDp: Int, level: Float?): Bitmap {
        val density = context.resources.displayMetrics.density
        // Cap the backing bitmap so large widgets stay under the RemoteViews
        // bitmap budget; fitXY upscaling of a solid fill is invisible.
        val scale = minOf(density, 900f / wDp)
        val w = (wDp * scale).toInt()
        val h = (hDp * scale).toInt()
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.surface }
        val r = style.radiusDp * scale
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, paint)
        if (level != null) {
            val inset = 20f * scale
            val y = h - 12f * scale
            val track = 3f * scale
            paint.color = (style.text and 0x00FFFFFF) or (0x1F shl 24)
            canvas.drawRoundRect(RectF(inset, y - track, w - inset, y), track, track, paint)
            paint.color = style.accent
            canvas.drawRoundRect(RectF(inset, y - track, inset + (w - 2 * inset) * level, y), track, track, paint)
        }
        return bitmap
    }
}
