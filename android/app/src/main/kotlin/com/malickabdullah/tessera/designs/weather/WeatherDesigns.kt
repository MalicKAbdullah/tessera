package com.malickabdullah.tessera.designs.weather

import com.malickabdullah.tessera.engine.WidgetDesign

/** Weather designs in gallery order; the first that fits a slot is that slot's default. */
val weatherDesigns: List<WidgetDesign> = listOf(
    WeatherNow,
    WeatherHourly,
    WeatherWeek,
    WeatherSky,
    WeatherMatrix,
    WeatherSun,
    WeatherGauges,
    WeatherClassic,
)
