package com.malickabdullah.tessera.designs.battery

import com.malickabdullah.tessera.data.BatteryHistory
import com.malickabdullah.tessera.data.PlugType
import com.malickabdullah.tessera.designs.Kit
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs

internal object BatteryKit {
    const val LOW = 20
    private const val WARNING = 0xFFFF5A4F.toInt()

    fun key(s: SceneInputs): String {
        val b = s.data.battery
        return "${b.level}|${b.charging}|${b.full}|${b.plug}|${b.chargeTimeRemainingMs?.div(600_000)}"
    }

    /** Accent, or a warning red when low and unplugged. */
    fun levelColor(s: Scene): Int {
        val b = s.data.battery
        return if (b.level <= LOW && b.plug == PlugType.NONE) WARNING else s.accent
    }

    fun status(s: Scene): String {
        val b = s.data.battery
        return when {
            b.full -> "Charged"
            b.charging -> "Charging · ${b.plug.label}"
            b.plug != PlugType.NONE -> "Plugged in · ${b.plug.label}"
            else -> "On battery"
        }
    }

    /** One line that always says something useful: time to full, time left, or temperature. */
    fun estimate(s: Scene): String {
        val b = s.data.battery
        b.chargeTimeRemainingMs?.let { return "Full in ${Kit.duration(it)}" }
        if (b.charging) return "Charging"
        val rate = BatteryHistory.drainRate(s.data.batteryHistory)
        if (rate != null && rate < -0.2f) return "≈ ${Kit.duration((b.level / -rate * 3_600_000).toLong())} left"
        return "%.1f°C".format(b.temperatureC)
    }
}
