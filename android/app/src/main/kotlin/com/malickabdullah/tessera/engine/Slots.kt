package com.malickabdullah.tessera.engine

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.malickabdullah.tessera.widgets.BatteryLargeWidget
import com.malickabdullah.tessera.widgets.BatterySmallWidget
import com.malickabdullah.tessera.widgets.BatteryWidget
import com.malickabdullah.tessera.widgets.CalendarWidget
import com.malickabdullah.tessera.widgets.ClockLargeWidget
import com.malickabdullah.tessera.widgets.ClockSmallWidget
import com.malickabdullah.tessera.widgets.ClockWidget
import com.malickabdullah.tessera.widgets.CountdownWidget
import com.malickabdullah.tessera.widgets.NoteWidget
import com.malickabdullah.tessera.widgets.TesseraProvider
import com.malickabdullah.tessera.widgets.WeatherWidget

/** A launcher-visible widget entry: one category at one footprint. */
data class Slot(val category: Category, val size: SizeClass, val provider: Class<out TesseraProvider>) {
    fun component(context: Context) = ComponentName(context, provider)
}

/**
 * One provider per category and size, so the launcher's picker and the pin
 * flow both place a widget at the design's intended footprint. The WIDE
 * providers keep the v0.1 class names so widgets placed then keep working.
 */
object Slots {
    val all = listOf(
        Slot(Category.CLOCK, SizeClass.SMALL, ClockSmallWidget::class.java),
        Slot(Category.CLOCK, SizeClass.WIDE, ClockWidget::class.java),
        Slot(Category.CLOCK, SizeClass.LARGE, ClockLargeWidget::class.java),
        Slot(Category.BATTERY, SizeClass.SMALL, BatterySmallWidget::class.java),
        Slot(Category.BATTERY, SizeClass.WIDE, BatteryWidget::class.java),
        Slot(Category.BATTERY, SizeClass.LARGE, BatteryLargeWidget::class.java),
        Slot(Category.CALENDAR, SizeClass.WIDE, CalendarWidget::class.java),
        Slot(Category.WEATHER, SizeClass.WIDE, WeatherWidget::class.java),
        Slot(Category.COUNTDOWN, SizeClass.WIDE, CountdownWidget::class.java),
        Slot(Category.NOTE, SizeClass.WIDE, NoteWidget::class.java),
    )

    fun of(provider: ComponentName): Slot = all.first { it.provider.name == provider.className }

    fun of(category: Category, size: SizeClass): Slot = all.first { it.category == category && it.size == size }

    fun forWidget(manager: AppWidgetManager, id: Int): Slot = of(manager.getAppWidgetInfo(id).provider)

    /** Every placed widget id with its slot. */
    fun placed(context: Context): List<Pair<Int, Slot>> {
        val manager = AppWidgetManager.getInstance(context)
        return all.flatMap { slot -> manager.getAppWidgetIds(slot.component(context)).map { it to slot } }
    }
}
