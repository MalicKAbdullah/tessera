package com.malickabdullah.tessera

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import com.malickabdullah.tessera.data.WeatherFetch
import com.malickabdullah.tessera.engine.Engine
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.Slots
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Work
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

open class MainActivity : FlutterActivity() {
    private var channel: EngineChannel? = null
    private var lastBattery: String? = null

    /** While the app is open, battery changes redraw live widgets and previews immediately. */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val key = "${intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)}|${intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0)}|" +
                intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            if (key == lastBattery) return
            lastBattery = key
            Engine.tick(context)
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        channel = EngineChannel(this, flutterEngine.dartExecutor.binaryMessenger)
    }

    override fun cleanUpFlutterEngine(flutterEngine: FlutterEngine) {
        channel?.dispose()
        channel = null
        super.cleanUpFlutterEngine(flutterEngine)
    }

    override fun onResume() {
        super.onResume()
        Slots.syncEnabled(this)
        Work.ensureScheduled(this)
        if (WeatherFetch.isStale(this)) Work.fetchWeatherNow(this)
        Engine.renderAll(this)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        unregisterReceiver(batteryReceiver)
        lastBattery = null
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        channel?.pushLaunchTarget()
    }

    /** What the app should open on: a tapped widget's editor, or null for the gallery. */
    open fun launchTarget(): Map<String, Any>? {
        val id = intent.getIntExtra(Engine.EXTRA_INSTANCE, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return null
        intent.removeExtra(Engine.EXTRA_INSTANCE)
        return mapOf("mode" to "edit", "id" to id)
    }
}

/**
 * The launcher's configure step (and "reconfigure" on Android 12+). Backing
 * out leaves RESULT_CANCELED, which tells the launcher not to place the widget.
 */
class ConfigureActivity : MainActivity() {
    private val widgetId by lazy {
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) { "Configure launched without a widget id" }
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
    }

    override fun launchTarget(): Map<String, Any> = mapOf("mode" to "configure", "id" to widgetId)

    fun complete(designId: String, style: Style) {
        Instances.put(this, widgetId, designId, style)
        Engine.render(this, widgetId)
        Engine.onInstancesChanged(this)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
