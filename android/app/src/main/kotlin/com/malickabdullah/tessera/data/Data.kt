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

/**
 * Per-render view of every source, each read at most once and only if a design asks.
 * Open so the screenshot tests can substitute fixed data for every source.
 */
open class Data(private val context: Context) {
    open val battery: BatteryState by lazy { BatterySource.read(context) }
    open val batteryHistory: List<BatterySample> by lazy { BatteryHistory.read(context) }
    open val weather: WeatherState? by lazy { WeatherSource.read(context) }
    open val nextAlarm: NextAlarm? by lazy { AlarmSource.read(context) }
    open val content: Content by lazy { ContentSource.read(context) }
    open val photos: PhotoAlbum by lazy { PhotoStore.read(context) }
    open val calendar: CalendarState by lazy { CalendarSource.read(context) }
    open val skyPlace: SkyPlace? by lazy { SkyPlaceSource.read(context) }
    open val storage: StorageState by lazy { StorageSource.read(context) }
    open val memory: MemoryState by lazy { MemorySource.read(context) }
    open val network: NetworkState by lazy { NetworkSource.read(context) }
    open val uptimeMs: Long by lazy { UptimeSource.read(context) }
}
