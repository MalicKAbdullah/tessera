package com.malickabdullah.tessera

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.util.SizeF
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextClock
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.malickabdullah.tessera.data.Data
import com.malickabdullah.tessera.engine.BgKind
import com.malickabdullah.tessera.engine.Engine
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.LiveTicker
import com.malickabdullah.tessera.engine.Registry
import com.malickabdullah.tessera.engine.SystemEventsReceiver
import com.malickabdullah.tessera.engine.Work
import com.malickabdullah.tessera.widgets.ClockSmallWidget
import com.malickabdullah.tessera.widgets.ClockWidget
import com.malickabdullah.tessera.widgets.WeatherWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZonedDateTime
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class EngineReliabilityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager get() = AppWidgetManager.getInstance(context)
    private val alarms get() = shadowOf(context.getSystemService(AlarmManager::class.java))

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    private var nextId = 1

    /** Places a widget the way a launcher does: bound to the provider, then sent APPWIDGET_UPDATE. */
    private fun place(provider: Class<out AppWidgetProvider>): Int {
        val id = nextId++
        shadowOf(manager).putWidgetInfo(id, AppWidgetProviderInfo().apply { this.provider = ComponentName(context, provider) })
        provider.getDeclaredConstructor().newInstance().onUpdate(context, manager, intArrayOf(id))
        return id
    }

    private fun uniqueWork(name: String): List<WorkInfo.State> =
        WorkManager.getInstance(context).getWorkInfosForUniqueWork(name).get().map { it.state }

    @Test
    fun oneWidgetFailingDoesNotStopTheOthersOrTheTicker() {
        val broken = place(ClockWidget::class.java)
        val healthy = place(ClockWidget::class.java)
        val minimal = Registry.design("clock.minimal")
        // A photo surface whose file is gone makes the renderer throw.
        val missingPhoto = minimal.defaults.copy(bg = minimal.defaults.bg.copy(kind = BgKind.PHOTO, photo = "/nonexistent.jpg"))
        Instances.put(context, broken, minimal.id, missingPhoto)
        Instances.put(context, healthy, minimal.id, minimal.defaults)
        Instances.setLastKey(context, broken, "stale")
        Instances.setLastKey(context, healthy, "stale")
        LiveTicker.cancel(context)

        Engine.renderAll(context)

        assertEquals("stale", Instances.lastKey(context, broken))
        assertNotEquals("stale", Instances.lastKey(context, healthy))
        assertNotNull("the live ticker must be re-armed", alarms.peekNextScheduledAlarm())
    }

    @Test
    fun aTickThatFailsStillReArmsTheTicker() {
        val broken = place(ClockWidget::class.java)
        val minimal = Registry.design("clock.minimal")
        Instances.put(context, broken, minimal.id, minimal.defaults.copy(bg = minimal.defaults.bg.copy(kind = BgKind.PHOTO, photo = "/nonexistent.jpg")))
        Instances.setLastKey(context, broken, "stale")
        LiveTicker.cancel(context)

        Engine.tick(context)

        assertNotNull(alarms.peekNextScheduledAlarm())
    }

    @Test
    fun bootAndAppUpdateReachTheRestoreReceiver() {
        listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED).forEach { action ->
            val receivers = context.packageManager.queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            assertTrue(action, receivers.any { it.activityInfo.name == SystemEventsReceiver::class.java.name })
        }
    }

    @Test
    fun restoreRedrawsEveryWidgetAndReArmsTickerAndJobs() {
        val id = place(ClockSmallWidget::class.java)
        Instances.put(context, id, "clock.minimal", Registry.design("clock.minimal").defaults)
        LiveTicker.cancel(context)
        assertNull(Instances.lastKey(context, id))

        Engine.restore(context)

        assertNotNull(Instances.lastKey(context, id))
        assertNotNull(alarms.peekNextScheduledAlarm())
        assertEquals(listOf(WorkInfo.State.ENQUEUED), uniqueWork("tessera.live"))
    }

    @Test
    fun theHourlyWeatherJobRunsOnlyWhileAWeatherWidgetIsPlaced() {
        place(ClockWidget::class.java)
        Work.ensureScheduled(context)
        assertTrue(uniqueWork("tessera.weather").none { it == WorkInfo.State.ENQUEUED })

        place(WeatherWidget::class.java)
        assertEquals(listOf(WorkInfo.State.ENQUEUED), uniqueWork("tessera.weather"))
    }

    @Test
    fun eachOrientationGetsABitmapAndClockLaidOutForItsOwnBox() {
        val design = Registry.design("clock.minimal")
        val portrait = SizeF(320f, 180f)
        val landscape = SizeF(560f, 110f)
        // The launcher picks one entry of the size map per box; each entry is built exactly like this.
        Engine.views(context, design, design.defaults, listOf(portrait, landscape), null, ZonedDateTime.now(), Data(context))
        val density = context.resources.displayMetrics.density
        listOf(portrait, landscape).forEach { size ->
            val views = Engine.views(context, design, design.defaults, listOf(size), null, ZonedDateTime.now(), Data(context))
            val root = views.apply(context, FrameLayout(context))
            val bitmap = (root.findViewById<ImageView>(R.id.bg).drawable as BitmapDrawable).bitmap
            val aspect = bitmap.width.toFloat() / bitmap.height
            assertTrue("$size drawn at aspect $aspect", abs(aspect - size.width / size.height) < 0.02f)
            val clock = findClock(root as FrameLayout)
            // The clock's insets are measured against this size, so its box stays inside it.
            assertTrue(clock.paddingLeft + clock.paddingRight < size.width * density)
            assertTrue(clock.paddingTop + clock.paddingBottom < size.height * density)
        }
    }

    private fun findClock(group: android.view.ViewGroup): TextClock {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is TextClock -> return child
                is android.view.ViewGroup -> runCatching { return findClock(child) }
            }
        }
        error("no TextClock")
    }
}
