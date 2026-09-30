package com.malickabdullah.tessera.engine

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.malickabdullah.tessera.widgets.BatteryLargeWidget
import com.malickabdullah.tessera.widgets.BatterySmallWidget
import com.malickabdullah.tessera.widgets.BatteryWidget
import com.malickabdullah.tessera.widgets.CalendarLargeWidget
import com.malickabdullah.tessera.widgets.CalendarSmallWidget
import com.malickabdullah.tessera.widgets.CalendarWidget
import com.malickabdullah.tessera.widgets.ClockLargeWidget
import com.malickabdullah.tessera.widgets.ClockSmallWidget
import com.malickabdullah.tessera.widgets.ClockWidget
import com.malickabdullah.tessera.widgets.CountdownLargeWidget
import com.malickabdullah.tessera.widgets.CountdownSmallWidget
import com.malickabdullah.tessera.widgets.CountdownWidget
import com.malickabdullah.tessera.widgets.DeviceLargeWidget
import com.malickabdullah.tessera.widgets.DeviceSmallWidget
import com.malickabdullah.tessera.widgets.DeviceWidget
import com.malickabdullah.tessera.widgets.NoteLargeWidget
import com.malickabdullah.tessera.widgets.NoteSmallWidget
import com.malickabdullah.tessera.widgets.NoteWidget
import com.malickabdullah.tessera.widgets.PhotoLargeWidget
import com.malickabdullah.tessera.widgets.PhotoSmallWidget
import com.malickabdullah.tessera.widgets.PhotoWidget
import com.malickabdullah.tessera.widgets.SkyLargeWidget
import com.malickabdullah.tessera.widgets.SkySmallWidget
import com.malickabdullah.tessera.widgets.SkyWidget
import com.malickabdullah.tessera.widgets.TesseraProvider
import com.malickabdullah.tessera.widgets.WeatherLargeWidget
import com.malickabdullah.tessera.widgets.WeatherSmallWidget
import com.malickabdullah.tessera.widgets.WeatherWidget

/** A launcher-visible widget entry: one category at one footprint. */
data class Slot(val category: Category, val size: SizeClass, val provider: Class<out TesseraProvider>) {
    fun component(context: Context) = ComponentName(context, provider)
}

/**
 * One provider per category and size, so the launcher's picker and the pin
 * flow both place a widget at the design's intended footprint. The WIDE
 * providers keep the v0.1 class names so widgets placed then keep working.
 * Every category has all three slots, so a category gains a size by adding a
 * design at it, with no provider or manifest change.
 */
object Slots {
    val all = listOf(
        Slot(Category.CLOCK, SizeClass.SMALL, ClockSmallWidget::class.java),
        Slot(Category.CLOCK, SizeClass.WIDE, ClockWidget::class.java),
        Slot(Category.CLOCK, SizeClass.LARGE, ClockLargeWidget::class.java),
        Slot(Category.BATTERY, SizeClass.SMALL, BatterySmallWidget::class.java),
        Slot(Category.BATTERY, SizeClass.WIDE, BatteryWidget::class.java),
        Slot(Category.BATTERY, SizeClass.LARGE, BatteryLargeWidget::class.java),
        Slot(Category.CALENDAR, SizeClass.SMALL, CalendarSmallWidget::class.java),
        Slot(Category.CALENDAR, SizeClass.WIDE, CalendarWidget::class.java),
        Slot(Category.CALENDAR, SizeClass.LARGE, CalendarLargeWidget::class.java),
        Slot(Category.WEATHER, SizeClass.SMALL, WeatherSmallWidget::class.java),
        Slot(Category.WEATHER, SizeClass.WIDE, WeatherWidget::class.java),
        Slot(Category.WEATHER, SizeClass.LARGE, WeatherLargeWidget::class.java),
        Slot(Category.COUNTDOWN, SizeClass.SMALL, CountdownSmallWidget::class.java),
        Slot(Category.COUNTDOWN, SizeClass.WIDE, CountdownWidget::class.java),
        Slot(Category.COUNTDOWN, SizeClass.LARGE, CountdownLargeWidget::class.java),
        Slot(Category.NOTE, SizeClass.SMALL, NoteSmallWidget::class.java),
        Slot(Category.NOTE, SizeClass.WIDE, NoteWidget::class.java),
        Slot(Category.NOTE, SizeClass.LARGE, NoteLargeWidget::class.java),
        Slot(Category.PHOTO, SizeClass.SMALL, PhotoSmallWidget::class.java),
        Slot(Category.PHOTO, SizeClass.WIDE, PhotoWidget::class.java),
        Slot(Category.PHOTO, SizeClass.LARGE, PhotoLargeWidget::class.java),
        Slot(Category.SKY, SizeClass.SMALL, SkySmallWidget::class.java),
        Slot(Category.SKY, SizeClass.WIDE, SkyWidget::class.java),
        Slot(Category.SKY, SizeClass.LARGE, SkyLargeWidget::class.java),
        Slot(Category.DEVICE, SizeClass.SMALL, DeviceSmallWidget::class.java),
        Slot(Category.DEVICE, SizeClass.WIDE, DeviceWidget::class.java),
        Slot(Category.DEVICE, SizeClass.LARGE, DeviceLargeWidget::class.java),
    )

    fun of(provider: ComponentName): Slot = all.first { it.provider.name == provider.className }

    fun of(category: Category, size: SizeClass): Slot = all.first { it.category == category && it.size == size }

    fun forWidget(manager: AppWidgetManager, id: Int): Slot = of(manager.getAppWidgetInfo(id).provider)

    /** Every placed widget id with its slot. */
    fun placed(context: Context): List<Pair<Int, Slot>> {
        val manager = AppWidgetManager.getInstance(context)
        return all.flatMap { slot -> manager.getAppWidgetIds(slot.component(context)).map { it to slot } }
    }

    /**
     * The launcher picker must never offer a slot no design fills, and the
     * manifest cannot know the registry, so receivers of slots that may be
     * empty ship `enabled="false"` and are switched here from [Registry] on
     * app start and package update.
     */
    fun syncEnabled(context: Context) {
        val pm = context.packageManager
        all.forEach { slot ->
            val state = if (Registry.designs.any { it.category == slot.category && slot.size in it.sizes }) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val component = slot.component(context)
            if (pm.getComponentEnabledSetting(component) != state) {
                pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
    }
}
