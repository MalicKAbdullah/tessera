package com.malickabdullah.tessera.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.malickabdullah.tessera.data.ContentSource
import com.malickabdullah.tessera.data.PhotoStore
import com.malickabdullah.tessera.data.WeatherSource
import com.malickabdullah.tessera.engine.Instances
import com.malickabdullah.tessera.engine.Registry
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** v0.3.0's plaintext, exactly as that release wrote it, is sealed and then deleted. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyPlaintextTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val surface = "0123456789abcdef0123456789abcdef"
    private val photoJpeg = byteArrayOf(1, 2, 3, 4)
    private val surfaceJpeg = byteArrayOf(5, 6, 7)

    private fun seedV030() {
        val content = JSONObject()
            .put("note", "Buy milk").put("noteAuthor", "Me").put("countdownTitle", "Lisbon")
            .put("countdownDate", "2027-01-20T00:00:00.000").put("countdownStart", "2026-11-14T00:00:00.000")
            .put("events", org.json.JSONArray()).put("checklist", org.json.JSONArray())
            .put("city", JSONObject().put("name", "Berlin").put("region", "DE").put("latitude", 52.52).put("longitude", 13.4))
        context.getSharedPreferences("tessera.content", Context.MODE_PRIVATE).edit()
            .putString("content", content.toString()).commit()

        val sticky = Registry.design("note.sticky")
        val photoStyle = sticky.defaults.toJson()
        photoStyle.getJSONObject("bg").put("kind", "photo").put("photo", "${context.filesDir}/backgrounds/$surface.jpg")
        context.getSharedPreferences(Instances.PREFS, Context.MODE_PRIVATE).edit()
            .putString("i.5", JSONObject().put("design", sticky.id).put("style", photoStyle).toString())
            .putString("k.5", "2026-11-14")
            .commit()

        context.getSharedPreferences("tessera.weather", Context.MODE_PRIVATE).edit()
            .putString("forecast", "{\"current\":{}}").putString("city", "52.52,13.4")
            .putInt("version", WeatherSource.CACHE_VERSION).putLong("fetchedAt", 42L).commit()

        val photos = File(context.filesDir, "photos").apply { mkdirs() }
        File(photos, "p1.jpg").writeBytes(photoJpeg)
        File(photos, "album.json").writeText(
            JSONObject().put("caption", "Summer").put(
                "photos",
                org.json.JSONArray().put(JSONObject().put("id", "p1").put("width", 3).put("height", 4).put("date", "2026-08-01")),
            ).toString(),
        )
        File(context.filesDir, "backgrounds").apply { mkdirs() }.resolve("$surface.jpg").writeBytes(surfaceJpeg)
    }

    @Test
    fun everyPlaintextStoreIsSealedThenDeleted() {
        seedV030()

        LegacyPlaintext.migrate(context, Vault.key(context))

        val content = ContentSource.read(context)
        assertEquals("Buy milk", content.note)
        assertEquals("Lisbon", content.countdownTitle)
        assertEquals("Berlin", content.city!!.name)

        val binding = Instances.get(context, 5)!!
        assertEquals("note.sticky", binding.designId)
        assertEquals(surface, binding.style.bg.photo)
        assertEquals("2026-11-14", Instances.lastKey(context, 5))

        assertEquals(42L, WeatherSource.cache(context)!!.getLong("fetchedAt"))

        val album = PhotoStore.read(context)
        assertEquals("Summer", album.caption)
        assertEquals(listOf("p1"), album.photos.map { it.id })
        assertArrayEquals(photoJpeg, PhotoStore.bytes(context, "p1"))
        assertArrayEquals(surfaceJpeg, PhotoStore.background(context, surface))

        val prefs = File(context.dataDir, "shared_prefs")
        assertFalse(File(prefs, "tessera.content.xml").exists())
        assertFalse(File(prefs, "tessera.weather.xml").exists())
        assertFalse(File(prefs, "${Instances.PREFS}.xml").readText().contains("i.5"))
        assertFalse(File(context.filesDir, "photos").exists())
        assertFalse(File(context.filesDir, "backgrounds").exists())
        File(context.filesDir, "vault").walk().filter { it.isFile }.forEach {
            assertFalse(it.path, String(it.readBytes(), Charsets.ISO_8859_1).contains("Buy milk"))
        }
    }

    @Test
    fun aSecondRunLeavesSealedDataAlone() {
        seedV030()
        LegacyPlaintext.migrate(context, Vault.key(context))
        ContentSource.write(context, "{\"note\":\"Edited\",\"noteAuthor\":\"\",\"countdownTitle\":\"\",\"countdownDate\":null,\"city\":null}")

        LegacyPlaintext.migrate(context, Vault.key(context))

        assertEquals("Edited", ContentSource.read(context).note)
    }
}
