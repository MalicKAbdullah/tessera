package com.malickabdullah.tessera.designs.calendar

import com.malickabdullah.tessera.engine.WidgetDesign

/** Calendar designs in gallery order; the first that fits a slot is that slot's default. */
val calendarDesigns: List<WidgetDesign> = listOf(
    CalendarMonth,
    CalendarAgenda,
    CalendarNext,
    CalendarMatrix,
    CalendarWeek,
    CalendarYear,
    CalendarClassic,
)
