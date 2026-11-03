package com.malickabdullah.tessera

import android.appwidget.AppWidgetManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.WidgetSizes
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetSizesTest {
    /** A 4×2 widget on a phone: tall cells in portrait, short wide ones in landscape. */
    private fun range() = Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 520)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180)
    }

    @Test
    fun android12UsesEveryReportedSize() {
        val sizes = arrayListOf(SizeF(320f, 180f), SizeF(520f, 110f), SizeF(410f, 200f), SizeF(320f, 180f))
        val options = range().apply { putParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, sizes) }
        assertEquals(
            listOf(SizeF(320f, 180f), SizeF(520f, 110f), SizeF(410f, 200f)),
            WidgetSizes.of(options, SizeClass.WIDE, landscape = false, sdk = Build.VERSION_CODES.S),
        )
    }

    @Test
    fun android12WithoutSizeListGetsBothOrientationsFromTheRange() {
        assertEquals(
            listOf(SizeF(320f, 180f), SizeF(520f, 110f)),
            WidgetSizes.of(range(), SizeClass.WIDE, landscape = false, sdk = Build.VERSION_CODES.S),
        )
    }

    @Test
    fun beforeAndroid12OnlyTheCurrentOrientationIsDrawn() {
        assertEquals(listOf(SizeF(320f, 180f)), WidgetSizes.of(range(), SizeClass.WIDE, landscape = false, sdk = Build.VERSION_CODES.R))
        assertEquals(listOf(SizeF(520f, 110f)), WidgetSizes.of(range(), SizeClass.WIDE, landscape = true, sdk = Build.VERSION_CODES.R))
    }

    @Test
    fun anUnreportedWidgetUsesItsSlotSize() {
        assertEquals(listOf(SizeF(350f, 350f)), WidgetSizes.of(Bundle(), SizeClass.LARGE, landscape = true, sdk = Build.VERSION_CODES.S))
        assertEquals(listOf(SizeF(170f, 170f)), WidgetSizes.of(Bundle(), SizeClass.SMALL, landscape = false, sdk = Build.VERSION_CODES.O))
    }
}
