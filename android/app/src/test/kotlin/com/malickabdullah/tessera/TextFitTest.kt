package com.malickabdullah.tessera

import com.malickabdullah.tessera.designs.TextFit
import com.malickabdullah.tessera.designs.note.Quotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TextFitTest {
    /** Every character is 10 wide at size 20. */
    private val ten: (String) -> Float = { it.length * 10f }
    private val perSize: (String, Float) -> Float = { str, size -> str.length * size / 2f }

    @Test
    fun wrapsAtWordBoundaries() {
        assertEquals(listOf("the quick", "brown fox"), TextFit.wrap("the quick brown fox", 90f, ten))
        assertEquals(listOf("a", "b"), TextFit.wrap("a\nb", 90f, ten))
        assertEquals(listOf("", "a"), TextFit.wrap("\na", 90f, ten))
    }

    @Test
    fun breaksWordsWiderThanTheLine() {
        assertEquals(listOf("abcde", "fghij", "k"), TextFit.wrap("abcdefghijk", 50f, ten))
    }

    @Test
    fun ellipsizeKeepsTheResultWithinTheWidth() {
        val out = TextFit.ellipsize("a very long title", 80f, ten)
        assertTrue(ten(out) <= 80f)
        assertTrue(out.endsWith("…"))
        assertEquals("short", TextFit.ellipsize("short", 80f, ten))
    }

    @Test
    fun fitSizeGrowsToTheBoxAndShrinksWithMoreText() {
        val short = TextFit.fitSize("hello", 200f, 100f, 1.2f, 8f, 60f, perSize)
        val long = TextFit.fitSize("hello there my very good friend how are you doing today", 200f, 100f, 1.2f, 8f, 60f, perSize)
        assertTrue(short > long)
        assertTrue(short <= 60f)
        for (size in listOf(short, long)) {
            val lines = TextFit.wrap("hello there my very good friend how are you doing today", 200f) { perSize(it, size) }
            if (size == long) assertTrue(lines.size * size * 1.2f <= 100f + 0.01f)
        }
    }

    @Test
    fun fitSizeReturnsTheMinimumWhenNothingFits() {
        assertEquals(8f, TextFit.fitSize("x ".repeat(500), 100f, 30f, 1.2f, 8f, 40f, perSize), 0f)
    }

    @Test
    fun clampCutsWithAnEllipsisWithinWidth() {
        val lines = listOf("aaaaaaaaaa", "bbbbbbbbbb", "cccccccccc")
        val out = TextFit.clamp(lines, 2, 100f, ten)
        assertEquals(2, out.size)
        assertTrue(out[1].endsWith("…"))
        assertTrue(ten(out[1]) <= 100f)
        assertEquals(lines, TextFit.clamp(lines, 3, 100f, ten))
    }

    @Test
    fun paginateSplitsIntoRowsAndCapsPages() {
        val lines = (1..7).map { "line$it" }
        assertEquals(listOf(lines.take(3), lines.drop(3).take(3), lines.drop(6)), TextFit.paginate(lines, 3, 5, 200f, ten))
        val capped = TextFit.paginate(lines, 3, 2, 200f, ten)
        assertEquals(2, capped.size)
        assertTrue(capped.last().last().endsWith("…"))
        assertEquals(listOf(emptyList<String>()), TextFit.paginate(emptyList(), 3, 2, 200f, ten))
    }

    @Test
    fun dailyQuoteChangesEachDayAndCyclesThroughTheSet() {
        val day = LocalDate.of(2026, 9, 30)
        assertTrue(Quotes.of(day) != Quotes.of(day.plusDays(1)))
        assertEquals(Quotes.of(day), Quotes.of(day.plusDays(Quotes.all.size.toLong())))
        assertEquals(Quotes.of(LocalDate.of(1969, 7, 20)), Quotes.of(LocalDate.of(1969, 7, 20)))
    }

    @Test
    fun everyQuoteIsCitedAndDistinct() {
        assertTrue(Quotes.all.all { it.text.isNotBlank() && it.author.isNotBlank() && it.source.isNotBlank() })
        assertEquals(Quotes.all.size, Quotes.all.map { it.text }.toSet().size)
    }
}
