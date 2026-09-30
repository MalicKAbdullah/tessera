package com.malickabdullah.tessera.designs.device

import com.malickabdullah.tessera.engine.WidgetDesign

/** Device designs in gallery order; the first that fits a slot is that slot's default. */
val deviceDesigns: List<WidgetDesign> = listOf(
    DeviceSystem,
    DeviceStorage,
    DeviceMemory,
    DeviceNetwork,
    DeviceUptime,
)
