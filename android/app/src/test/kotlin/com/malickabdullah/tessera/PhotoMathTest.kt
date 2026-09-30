package com.malickabdullah.tessera

import com.malickabdullah.tessera.data.Crop
import com.malickabdullah.tessera.data.PhotoMath
import com.malickabdullah.tessera.designs.photo.Wrap
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMathTest {
    @Test
    fun budgetIsOneAndAHalfScreensOfArgb() {
        assertEquals(1080L * 2400 * 4 * 3 / 2, PhotoMath.remoteViewsBudget(1080, 2400))
    }

    @Test
    fun storedSizeCapsTheShortSideAndTheLongSideAtTwiceIt() {
        assertEquals(1080 to 1440, PhotoMath.storedSize(3000, 4000, 1080))
        assertEquals(2160 to 540, PhotoMath.storedSize(8000, 2000, 1080))
        assertEquals(800 to 600, PhotoMath.storedSize(800, 600, 1080))
    }

    @Test
    fun sampleSizeNeverDropsBelowTheRequest() {
        assertEquals(4, PhotoMath.sampleSize(4000, 3000, 900, 700))
        assertEquals(1, PhotoMath.sampleSize(1000, 800, 900, 700))
        assertEquals(2, PhotoMath.sampleSize(4000, 3000, 1000, 1500))
    }

    @Test
    fun coverCropsTheCentreAtTheTargetAspect() {
        assertEquals(Crop(500, 0, 3000, 3000), PhotoMath.cover(4000, 3000, 170f, 170f))
        assertEquals(Crop(0, 1000, 3000, 1000), PhotoMath.cover(3000, 3000, 350f, 116.666f))
    }

    @Test
    fun flipperKeepsFullScaleWhenItFits() {
        val budget = PhotoMath.remoteViewsBudget(1080, 2400)
        val plan = PhotoMath.flipperPlan(budget, PhotoMath.bitmapBytes(170f, 170f, 2.75f), 150f, 150f, 2.75f, 1.6f, 4)
        assertEquals(4, plan.frames)
        assertEquals(2.75f, plan.scale, 0.0001f)
    }

    @Test
    fun flipperGivesUpResolutionThenFramesAndStaysInBudget() {
        val budget = PhotoMath.remoteViewsBudget(1080, 2400)
        val main = PhotoMath.bitmapBytes(350f, 350f, 2.7f)
        val lower = PhotoMath.flipperPlan(budget, main, 330f, 330f, 2.7f, 1.6f, 6)
        assertEquals(6, lower.frames)
        assertTrue(lower.scale < 2.7f && lower.scale >= 1.6f)
        assertTrue(main + lower.frames * PhotoMath.bitmapBytes(330f, 330f, lower.scale) <= budget)

        val tiny = PhotoMath.remoteViewsBudget(720, 1280)
        val fewer = PhotoMath.flipperPlan(tiny, PhotoMath.bitmapBytes(350f, 350f, 2f), 330f, 330f, 2f, 1.6f, 6)
        assertTrue(fewer.frames in 2..5)
        assertTrue(PhotoMath.bitmapBytes(350f, 350f, 2f) + fewer.frames * PhotoMath.bitmapBytes(330f, 330f, fewer.scale) <= tiny)
    }

    @Test
    fun flipperIsDroppedWhenTwoFramesCannotFit() {
        val plan = PhotoMath.flipperPlan(4_000_000, 3_500_000, 330f, 330f, 2f, 1.6f, 6)
        assertEquals(0, plan.frames)
    }

    @Test
    fun normalizeStretchesToFullRange() {
        val out = PhotoMath.normalize(FloatArray(100) { 0.4f + it * 0.002f })
        assertEquals(0f, out.first(), 0.0001f)
        assertEquals(1f, out.last(), 0.0001f)
        assertArrayEquals(floatArrayOf(0.5f, 0.5f), PhotoMath.normalize(floatArrayOf(0.3f, 0.3f)), 0.0001f)
    }

    @Test
    fun rotationWrapsAroundTheAlbum() {
        assertEquals(2, PhotoMath.rotation(7, 5))
        assertEquals(0, PhotoMath.rotation(-5, 5))
        assertEquals(0, PhotoMath.rotation(3, 0))
    }

    @Test
    fun captionsWrapWhenItMakesTheTextLarger() {
        val measure = { s: String -> s.length * 50f }
        assertEquals(listOf("Summer in Lisbon"), Wrap.fit("Summer in Lisbon", 3, 400f, 40f, 1.1f, measure).lines)
        val tall = Wrap.fit("Summer in Lisbon", 3, 120f, 200f, 1.1f, measure)
        assertTrue(tall.lines.size > 1)
        assertEquals("Summer in Lisbon", tall.lines.joinToString(" "))
    }
}
