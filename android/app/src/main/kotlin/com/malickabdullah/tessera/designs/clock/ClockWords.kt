package com.malickabdullah.tessera.designs.clock

import android.graphics.Paint
import android.graphics.RectF
import com.malickabdullah.tessera.engine.Category
import com.malickabdullah.tessera.engine.Scene
import com.malickabdullah.tessera.engine.SceneInputs
import com.malickabdullah.tessera.engine.SizeClass
import com.malickabdullah.tessera.engine.Style
import com.malickabdullah.tessera.engine.WidgetDesign
import kotlin.math.min

/** Which letters of the grid spell a time. Pure, so it is unit-tested. */
object WordClock {
    val grid = listOf(
        "ITLISASAMPM",
        "ACQUARTERDC",
        "TWENTYFIVEX",
        "HALFSTENFTO",
        "PASTERUNINE",
        "ONESIXTHREE",
        "FOURFIVETWO",
        "EIGHTELEVEN",
        "SEVENTWELVE",
        "TENSEOCLOCK",
    )
    const val COLS = 11

    private data class Word(val row: Int, val col: Int, val len: Int)

    private val IT = Word(0, 0, 2)
    private val IS = Word(0, 3, 2)
    private val A = Word(1, 0, 1)
    private val QUARTER = Word(1, 2, 7)
    private val TWENTY = Word(2, 0, 6)
    private val FIVE_MIN = Word(2, 6, 4)
    private val HALF = Word(3, 0, 4)
    private val TEN_MIN = Word(3, 5, 3)
    private val TO = Word(3, 9, 2)
    private val PAST = Word(4, 0, 4)
    private val OCLOCK = Word(9, 5, 6)
    private val HOURS = listOf(
        Word(8, 5, 6), // twelve
        Word(5, 0, 3), Word(6, 8, 3), Word(5, 6, 5), Word(6, 0, 4), Word(6, 4, 4), Word(5, 3, 3),
        Word(8, 0, 5), Word(7, 0, 5), Word(4, 7, 4), Word(9, 0, 3), Word(7, 5, 6),
    )

    /** Cell indexes (row * COLS + col) lit for the five-minute step containing [hour]:[minute]. */
    fun lit(hour: Int, minute: Int): Set<Int> {
        val (minutes, hourWord) = words(hour, minute)
        return (listOf(IT, IS) + minutes + hourWord).flatMap { w -> (w.col until w.col + w.len).map { w.row * COLS + it } }.toSet()
    }

    /**
     * The lit phrase without "IT IS", as (minute words, relation, hour):
     * ("TWENTY", "TO", "TEN"), or ("", "O'CLOCK", "NINE") on the hour.
     */
    fun phrase(hour: Int, minute: Int): Triple<String, String, String> {
        val (minutes, hourWord) = words(hour, minute)
        val relation = minutes.last()
        val rest = minutes.dropLast(1).joinToString(" ") { text(it) }
        return Triple(rest, if (relation == OCLOCK) "O'CLOCK" else text(relation), text(hourWord))
    }

    private fun text(w: Word) = grid[w.row].substring(w.col, w.col + w.len)

    /** Minute words ending in PAST, TO or O'CLOCK, and the hour word. */
    private fun words(hour: Int, minute: Int): Pair<List<Word>, Word> {
        val step = minute / 5
        val minutes = when (step) {
            0 -> listOf(OCLOCK)
            1 -> listOf(FIVE_MIN, PAST)
            2 -> listOf(TEN_MIN, PAST)
            3 -> listOf(A, QUARTER, PAST)
            4 -> listOf(TWENTY, PAST)
            5 -> listOf(TWENTY, FIVE_MIN, PAST)
            6 -> listOf(HALF, PAST)
            7 -> listOf(TWENTY, FIVE_MIN, TO)
            8 -> listOf(TWENTY, TO)
            9 -> listOf(A, QUARTER, TO)
            10 -> listOf(TEN_MIN, TO)
            else -> listOf(FIVE_MIN, TO)
        }
        return minutes to HOURS[(if (step >= 7) hour + 1 else hour) % 12]
    }
}

