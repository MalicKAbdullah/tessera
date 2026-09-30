package com.malickabdullah.tessera.engine

import android.content.Context
import org.json.JSONObject

/** The design and style one placed widget (appWidgetId) is bound to. */
data class Binding(val designId: String, val style: Style)

/**
 * Per-appWidgetId bindings. A placed widget is unbound until the pin
 * callback, the configure screen or the v0.1 migration binds it; unbound
 * widgets draw their slot's default design in its default style.
 */
object Instances {
    private const val PREFS = "tessera.instances"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(context: Context, id: Int): Binding? {
        val raw = prefs(context).getString("i.$id", null) ?: return null
        val json = JSONObject(raw)
        return Binding(json.getString("design"), Style.parse(json.getJSONObject("style")))
    }

    fun put(context: Context, id: Int, designId: String, style: Style) {
        Registry.design(designId)
        prefs(context).edit()
            .putString("i.$id", JSONObject().put("design", designId).put("style", style.toJson()).toString())
            .remove("k.$id")
            .apply()
    }

    fun remove(context: Context, id: Int) {
        prefs(context).edit().remove("i.$id").remove("k.$id").apply()
    }

    /** The [WidgetDesign.liveKey] the instance was last drawn with. */
    fun lastKey(context: Context, id: Int): String? = prefs(context).getString("k.$id", null)

    fun setLastKey(context: Context, id: Int, key: String?) {
        prefs(context).edit().putString("k.$id", key).apply()
    }
}
