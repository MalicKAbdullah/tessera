package com.malickabdullah.tessera

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.malickabdullah.tessera.data.ContentSource
import com.malickabdullah.tessera.data.PhotoStore
import com.malickabdullah.tessera.data.WeatherFetch
import com.malickabdullah.tessera.engine.Engine
import com.malickabdullah.tessera.engine.Fonts
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.Registry
import com.malickabdullah.tessera.engine.Renderer
import com.malickabdullah.tessera.engine.Signal
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Slots
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.Work
import com.malickabdullah.tessera.engine.PinReceiver
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/** The app's only bridge to the widget engine (Dart side: lib/src/features/widgets/services/engine.dart). */
class EngineChannel(private val activity: MainActivity, messenger: BinaryMessenger) : MethodChannel.MethodCallHandler {
    private val channel = MethodChannel(messenger, "tessera/engine")
    private val main = Handler(Looper.getMainLooper())
    private val encoder = Executors.newSingleThreadExecutor()
    private val photoImport = Executors.newSingleThreadExecutor()

    init {
        channel.setMethodCallHandler(this)
        Engine.onDataChanged = { main.post { channel.invokeMethod("dataChanged", null) } }
    }

    fun dispose() {
        channel.setMethodCallHandler(null)
        Engine.onDataChanged = null
        encoder.shutdown()
        photoImport.shutdown()
    }