object ClockWords : WidgetDesign {
    override val id = "clock.words"
    override val category = Category.CLOCK
    override val name = "Word Clock"
    override val blurb = "The time spelled out in a lit letter grid, with a dot per minute in the corners."
    override val sizes = listOf(SizeClass.SMALL, SizeClass.LARGE)
    override val defaults = Style.of("mono", 500, text = 0xFFEAF0FF, accent = 0xFF8FB3FF, background = 0xFF0F1A2B, radius = 30f, padding = 14f)

    override fun liveKey(scene: SceneInputs) = "${scene.now.hour}:${scene.now.minute}"

    override fun draw(s: Scene) {
        if (s.w < 240f) small(s) else grid(s)
        val b = s.box
        val dots = s.now.minute % 5
        val corners = listOf(b.left + 3f to b.top + 3f, b.right - 3f to b.top + 3f, b.right - 3f to b.bottom - 3f, b.left + 3f to b.bottom - 3f)
        corners.forEachIndexed { i, (x, y) -> s.canvas.drawCircle(x, y, 2.2f, s.fill(if (i < dots) s.accent else s.ink(0.1f))) }
    }

    /** The full letter grid with the phrase lit, in cells that fill the box. */
    private fun grid(s: Scene) {
        val b = s.box
        val inset = 8f
        val cell = min((b.width() - 2 * inset) / WordClock.COLS, (b.height() - 2 * inset) / WordClock.grid.size)
        val left = b.centerX() - cell * WordClock.COLS / 2f
        val top = b.centerY() - cell * WordClock.grid.size / 2f
        val lit = WordClock.lit(s.now.hour, s.now.minute)
        val on = s.paint(cell * 0.62f * s.hero, s.text, weight = 700, align = Paint.Align.CENTER, tracking = 0f)
        val off = s.paint(cell * 0.62f * s.hero, s.ink(0.12f), weight = 300, align = Paint.Align.CENTER, tracking = 0f)
        WordClock.grid.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch ->
                val p = if ((r * WordClock.COLS + c) in lit) on else off
                s.textMid(ch.toString(), left + (c + 0.5f) * cell, top + (r + 0.5f) * cell, p)
            }
        }
    }

    /**
     * 2×2 is too small for a readable 11-letter grid: the lit phrase is set
     * large, one part per line, over the grid as a faint texture.
     */
    private fun small(s: Scene) {
        val b = s.box
        val cell = min(b.width() / WordClock.COLS, b.height() / WordClock.grid.size)
        val texture = s.paint(cell * 0.6f, s.ink(0.06f), weight = 400, align = Paint.Align.CENTER, tracking = 0f)
        val gx = b.centerX() - cell * WordClock.COLS / 2f
        val gy = b.centerY() - cell * WordClock.grid.size / 2f
        WordClock.grid.forEachIndexed { r, row ->
            row.forEachIndexed { c, ch -> s.textMid(ch.toString(), gx + (c + 0.5f) * cell, gy + (r + 0.5f) * cell, texture) }
        }

        val (minutes, relation, hour) = WordClock.phrase(s.now.hour, s.now.minute)
        val lines = listOf(minutes to s.text, relation to s.ink(0.6f), hour to s.accent).filter { it.first.isNotEmpty() }
        val area = RectF(b.left + 6f, b.top + 8f, b.right - 6f, b.bottom - 8f)
        val lineH = area.height() / lines.size
        val widest = lines.maxBy { s.paint(100f, weight = 700, tracking = 0f).measureText(it.first) }.first
        val size = s.fitCaps(widest, area.width(), lineH * 0.62f, weight = 700) * s.hero
        lines.forEachIndexed { i, (text, color) ->
            s.textMid(text, area.left, area.top + lineH * (i + 0.5f), s.paint(size, color, weight = 700, tracking = 0f))
        }
    }
}
