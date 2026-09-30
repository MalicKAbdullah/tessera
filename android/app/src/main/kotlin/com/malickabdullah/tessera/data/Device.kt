package com.malickabdullah.tessera.data

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock

data class StorageState(val totalBytes: Long, val freeBytes: Long) {
    val usedBytes: Long get() = totalBytes - freeBytes
    val usedFraction: Float get() = usedBytes.toFloat() / totalBytes
}

data class MemoryState(val totalBytes: Long, val availableBytes: Long, val lowMemory: Boolean, val thresholdBytes: Long) {
    val usedFraction: Float get() = (totalBytes - availableBytes).toFloat() / totalBytes
}

enum class Transport(val label: String) { WIFI("Wi-Fi"), CELLULAR("Mobile"), ETHERNET("Ethernet"), VPN("VPN"), OTHER("Other"), NONE("Offline") }

data class NetworkState(
    val transport: Transport,
    /** Wi-Fi RSSI in dBm from the network's capabilities (API 29+); null when not on Wi-Fi or unreported. */
    val wifiRssi: Int?,
    val validated: Boolean,
    val metered: Boolean,
    /** The platform's bandwidth estimates, in kbit/s. */
    val downKbps: Int,
    val upKbps: Int,
) {
    /** 0–4 bars, the same thresholds the status bar uses. */
    val wifiBars: Int?
        get() = wifiRssi?.let { r ->
            when {
                r >= -55 -> 4
                r >= -66 -> 3
                r >= -77 -> 2
                r >= -88 -> 1
                else -> 0
            }
        }
}

/** Internal storage as the user sees it in Settings: the data partition. */
object StorageSource : DataSource<StorageState> {
    override fun read(context: Context): StorageState {
        val fs = StatFs(Environment.getDataDirectory().path)
        return StorageState(fs.totalBytes, fs.availableBytes)
    }
}

object MemorySource : DataSource<MemoryState> {
    override fun read(context: Context): MemoryState {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        return MemoryState(info.totalMem, info.availMem, info.lowMemory, info.threshold)
    }
}

/** Needs only ACCESS_NETWORK_STATE (a normal, install-time permission); no SSID, no location. */
object NetworkSource : DataSource<NetworkState> {
    override fun read(context: Context): NetworkState {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
            ?: return NetworkState(Transport.NONE, null, validated = false, metered = false, downKbps = 0, upKbps = 0)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> Transport.VPN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Transport.ETHERNET
            else -> Transport.OTHER
        }
        val rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            caps.signalStrength.takeIf { it != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED }
        } else {
            null
        }
        return NetworkState(
            transport = transport,
            wifiRssi = rssi,
            validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            downKbps = caps.linkDownstreamBandwidthKbps,
            upKbps = caps.linkUpstreamBandwidthKbps,
        )
    }
}

/** Time since boot, from the monotonic clock that keeps counting in deep sleep. */
object UptimeSource : DataSource<Long> {
    override fun read(context: Context): Long = SystemClock.elapsedRealtime()
}