    fun pushLaunchTarget() = channel.invokeMethod("launchTarget", activity.launchTarget())

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "catalog" -> result.success(Registry.catalog().toString())
            "render" -> renderPreview(call, result)
            "specimen" -> specimen(call, result)
            "placed" -> result.success(placed().toString())
            "bind" -> {
                val id = call.argument<Int>("id")!!
                Instances.put(activity, id, call.argument<String>("design")!!, style(call))
                Engine.render(activity, id)
                Engine.onInstancesChanged(activity)
                result.success(null)
            }
            "canPin" -> result.success(AppWidgetManager.getInstance(activity).isRequestPinAppWidgetSupported)
            "pin" -> result.success(pin(call))
            "setContent" -> {
                ContentSource.write(activity, call.argument<String>("json")!!)
                if (WeatherFetch.isStale(activity)) Work.fetchWeatherNow(activity)
                Engine.refresh(activity, Signal.CONTENT)
                result.success(null)
            }
            "refreshWeather" -> {
                Work.fetchWeatherNow(activity)
                result.success(null)
            }
            "photos" -> result.success(PhotoStore.read(activity).toJson().toString())
            "pickPhotos" -> pickPhotos(result)
            "removePhoto" -> {
                val album = PhotoStore.remove(activity, call.argument<String>("id")!!)
                Engine.refresh(activity, Signal.CONTENT)
                result.success(album.toJson().toString())
            }
            "setPhotoCaption" -> {
                val album = PhotoStore.setCaption(activity, call.argument<String>("caption")!!)
                Engine.refresh(activity, Signal.CONTENT)
                result.success(album.toJson().toString())
            }
            "launchTarget" -> result.success(activity.launchTarget())
            "finishConfigure" -> {
                val configure = activity as ConfigureActivity
                configure.complete(call.argument<String>("design")!!, style(call))
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    /**
     * Opens the system photo picker for the album's free places, then
     * imports the picks off the main thread. Replies with the album JSON;
     * a cancelled pick replies with the album unchanged.
     */
    private fun pickPhotos(result: MethodChannel.Result) {
        val free = PhotoStore.MAX - PhotoStore.read(activity).photos.size
        require(free > 0) { "The album is full" }
        activity.pickPhotos(free) { uris ->
            if (uris.isEmpty()) {
                result.success(PhotoStore.read(activity).toJson().toString())
                return@pickPhotos
            }
            photoImport.execute {
                try {
                    val album = PhotoStore.import(activity, uris)
                    Engine.refresh(activity, Signal.CONTENT)
                    main.post { result.success(album.toJson().toString()) }
                } catch (e: Exception) {
                    main.post { result.error("import", e.message, null) }
                }
            }
        }
    }

    private fun style(call: MethodCall) = Style.parse(JSONObject(call.argument<String>("style")!!))

    private fun placed(): JSONArray = JSONArray(
        Slots.placed(activity).map { (id, slot) ->
            val bound = Instances.get(activity, id)
            val binding = Engine.resolve(activity, id, slot)
            JSONObject()
                .put("id", id)
                .put("category", slot.category.id)
                .put("size", slot.size.id)
                .put("bound", bound != null)
                .put("design", binding.designId)
                .put("style", binding.style.toJson())
        },
    )

    private fun pin(call: MethodCall): Boolean {
        val manager = AppWidgetManager.getInstance(activity)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val design = Registry.design(call.argument<String>("design")!!)
        val size = SizeClass.entries.first { it.id == call.argument<String>("size")!! }
        val style = style(call)
        val slot = Slots.of(design.category, size)
        // The launcher appends EXTRA_APPWIDGET_ID to this intent, so it must be mutable.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val callback = PendingIntent.getBroadcast(
            activity,
            System.currentTimeMillis().toInt(),
            Intent(activity, PinReceiver::class.java)
                .putExtra(PinReceiver.EXTRA_DESIGN, design.id)
                .putExtra(PinReceiver.EXTRA_STYLE, style.toJson().toString()),
            mutable or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val preview = Renderer.build(activity, design, style, size.widthDp.toFloat(), size.heightDp.toFloat(), null)
        val extras = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
        return manager.requestPinAppWidget(slot.component(activity), extras, callback)
    }

    /**
     * Inflates the exact RemoteViews the launcher would get and draws the
     * real view tree, so TextClocks, AnalogClock hands and fonts in the
     * preview are the home-screen pixels. The tree is attached off-screen
     * because TextClock and AnalogClock only read the time once attached.
     */
    private fun renderPreview(call: MethodCall, result: MethodChannel.Result) {
        val design = Registry.design(call.argument<String>("design")!!)
        val wDp = call.argument<Double>("widthDp")!!.toFloat()
        val hDp = call.argument<Double>("heightDp")!!.toFloat()
        val views = Renderer.build(activity, design, style(call), wDp, hDp, null)
        val density = activity.resources.displayMetrics.density
        val wPx = (wDp * density).roundToInt()
        val hPx = (hDp * density).roundToInt()
        val host = FrameLayout(activity).apply {
            translationX = -4f * (wPx + activity.resources.displayMetrics.widthPixels)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
        val decor = activity.window.decorView as ViewGroup
        decor.addView(host, FrameLayout.LayoutParams(wPx, hPx))
        val view = views.apply(activity, host)
        host.addView(view, FrameLayout.LayoutParams(wPx, hPx))
        view.measure(View.MeasureSpec.makeMeasureSpec(wPx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(hPx, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, wPx, hPx)
        val bitmap = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        decor.removeView(host)
        encode(bitmap, result)
    }

    private fun specimen(call: MethodCall, result: MethodChannel.Result) {
        val density = activity.resources.displayMetrics.density
        val size = call.argument<Double>("size")!!.toFloat()
        val text = call.argument<String>("text")!!
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Fonts.typeface(activity, call.argument<String>("font")!!, call.argument<Int>("weight")!!)
            textSize = size * density
            color = call.argument<Number>("color")!!.toLong().toInt()
        }
        val fm = paint.fontMetrics
        val bitmap = Bitmap.createBitmap(
            paint.measureText(text).roundToInt().coerceAtLeast(1) + 2,
            (fm.descent - fm.ascent).roundToInt() + 2,
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).drawText(text, 1f, 1f - fm.ascent, paint)
        encode(bitmap, result)
    }

    private fun encode(bitmap: Bitmap, result: MethodChannel.Result) {
        encoder.execute {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            main.post { result.success(out.toByteArray()) }
        }
    }
}
