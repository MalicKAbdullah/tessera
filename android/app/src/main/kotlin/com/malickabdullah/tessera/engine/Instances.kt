package com.malickabdullah.tessera.engine

import android.content.Context
import com.malickabdullah.tessera.security.Vault
import org.json.JSONObject

/** The design and style one placed widget (appWidgetId) is bound to. */
data class Binding(val designId: String, val style: Style)

/**
 * Per-appWidgetId bindings. A placed widget is unbound until the pin
 * callback, the configure screen or the v0.1 migration binds it; unbound
 * widgets draw their slot's default design in its default style.
 *
 * Bindings carry what the user chose (photo surfaces included), so they live
 * in the [Vault]. The live keys only record what a render depended on (dates,
 * levels, counts) and stay in plain preferences.
 */
object Instances {
    const val DOC = "instances"
    const val PREFS = "tessera.instances"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun all(context: Context): JSONObject = Vault.readText(context, DOC)?.let(::JSONObject) ?: JSONObject()

    fun get(context: Context, id: Int): Binding? {
        val json = all(context).optJSONObject("$id") ?: return null
        return Binding(json.getString("design"), Style.parse(json.getJSONObject("style")))
    }

    /** Synchronized: the pin callback, configure screen and editor may bind at once, and each rewrites the whole document. */
    @Synchronized
    fun put(context: Context, id: Int, designId: String, style: Style) {
        Registry.design(designId)
        val all = all(context).put("$id", JSONObject().put("design", designId).put("style", style.toJson()))
        Vault.writeText(context, DOC, all.toString())
        prefs(context).edit().remove("k.$id").apply()
    }

    @Synchronized
    fun remove(context: Context, id: Int) {
        val all = all(context)
        if (all.remove("$id") != null) Vault.writeText(context, DOC, all.toString())
        prefs(context).edit().remove("k.$id").apply()
    }

    /** The [WidgetDesign.liveKey] the instance was last drawn with. */
    fun lastKey(context: Context, id: Int): String? = prefs(context).getString("k.$id", null)

    fun setLastKey(context: Context, id: Int, key: String?) {
        prefs(context).edit().putString("k.$id", key).apply()
    }
}
