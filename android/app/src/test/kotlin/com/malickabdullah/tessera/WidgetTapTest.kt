package com.malickabdullah.tessera

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.malickabdullah.tessera.engine.WidgetTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetTapTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theTapOpensMainActivityExplicitlyWithTheWidgetId() {
        val tap = shadowOf(WidgetTap.pendingIntent(context, 42))
        assertTrue(tap.isActivityIntent)
        assertEquals(42, tap.requestCode)
        assertTrue(tap.flags and PendingIntent.FLAG_IMMUTABLE != 0)
        val intent = tap.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component!!.className)
        assertEquals(context.packageName, intent.component!!.packageName)
        assertEquals(42, intent.getIntExtra(WidgetTap.EXTRA_WIDGET_ID, -1))
        assertEquals(42, WidgetTap.widgetId(intent))
    }

    @Test
    fun aRunningAppIsReusedRatherThanStackedAgain() {
        val flags = WidgetTap.intent(context, 7).flags
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertTrue(flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun eachWidgetHasItsOwnPendingIntent() {
        val a = WidgetTap.pendingIntent(context, 3)
        val b = WidgetTap.pendingIntent(context, 4)
        assertNotEquals(a, b)
        assertEquals(4, shadowOf(b).savedIntent.getIntExtra(WidgetTap.EXTRA_WIDGET_ID, -1))
        assertEquals(3, shadowOf(a).savedIntent.getIntExtra(WidgetTap.EXTRA_WIDGET_ID, -1))
        assertFalse(WidgetTap.intent(context, 3).filterEquals(WidgetTap.intent(context, 4)))
    }

    @Test
    fun aLauncherIconStartIsNotAWidgetTap() {
        assertNull(WidgetTap.widgetId(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN)))
    }
}
