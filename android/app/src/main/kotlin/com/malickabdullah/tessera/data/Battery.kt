package com.malickabdullah.tessera.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

enum class PlugType(val label: String) { NONE("On battery"), AC("AC"), USB("USB"), WIRELESS("Wireless"), DOCK("Dock") }

data class BatteryState(
    val level: Int,
    val charging: Boolean,
    val full: Boolean,
    val plug: PlugType,
    val temperatureC: Float,
    val voltageMv: Int,
    val health: String,
    /** Platform estimate (API 28+); null when unknown or not charging. */
    val chargeTimeRemainingMs: Long?,
)

/**
 * ACTION_BATTERY_CHANGED is sticky: registering a null receiver returns the
 * latest broadcast synchronously without subscribing, so every render reads
 * the true current state instead of a value cached at the last push.
 */
object BatterySource : DataSource<BatteryState> {
    override fun read(context: Context): BatteryState {
        val intent = context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: error("ACTION_BATTERY_CHANGED is a sticky system broadcast and is always present")
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val plug = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            PLUGGED_DOCK -> PlugType.DOCK
            else -> PlugType.NONE
        }
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val remaining = if (charging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val manager = context.getSystemService(BatteryManager::class.java)
            manager.computeChargeTimeRemaining().takeIf { it > 0 }
        } else {
            null
        }
        return BatteryState(
            level = level,
            charging = charging,
            full = status == BatteryManager.BATTERY_STATUS_FULL || (plug != PlugType.NONE && level >= 100),
            plug = plug,
            temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Hot"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-volt"
                else -> "Unknown"
            },
            chargeTimeRemainingMs = remaining,
        )
    }

    /** BatteryManager.BATTERY_PLUGGED_DOCK, public only from API 33. */
    private const val PLUGGED_DOCK = 8
}

data class BatterySample(val epochMinute: Long, val level: Int, val charging: Boolean)

/**
 * The last 24 hours of battery levels, sampled whenever the engine renders
 * or ticks. Stored compactly as "minute:level:c" entries.
 */
object BatteryHistory : DataSource<List<BatterySample>> {
    private const val PREFS = "tessera.battery"
    private const val KEY = "samples"
    private const val WINDOW_MINUTES = 24 * 60
    private const val MIN_GAP_MINUTES = 5

    override fun read(context: Context): List<BatterySample> =
        prefs(context).getString(KEY, "").orEmpty().split(',').filter { it.isNotEmpty() }.map {
            val (m, l, c) = it.split(':')
            BatterySample(m.toLong(), l.toInt(), c == "1")
        }

    fun record(context: Context, state: BatteryState, nowMinute: Long) {
        val samples = read(context)
        val last = samples.lastOrNull()
        val changed = last == null || last.level != state.level || last.charging != state.charging
        if (!changed && nowMinute - last!!.epochMinute < MIN_GAP_MINUTES) return
        val kept = samples.filter { nowMinute - it.epochMinute <= WINDOW_MINUTES } +
            BatterySample(nowMinute, state.level, state.charging)
        prefs(context).edit()
            .putString(KEY, kept.joinToString(",") { "${it.epochMinute}:${it.level}:${if (it.charging) 1 else 0}" })
            .apply()
    }

    /** Percent per hour over the most recent unplugged stretch; negative while draining. */
    fun drainRate(samples: List<BatterySample>): Float? {
        val stretch = samples.takeLastWhile { !it.charging }
        if (stretch.size < 2) return null
        val minutes = stretch.last().epochMinute - stretch.first().epochMinute
        if (minutes < 20) return null
        return (stretch.last().level - stretch.first().level) * 60f / minutes
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
