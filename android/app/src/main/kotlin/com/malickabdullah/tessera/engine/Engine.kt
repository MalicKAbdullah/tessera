package com.malickabdullah.tessera.engine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.util.SizeF
import android.widget.RemoteViews
import com.malickabdullah.tessera.data.BatteryHistory
import com.malickabdullah.tessera.data.Data
import java.time.ZonedDateTime

/** Resolves what each placed widget shows and pushes fresh RemoteViews to the launcher. */
object Engine {
    private const val TAG = "TesseraEngine"

    /**
     * The app window that redraws its previews when live data changes. The
     * launcher's configure screen is a second activity with its own Flutter
     * engine, so each window claims the hook when it comes to the front and
     * releases only its own.
     */
    @Volatile
    var onDataChanged: (() -> Unit)? = null
        private set

    fun claimDataChanged(listener: () -> Unit) {
        onDataChanged = listener
    }

    fun releaseDataChanged(listener: () -> Unit) {
        if (onDataChanged === listener) onDataChanged = null
    }

    fun resolve(context: Context, id: Int, slot: Slot): Binding =
        Instances.get(context, id) ?: Registry.defaultFor(slot).let { Binding(it.id, it.defaults) }

    /**
     * Draws widget [id] once per size the launcher may show it at (see
     * [WidgetSizes]), so the bitmap and the overlays positioned over it are
     * laid out for the box they actually fill in every orientation.
     */
    fun render(
        context: Context,
        id: Int,
        data: Data = Data(context),
        now: ZonedDateTime = ZonedDateTime.now(),
        slot: Slot = Slots.forWidget(AppWidgetManager.getInstance(context), id),
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val binding = resolve(context, id, slot)
        val design = Registry.design(binding.designId)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val sizes = WidgetSizes.of(manager.getAppWidgetOptions(id), slot.size, landscape)
        manager.updateAppWidget(id, views(context, design, binding.style, sizes, WidgetTap.pendingIntent(context, id), now, data))
        Instances.setLastKey(context, id, design.liveKey(SceneInputs(context, design, binding.style, data, now)))
        if (Signal.BATTERY in design.signals) {
            BatteryHistory.record(context, data.battery, now.toEpochSecond() / 60)
            if (!data.battery.charging) Work.armChargingTrigger(context)
        }
    }

    /**
     * One RemoteViews per size, sharing the host's bitmap budget equally;
     * the launcher picks the one matching the box it lays the widget out in.
     */
    fun views(
        context: Context,
        design: WidgetDesign,
        style: Style,
        sizes: List<SizeF>,
        click: PendingIntent?,
        now: ZonedDateTime,
        data: Data,
    ): RemoteViews {
        val budget = Renderer.budget(context) / sizes.size
        val one = { size: SizeF -> Renderer.build(context, design, style, size.width, size.height, click, now, data, budget) }
        return when {
            sizes.size == 1 -> one(sizes.single())
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> RemoteViews(sizes.associateWith(one))
            else -> error("WidgetSizes reported ${sizes.size} sizes before Android 12")
        }
    }

    /** Everything a reboot or app update loses or leaves stale: enabled slots, jobs, the ticker and every bitmap. */
    fun restore(context: Context) {
        Slots.syncEnabled(context)
        Work.ensureScheduled(context)
        renderAll(context)
    }

    fun renderAll(context: Context) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        try {
            eachPlaced(context) { id, slot -> render(context, id, data, now, slot) }
        } finally {
            onInstancesChanged(context)
            onDataChanged?.invoke()
        }
    }

    /** Redraws the widgets whose design depends on [signal]. */
    fun refresh(context: Context, signal: Signal) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        eachPlaced(context) { id, slot ->
            if (signal in Registry.design(resolve(context, id, slot).designId).signals) render(context, id, data, now, slot)
        }
        onDataChanged?.invoke()
    }

    /** Redraws only the widgets whose live key moved since their last render. */
    fun tick(context: Context) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        var batteryRecorded = false
        try {
            eachPlaced(context) { id, slot ->
                val binding = resolve(context, id, slot)
                val design = Registry.design(binding.designId)
                val key = design.liveKey(SceneInputs(context, design, binding.style, data, now))
                if (key != null && key != Instances.lastKey(context, id)) {
                    render(context, id, data, now, slot)
                } else if (Signal.BATTERY in design.signals && !batteryRecorded) {
                    BatteryHistory.record(context, data.battery, now.toEpochSecond() / 60)
                    batteryRecorded = true
                }
            }
        } finally {
            // The ticker is one-shot: it must be re-armed even when a pass fails.
            onInstancesChanged(context)
            onDataChanged?.invoke()
        }
    }

    /** Keeps the live ticker running exactly while some placed widget has a live key. */
    fun onInstancesChanged(context: Context) {
        val live = Slots.placed(context).any { (id, _) -> Instances.lastKey(context, id) != null }
        if (live) LiveTicker.schedule(context) else LiveTicker.cancel(context)
    }

    /** A widget was placed, removed or rebound: re-arm the ticker and keep the weather job only while it is needed. */
    fun onPlacementChanged(context: Context) {
        onInstancesChanged(context)
        Work.syncWeather(context)
    }

    /** Whether some placed widget shows the forecast. */
    fun showsWeather(context: Context): Boolean = Slots.placed(context).any { (id, slot) ->
        Signal.WEATHER in Registry.design(resolve(context, id, slot).designId).signals
    }

    /**
     * Runs [block] for every placed widget. A widget that fails to draw keeps
     * its last bitmap; the others in the same pass must still be drawn.
     */
    private fun eachPlaced(context: Context, block: (Int, Slot) -> Unit) {
        Slots.placed(context).forEach { (id, slot) ->
            try {
                block(id, slot)
            } catch (e: Exception) {
                Log.e(TAG, "Widget $id (${slot.category.id}, ${slot.size.id}) failed to draw", e)
            }
        }
    }
}
