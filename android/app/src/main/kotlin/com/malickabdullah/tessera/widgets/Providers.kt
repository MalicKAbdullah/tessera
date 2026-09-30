package com.malickabdullah.tessera.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.malickabdullah.tessera.engine.Engine
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.Work

/**
 * Every provider hands straight to the [Engine]; which design a widget shows
 * is its per-id binding, not its provider class. Classes live in this
 * package because v0.1 widgets are registered under these names.
 */
abstract class TesseraProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { Engine.render(context, it) }
        Engine.onInstancesChanged(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        Engine.render(context, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { Instances.remove(context, it) }
        Engine.onInstancesChanged(context)
    }

    override fun onEnabled(context: Context) {
        Work.ensureScheduled(context)
    }
}

class ClockSmallWidget : TesseraProvider()
class ClockWidget : TesseraProvider()
class ClockLargeWidget : TesseraProvider()
class BatterySmallWidget : TesseraProvider()
class BatteryWidget : TesseraProvider()
class BatteryLargeWidget : TesseraProvider()
class CalendarSmallWidget : TesseraProvider()
class CalendarWidget : TesseraProvider()
class CalendarLargeWidget : TesseraProvider()
class WeatherSmallWidget : TesseraProvider()
class WeatherWidget : TesseraProvider()
class WeatherLargeWidget : TesseraProvider()
class CountdownSmallWidget : TesseraProvider()
class CountdownWidget : TesseraProvider()
class CountdownLargeWidget : TesseraProvider()
class NoteSmallWidget : TesseraProvider()
class NoteWidget : TesseraProvider()
class NoteLargeWidget : TesseraProvider()
class PhotoSmallWidget : TesseraProvider()
class PhotoWidget : TesseraProvider()
class PhotoLargeWidget : TesseraProvider()
