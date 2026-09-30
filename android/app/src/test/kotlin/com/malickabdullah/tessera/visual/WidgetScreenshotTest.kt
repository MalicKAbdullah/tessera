package com.malickabdullah.tessera.visual

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.malickabdullah.tessera.engine.Category
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Locale
import java.util.TimeZone

/**
 * Golden screenshots of every design × size × theme × data state, drawn by
 * the real engine with Robolectric's native (Skia) graphics. `recordRoborazziDebug`
 * rewrites them, `verifyRoborazziDebug` fails on any visual change.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class WidgetScreenshotTest(private val category: String) {
    private lateinit var context: Context

    // Built inside the sandbox: Roborazzi's options touch Android classes.
    private val options by lazy {
        RoborazziOptions(
            // Goldens are stored at half the device pixels to keep the repository small.
            recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
            compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f),
        )
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ScreenshotEnv.apply(context)
    }

    @Test
    fun screenshots() {
        val cat = Category.entries.first { it.id == category }
        val failures = mutableListOf<String>()
        Shots.of(cat).forEach { shot ->
            val bitmap = Shots.render(context, shot, ScreenshotEnv.photoDir)
            try {
                bitmap.captureRoboImage("src/test/snapshots/$category/${shot.name}.png", options)
            } catch (e: AssertionError) {
                failures += "${shot.name}: ${e.message}"
            } finally {
                bitmap.recycle()
            }
        }
        val warnings = Shots.warnings.filter { it.startsWith("$category.") }
        assertTrue(warnings.joinToString("\n"), warnings.isEmpty())
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun categories(): List<Array<Any>> =
            listOf("clock", "battery", "calendar", "weather", "countdown", "note", "photo", "sky", "device").map { arrayOf(it) }
    }
}

/** Locale, zone and clock format every screenshot is taken in. */
object ScreenshotEnv {
    val photoDir = File(System.getProperty("java.io.tmpdir"), "tessera-test-photos")

    fun apply(context: Context) {
        Locale.setDefault(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(Fixed.zone))
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
    }
}
