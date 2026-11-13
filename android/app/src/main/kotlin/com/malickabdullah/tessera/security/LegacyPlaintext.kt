package com.malickabdullah.tessera.security

import android.content.Context
import com.malickabdullah.tessera.data.ContentSource
import com.malickabdullah.tessera.data.PhotoStore
import com.malickabdullah.tessera.data.WeatherSource
import com.malickabdullah.tessera.engine.Instances
import org.json.JSONObject
import java.io.File

/**
 * Up to v0.3.0 the engine kept the user's data in plain files: content,
 * widget bindings and the forecast for their city in shared preferences,
 * photos and photo surfaces as JPEGs. Each is sealed into the [Vault] and
 * only then deleted, so an interrupted move repeats from the untouched
 * plaintext on the next start.
 */
internal object LegacyPlaintext {
    private const val CONTENT_PREFS = "tessera.content"
    private const val WEATHER_PREFS = "tessera.weather"
    private const val INSTANCE_PREFIX = "i."

    fun migrate(context: Context, key: ByteArray) {
        content(context, key)
        instances(context, key)
        weather(context, key)
        photos(context, key)
        backgrounds(context, key)
    }

    /**
     * Styles referred to a photo surface by its path in `files/backgrounds`;
     * they now refer to it by the content hash that named the file.
     */
    fun backgroundId(path: String): String = File(path).nameWithoutExtension

    private fun prefsExist(context: Context, name: String) =
        File(context.dataDir, "shared_prefs/$name.xml").exists()

    private fun content(context: Context, key: ByteArray) {
        if (!prefsExist(context, CONTENT_PREFS)) return
        context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE).getString("content", null)?.let {
            Vault.put(context, key, ContentSource.DOC, it.toByteArray())
        }
        check(context.deleteSharedPreferences(CONTENT_PREFS)) { "Could not delete $CONTENT_PREFS" }
    }

    /** Bindings move to the vault; the same file keeps only the live keys, which hold no user data. */
    private fun instances(context: Context, key: ByteArray) {
        val prefs = context.getSharedPreferences(Instances.PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.all.filterKeys { it.startsWith(INSTANCE_PREFIX) }
        if (legacy.isEmpty()) return
        val doc = JSONObject()
        legacy.forEach { (name, raw) ->
            val binding = JSONObject(raw as String)
            val bg = binding.getJSONObject("style").getJSONObject("bg")
            if (!bg.isNull("photo")) bg.put("photo", backgroundId(bg.getString("photo")))
            doc.put(name.removePrefix(INSTANCE_PREFIX), binding)
        }
        Vault.put(context, key, Instances.DOC, doc.toString().toByteArray())
        val editor = prefs.edit()
        legacy.keys.forEach(editor::remove)
        check(editor.commit()) { "Could not clear plaintext bindings" }
    }

    private fun weather(context: Context, key: ByteArray) {
        if (!prefsExist(context, WEATHER_PREFS)) return
        val prefs = context.getSharedPreferences(WEATHER_PREFS, Context.MODE_PRIVATE)
        prefs.getString("forecast", null)?.let { forecast ->
            val doc = JSONObject()
                .put("forecast", JSONObject(forecast))
                .put("city", prefs.getString("city", null))
                .put("version", prefs.getInt("version", 0))
                .put("fetchedAt", prefs.getLong("fetchedAt", 0))
            Vault.put(context, key, WeatherSource.DOC, doc.toString().toByteArray())
        }
        check(context.deleteSharedPreferences(WEATHER_PREFS)) { "Could not delete $WEATHER_PREFS" }
    }

    private fun photos(context: Context, key: ByteArray) {
        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) return
        val index = File(dir, "album.json")
        if (index.exists()) {
            val album = JSONObject(index.readText())
            val photos = album.getJSONArray("photos")
            for (i in 0 until photos.length()) {
                val id = photos.getJSONObject(i).getString("id")
                Vault.put(context, key, PhotoStore.photoName(id), File(dir, "$id.jpg").readBytes())
            }
            Vault.put(context, key, PhotoStore.ALBUM, album.toString().toByteArray())
        }
        check(dir.deleteRecursively()) { "Could not delete plaintext photos" }
    }

    private fun backgrounds(context: Context, key: ByteArray) {
        val dir = File(context.filesDir, "backgrounds")
        if (!dir.exists()) return
        dir.listFiles { f -> f.extension == "jpg" }!!.forEach {
            Vault.put(context, key, PhotoStore.backgroundName(it.nameWithoutExtension), it.readBytes())
        }
        check(dir.deleteRecursively()) { "Could not delete plaintext photo surfaces" }
    }
}
