package com.malickabdullah.tessera.designs.battery

import com.malickabdullah.tessera.engine.WidgetDesign

/** Battery designs in gallery order; the first that fits a slot is that slot's default. */
val batteryDesigns: List<WidgetDesign> = listOf(
    BatteryCell,
    BatteryRing,
    BatterySegments,
    BatteryNumeric,
    BatteryHistoryChart,
)
