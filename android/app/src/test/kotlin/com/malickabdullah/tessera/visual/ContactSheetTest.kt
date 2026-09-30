package com.malickabdullah.tessera.visual

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.malickabdullah.tessera.engine.Category
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * One PNG per category for reviewing designs side by side: a row per design
 * and size, a column per theme and data state. Runs only when a folder is
 * given: `./gradlew :app:testDebugUnitTest --tests '*ContactSheetTest' -Ptessera.sheets=/some/dir`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-420dpi")
class ContactSheetTest {
    @Test
    fun sheets() {
        val out = System.getProperty("tessera.sheets").orEmpty()
        assumeTrue("no -Ptessera.sheets folder given", out.isNotEmpty())
        val context: Context = ApplicationProvider.getApplicationContext()
        ScreenshotEnv.apply(context)
        File(out).mkdirs()
        Category.entries.forEach { sheet(context, it, File(out, "${it.id}.png")) }
    }

    private fun sheet(context: Context, category: Category, file: File) {
        val px = 2f
        val gap = 24f
        val label = 22f
        val shots = Shots.of(category)
        val rows = shots.groupBy { it.design.id to it.size }.values.toList()
        val columns = Theme.entries.flatMap { t -> Shots.states(category).map { t to it } }
        val colW = 350f * px
        val rowH = rows.map { it.first().size.heightDp * px }
        val headerH = 48f
        val width = (gap + (colW + gap) * columns.size + 260f).toInt()
        val height = (headerH + rowH.sumOf { (it + label + gap).toDouble() } + gap).toInt()
        val sheet = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        c.drawColor(0xFF7A7F87.toInt())
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 20f }
        val head = Paint(text).apply { textSize = 26f; isFakeBoldText = true }
        columns.forEachIndexed { i, (t, s) -> c.drawText("${t.id} · ${s.id}", 260f + gap + i * (colW + gap), 32f, head) }
        var y = headerH
        rows.forEachIndexed { r, row ->
            c.drawText(row.first().design.id, gap, y + label + 20f, head)
            c.drawText(row.first().size.id, gap, y + label + 52f, text)
            columns.forEachIndexed { i, (t, s) ->
                val shot = row.first { it.theme == t && it.state == s }
                val bmp = Shots.render(context, shot, ScreenshotEnv.photoDir, px)
                val x = 260f + gap + i * (colW + gap)
                c.drawText(shot.name.substringAfter('_'), x, y + 16f, text)
                c.drawBitmap(bmp, null, RectF(x, y + label, x + bmp.width, y + label + bmp.height), null)
                bmp.recycle()
            }
            y += rowH[r] + label + gap
        }
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sheet.recycle()
    }
}
