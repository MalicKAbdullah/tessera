package com.malickabdullah.tessera.engine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import com.malickabdullah.tessera.MainActivity

/**
 * The boxes, in dp, a launcher lays one widget out in. A widget is drawn
 * once per box: the bitmap is stretched to fill its view (fitXY) while
 * overlays are positioned in absolute pixels, so a bitmap drawn for any
 * other box would drift away from its TextClocks.
 */
object WidgetSizes {
    /**
     * Android 12+ launchers report every size the widget can be shown at
     * (portrait and landscape, each display of a foldable or tablet) and pick
     * the matching layout themselves. Earlier ones report a min/max range
     * whose portrait box is MIN_WIDTH x MAX_HEIGHT and landscape box
     * MAX_WIDTH x MIN_HEIGHT; only the current orientation's can be supplied.
     * Right after placement some launchers have not reported yet (zeros);
     * the slot's nominal size stands in until onAppWidgetOptionsChanged.
     */
    fun of(options: Bundle, nominal: SizeClass, landscape: Boolean, sdk: Int = Build.VERSION.SDK_INT): List<SizeF> {
        if (sdk >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            val reported = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                .orEmpty().filter { it.width > 0f && it.height > 0f }.distinct()
            if (reported.isNotEmpty()) return reported
        }
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        if (minW <= 0 || maxW <= 0 || minH <= 0 || maxH <= 0) {
            return listOf(SizeF(nominal.widthDp.toFloat(), nominal.heightDp.toFloat()))
        }
        val portrait = SizeF(minW.toFloat(), maxH.toFloat())
        val wide = SizeF(maxW.toFloat(), minH.toFloat())
        return when {
            sdk >= Build.VERSION_CODES.S -> listOf(portrait, wide).distinct()
            landscape -> listOf(wide)
            else -> listOf(portrait)
        }
    }
}

/** Tapping a placed widget opens Tessera on that widget's editor. */
object WidgetTap {
    const val ACTION_EDIT = "com.malickabdullah.tessera.EDIT_WIDGET"
    const val EXTRA_WIDGET_ID = "tessera.instance"

    /**
     * Explicit, so only MainActivity can receive it. CLEAR_TOP with
     * SINGLE_TOP reuses a running MainActivity (onNewIntent) even when
     * another screen, such as the photo picker, sits above it.
     */
    fun intent(context: Context, id: Int): Intent = Intent(context, MainActivity::class.java)
        .setAction(ACTION_EDIT)
        .setData(Uri.parse("tessera://widget/$id"))
        .putExtra(EXTRA_WIDGET_ID, id)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** The widget id is the request code, so each placed widget has its own PendingIntent. */
    fun pendingIntent(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        context,
        id,
        intent(context, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The widget a launch intent was sent from, or null for any other launch. */
    fun widgetId(intent: Intent): Int? =
        intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            .takeIf { it != AppWidgetManager.INVALID_APPWIDGET_ID }
}
