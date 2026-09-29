package com.malickabdullah.tessera.engine

import android.content.Context
import android.graphics.Typeface
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/** One static font file: its weight, the font resource and its TextClock layouts. */
data class FontFace(val weight: Int, val font: Int, val clockLayout: Int, val capsClockLayout: Int)

data class FontFamilyInfo(val key: String, val label: String, val faces: List<FontFace>)

/**
 * Bundled families (see tool/gen_font_layouts.sh). Every family ships static
 * files per weight, so a requested weight snaps to the nearest face — the
 * same rule the Dart weight slider uses, so the editor never shows a weight
 * the widget cannot draw.
 */
object Fonts {
    val families: List<FontFamilyInfo> = fontTable

    private val typefaces = HashMap<Int, Typeface>()

    fun family(key: String): FontFamilyInfo = families.first { it.key == key }

    fun face(key: String, weight: Int): FontFace = family(key).faces.minBy { abs(it.weight - weight) }

    fun typeface(context: Context, key: String, weight: Int): Typeface {
        val res = face(key, weight).font
        return synchronized(typefaces) { typefaces.getOrPut(res) { context.resources.getFont(res) } }
    }

    fun catalog(): JSONArray = JSONArray(
        families.map { f ->
            JSONObject().put("key", f.key).put("label", f.label).put("weights", JSONArray(f.faces.map { it.weight }))
        },
    )
}
