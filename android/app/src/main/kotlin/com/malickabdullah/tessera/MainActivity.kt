package com.malickabdullah.tessera

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import com.malickabdullah.tessera.data.WeatherFetch
import com.malickabdullah.tessera.engine.Engine
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.Slots
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Work
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

open class MainActivity : FlutterActivity() {
    private companion object {
        const val REQUEST_PHOTOS = 0x7E55
    }

    private var channel: EngineChannel? = null
    private var lastBattery: String? = null
    private var onPhotosPicked: ((List<Uri>) -> Unit)? = null

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

    /**
     * The system photo picker (Android 11+ with the SDK extension, 13+
     * natively) or, before it, the document picker: both grant read access
     * to the chosen images only, so no storage permission is requested.
     */
    fun pickPhotos(max: Int, onPicked: (List<Uri>) -> Unit) {
        onPhotosPicked = onPicked
        val photoPicker = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2)
        val intent = if (photoPicker) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = "image/*"
                if (max > 1) putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, minOf(max, MediaStore.getPickImagesMaxLimit()))
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, max > 1)
            }
        }
        startActivityForResult(intent, REQUEST_PHOTOS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PHOTOS) return
        // A process recreated while the picker was open has no caller left to answer.
        val picked = onPhotosPicked ?: return
        onPhotosPicked = null
        val uris = when {
            resultCode != RESULT_OK || data == null -> emptyList()
            data.clipData != null -> data.clipData!!.let { clip -> (0 until clip.itemCount).map { clip.getItemAt(it).uri } }
            else -> listOfNotNull(data.data)
        }
        picked(uris)
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
