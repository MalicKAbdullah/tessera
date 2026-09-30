package com.malickabdullah.tessera.designs.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import com.malickabdullah.tessera.data.MoonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The drawn disc's lit area and side match the phase, sampled from real Skia pixels. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class MoonDiscTest {
    private val r = 100f
    private val c = 120f

    /** (lit fraction of the disc, lit fraction of its right half). */
    private fun render(phase: MoonPhase, southern: Boolean, lit: Int, shadow: Int, background: Int): Pair<Double, Double> {
        val bmp = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(background)
        SkyKit.moonDisc(canvas, c, c, r, phase, southern, lit, shadow)
        var disc = 0
        var litCount = 0
        var right = 0
        var litRight = 0
        // Maria, craters and limb darkening dim the lit side by up to about half; the shadow side stays near its tone.
        val threshold = lum(shadow) + 0.3f * (lum(lit) - lum(shadow))
        for (y in 0 until 240) for (x in 0 until 240) {
            val dx = x + 0.5f - c
            val dy = y + 0.5f - c
            if (dx * dx + dy * dy > (r - 2f) * (r - 2f)) continue
            val isLit = lum(bmp.getPixel(x, y)) > threshold
            disc++
            if (isLit) litCount++
            if (dx > 0) {
                right++
                if (isLit) litRight++
            }
        }
        bmp.recycle()
        return litCount.toDouble() / disc to litRight.toDouble() / right
    }

    private fun lum(color: Int): Float =
        0.2126f * (color shr 16 and 0xFF) + 0.7152f * (color shr 8 and 0xFF) + 0.0722f * (color and 0xFF)

    private val dark = Triple(0xFFE9E6DA.toInt(), 0xFF141518.toInt(), 0xFF0E0F13.toInt())
    private val light = Triple(0xFFE9E6DA.toInt(), 0xFF2B2D33.toInt(), 0xFFF2F3F5.toInt())

    private fun check(illumination: Double, waxing: Boolean, southern: Boolean) {
        for ((lit, shadow, bg) in listOf(dark, light)) {
            val (fraction, rightFraction) = render(MoonPhase(illumination, 0.0, waxing), southern, lit, shadow, bg)
            assertEquals("lit area at $illumination (waxing=$waxing, southern=$southern)", illumination, fraction, 0.05)
            // The lit limb is on the right when waxing in the north, and mirrored in the south.
            val rightLit = waxing != southern
            val expectedRight = if (rightLit) minOf(1.0, illumination * 2) else maxOf(0.0, illumination * 2 - 1)
            assertEquals("right half at $illumination (waxing=$waxing, southern=$southern)", expectedRight, rightFraction, 0.06)
        }
    }

    @Test
    fun waningGibbousLeavesTheRightEdgeDark() = check(0.84, waxing = false, southern = false)

    @Test
    fun waxingCrescentIsLitOnTheRight() = check(0.25, waxing = true, southern = false)

    @Test
    fun southernHemisphereMirrorsTheLitSide() {
        check(0.25, waxing = true, southern = true)
        check(0.84, waxing = false, southern = true)
    }

    @Test
    fun quarterAndFullAndNew() {
        check(0.5, waxing = true, southern = false)
        check(1.0, waxing = true, southern = false)
        val (fraction, _) = render(MoonPhase(0.0, 0.0, true), false, dark.first, dark.second, dark.third)
        assertTrue("new moon shows no lit area, got $fraction", fraction < 0.02)
    }
}
