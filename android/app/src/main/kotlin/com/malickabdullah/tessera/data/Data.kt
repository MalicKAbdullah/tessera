package com.malickabdullah.tessera.data

import android.content.Context

/**
 * A source of live data a design can draw. Sources read at render time so a
 * bitmap is never older than the render that produced it; sources that need
 * the network (weather) read a cache that a worker keeps fresh.
 */
interface DataSource<T> {
    fun read(context: Context): T
}

/** Per-render view of every source, each read at most once and only if a design asks. */
class Data(private val context: Context) {
    val battery: BatteryState by lazy { BatterySource.read(context) }
    val batteryHistory: List<BatterySample> by lazy { BatteryHistory.read(context) }
    val weather: WeatherState? by lazy { WeatherSource.read(context) }
    val nextAlarm: NextAlarm? by lazy { AlarmSource.read(context) }
    val content: Content by lazy { ContentSource.read(context) }
    val photos: PhotoAlbum by lazy { PhotoStore.read(context) }
    val calendar: CalendarState by lazy { CalendarSource.read(context) }
    val skyPlace: SkyPlace? by lazy { SkyPlaceSource.read(context) }
    val storage: StorageState by lazy { StorageSource.read(context) }
    val memory: MemoryState by lazy { MemorySource.read(context) }
    val network: NetworkState by lazy { NetworkSource.read(context) }
    val uptimeMs: Long by lazy { UptimeSource.read(context) }
}
