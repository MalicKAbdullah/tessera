package com.malickabdullah.tessera.engine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.malickabdullah.tessera.MainActivity
import com.malickabdullah.tessera.data.BatteryHistory
import com.malickabdullah.tessera.data.Data
import java.time.ZonedDateTime

/** Resolves what each placed widget shows and pushes fresh RemoteViews to the launcher. */
object Engine {
    const val EXTRA_INSTANCE = "tessera.instance"

    /** Set while the app is open so previews redraw when live data changes. */
    @Volatile
    var onDataChanged: (() -> Unit)? = null

    fun resolve(context: Context, id: Int, slot: Slot): Binding =
        Instances.get(context, id) ?: Registry.defaultFor(slot).let { Binding(it.id, it.defaults) }

    fun render(context: Context, id: Int, data: Data = Data(context), now: ZonedDateTime = ZonedDateTime.now()) {
        val manager = AppWidgetManager.getInstance(context)
        val slot = Slots.forWidget(manager, id)
        val binding = resolve(context, id, slot)
        val design = Registry.design(binding.designId)
        val (w, h) = sizeDp(manager, id, slot)
        manager.updateAppWidget(id, Renderer.build(context, design, binding.style, w, h, openIntent(context, id), now, data))
        Instances.setLastKey(context, id, design.liveKey(SceneInputs(context, design, binding.style, data, now)))
        if (Signal.BATTERY in design.signals) {
            BatteryHistory.record(context, data.battery, now.toEpochSecond() / 60)
            if (!data.battery.charging) Work.armChargingTrigger(context)
        }
    }

    fun renderAll(context: Context) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        Slots.placed(context).forEach { (id, _) -> render(context, id, data, now) }
        onInstancesChanged(context)
        onDataChanged?.invoke()
    }

    /** Redraws the widgets whose design depends on [signal]. */
    fun refresh(context: Context, signal: Signal) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        Slots.placed(context).forEach { (id, slot) ->
            if (signal in Registry.design(resolve(context, id, slot).designId).signals) render(context, id, data, now)
        }
        onDataChanged?.invoke()
    }

    /** Redraws only the widgets whose live key moved since their last render. */
    fun tick(context: Context) {
        val data = Data(context)
        val now = ZonedDateTime.now()
        var batteryRecorded = false
        Slots.placed(context).forEach { (id, slot) ->
            val binding = resolve(context, id, slot)
            val design = Registry.design(binding.designId)
            val key = design.liveKey(SceneInputs(context, design, binding.style, data, now))
            if (key != null && key != Instances.lastKey(context, id)) {
                render(context, id, data, now)
            } else if (Signal.BATTERY in design.signals && !batteryRecorded) {
                BatteryHistory.record(context, data.battery, now.toEpochSecond() / 60)
                batteryRecorded = true
            }
        }
        onInstancesChanged(context)
        onDataChanged?.invoke()
    }

    /** Keeps the live ticker running exactly while some placed widget has a live key. */
    fun onInstancesChanged(context: Context) {
        val live = Slots.placed(context).any { (id, _) -> Instances.lastKey(context, id) != null }
        if (live) LiveTicker.schedule(context) else LiveTicker.cancel(context)
    }

    /**
     * Portrait size in dp as the launcher reports it. Right after placement
     * some launchers have not reported yet (zeros); the slot's nominal size
     * stands in until onAppWidgetOptionsChanged delivers the real one.
     */
    private fun sizeDp(manager: AppWidgetManager, id: Int, slot: Slot): Pair<Float, Float> {
        val options = manager.getAppWidgetOptions(id)
        val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (w > 0 && h > 0) w.toFloat() to h.toFloat() else slot.size.widthDp.toFloat() to slot.size.heightDp.toFloat()
    }

    private fun openIntent(context: Context, id: Int): PendingIntent = PendingIntent.getActivity(
        context,
        id,
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_INSTANCE, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
