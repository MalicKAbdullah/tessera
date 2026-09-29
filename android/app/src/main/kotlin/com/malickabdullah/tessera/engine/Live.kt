package com.malickabdullah.tessera.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.malickabdullah.tessera.data.WeatherFetch
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A non-wakeup (RTC) alarm about a minute out. It never wakes the device:
 * while the screen is off it waits, and it fires as soon as the device wakes,
 * so live widgets (battery level, word clock) are current moments after the
 * screen turns on — the manifest-free substitute for ACTION_SCREEN_ON.
 */
object LiveTicker {
    private const val INTERVAL_MS = 60_000L

    private fun intent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TickReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC, System.currentTimeMillis() + INTERVAL_MS, intent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(intent(context))
    }
}

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Engine.tick(context)
    }
}

/** System events after which bitmap-drawn dates, zones and labels are stale. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Work.ensureScheduled(context)
        Engine.renderAll(context)
    }
}

/** Binds a widget placed through requestPinAppWidget to the design and style chosen in the app. */
class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        check(id != AppWidgetManager.INVALID_APPWIDGET_ID) { "Pin callback without a widget id" }
        Instances.put(
            context,
            id,
            checkNotNull(intent.getStringExtra(EXTRA_DESIGN)),
            Style.parse(JSONObject(checkNotNull(intent.getStringExtra(EXTRA_STYLE)))),
        )
        Engine.render(context, id)
        Engine.onInstancesChanged(context)
        Engine.onDataChanged?.invoke()
    }

    companion object {
        const val EXTRA_DESIGN = "tessera.design"
        const val EXTRA_STYLE = "tessera.style"
    }
}

object Work {
    /** Unique name of the v0.1 Flutter workmanager job, whose worker class no longer exists. */
    private const val LEGACY_REFRESH = "tessera.refresh"
    private const val LIVE = "tessera.live"
    private const val WEATHER = "tessera.weather"
    private const val WEATHER_NOW = "tessera.weather.now"
    private const val CHARGING = "tessera.charging"

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun ensureScheduled(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_REFRESH)
        wm.enqueueUniquePeriodicWork(
            LIVE,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build(),
        )
        wm.enqueueUniquePeriodicWork(
            WEATHER,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeatherWorker>(60, TimeUnit.MINUTES).setConstraints(online).build(),
        )
    }

    fun fetchWeatherNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WEATHER_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WeatherWorker>().setConstraints(online).build(),
        )
    }

    /**
     * POWER_CONNECTED is not delivered to manifest receivers since Android 8,
     * but a job constrained on charging starts as soon as power arrives, so
     * battery widgets flip to their charging look without waiting for a tick.
     */
    fun armChargingTrigger(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            CHARGING,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                .build(),
        )
    }
}

class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Engine.tick(applicationContext)
        return Result.success()
    }
}

class WeatherWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = try {
        if (WeatherFetch.fetch(applicationContext)) Engine.refresh(applicationContext, Signal.WEATHER)
        Result.success()
    } catch (e: IOException) {
        Result.retry()
    }
}
