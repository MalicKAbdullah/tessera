package com.malickabdullah.tessera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import com.malickabdullah.tessera.security.Vault
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.min

/** One imported photo: a private, pre-downscaled JPEG sealed in the [Vault]; its bytes come from [Data.photo]. */
data class Photo(val id: String, val width: Int, val height: Int, val date: LocalDate) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("width", width).put("height", height)
        .put("date", date.toString())
}

/** The photos the photo designs show, newest first, and an optional caption. */
data class PhotoAlbum(val photos: List<Photo>, val caption: String) {
    fun toJson(): JSONObject = JSONObject()
        .put("photos", JSONArray(photos.map { it.toJson() }))
        .put("caption", caption)
        .put("max", PhotoStore.MAX)
}

/**
 * Photos arrive from the system photo picker as content URIs, which only
 * stay readable while the picker's grant lasts, so each is decoded once,
 * rotated upright, downscaled to what a widget can show and sealed into the
 * [Vault]. Widgets read only those copies.
 */
object PhotoStore : DataSource<PhotoAlbum> {
    const val MAX = 12
    const val ALBUM = "album"

    fun photoName(id: String) = "photos/$id"

    fun backgroundName(id: String) = "backgrounds/$id"

    override fun read(context: Context): PhotoAlbum {
        val json = Vault.readText(context, ALBUM)?.let(::JSONObject) ?: return PhotoAlbum(emptyList(), "")
        val photos = json.getJSONArray("photos")
        return PhotoAlbum(
            (0 until photos.length()).map { i ->
                val p = photos.getJSONObject(i)
                Photo(
                    p.getString("id"),
                    p.getInt("width"),
                    p.getInt("height"),
                    LocalDate.parse(p.getString("date")),
                )
            },
            json.getString("caption"),
        )
    }

    fun bytes(context: Context, id: String): ByteArray =
        checkNotNull(Vault.read(context, photoName(id))) { "Photo $id is missing" }

    /** The JPEG of a photo surface, by the id [importBackground] returned. */
    fun background(context: Context, id: String): ByteArray =
        checkNotNull(Vault.read(context, backgroundName(id))) { "Photo surface $id is missing" }

    /** The index is replaced before any photo is deleted, so a render never lists a photo that is gone. */
    private fun write(context: Context, album: PhotoAlbum) {
        val json = JSONObject()
            .put("caption", album.caption)
            .put(
                "photos",
                JSONArray(
                    album.photos.map {
                        JSONObject().put("id", it.id).put("width", it.width).put("height", it.height).put("date", it.date.toString())
                    },
                ),
            )
        Vault.writeText(context, ALBUM, json.toString())
    }

    /** Imports [uris] ahead of the existing photos; the album keeps at most [MAX]. Runs off the main thread. */
    fun import(context: Context, uris: List<Uri>): PhotoAlbum {
        val album = read(context)
        val maxShort = context.resources.displayMetrics.let { min(it.widthPixels, it.heightPixels) }
        val added = uris.take(MAX).map { importOne(context, it, maxShort) }
        val kept = added + album.photos
        val next = album.copy(photos = kept.take(MAX))
        write(context, next)
        kept.drop(MAX).forEach { Vault.delete(context, photoName(it.id)) }
        return next
    }

    fun remove(context: Context, id: String): PhotoAlbum {
        val album = read(context)
        val next = album.copy(photos = album.photos.filter { it.id != id })
        write(context, next)
        album.photos.filter { it.id == id }.forEach { Vault.delete(context, photoName(it.id)) }
        return next
    }

    fun setCaption(context: Context, caption: String): PhotoAlbum =
        read(context).copy(caption = caption.trim()).also { write(context, it) }

    private fun importOne(context: Context, uri: Uri, maxShort: Int): Photo {
        val exif = context.contentResolver.openInputStream(uri).use { ExifInterface(checkNotNull(it)) }
        val upright = decodeUpright(context, uri, maxShort, exif)
        val id = UUID.randomUUID().toString()
        val jpeg = ByteArrayOutputStream().also { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        Vault.write(context, photoName(id), jpeg)
        val photo = Photo(id, upright.width, upright.height, dateOf(context, uri, exif))
        upright.recycle()
        return photo
    }

    /**
     * Imports one picked image as a widget's photo surface and returns its
     * id. Styles (drafts and placed widgets alike) refer to it by that id and
     * outlive any album change, so it is named by its content and never
     * deleted: picking the same image again reuses it. Runs off the main thread.
     */
    fun importBackground(context: Context, uri: Uri): String {
        val maxShort = context.resources.displayMetrics.let { min(it.widthPixels, it.heightPixels) }
        val exif = context.contentResolver.openInputStream(uri).use { ExifInterface(checkNotNull(it)) }
        val upright = decodeUpright(context, uri, maxShort, exif)
        val bytes = ByteArrayOutputStream().also { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        upright.recycle()
        val id = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }.take(32)
        if (!Vault.exists(context, backgroundName(id))) Vault.write(context, backgroundName(id), bytes)
        return id
    }

    /** Decodes [uri] once at the stored size ([PhotoMath.storedSize]) and turns it upright from EXIF. */
    private fun decodeUpright(context: Context, uri: Uri, maxShort: Int, exif: ExifInterface): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "$uri is not a decodable image" }
        val (targetW, targetH) = PhotoMath.storedSize(bounds.outWidth, bounds.outHeight, maxShort)
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoMath.sampleSize(bounds.outWidth, bounds.outHeight, targetW, targetH)
        }
        val decoded = checkNotNull(resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }) {
            "$uri could not be decoded"
        }
        val m = Matrix().apply {
            postScale(targetW.toFloat() / decoded.width, targetH.toFloat() / decoded.height)
            postRotate(rotation(exif))
        }
        val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
        if (upright !== decoded) decoded.recycle()
        return upright
    }

    private fun rotation(exif: ExifInterface): Float = when (
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    /** When the photo was taken: EXIF, then the media store, then today (screenshots and edits carry neither). */
    private fun dateOf(context: Context, uri: Uri, exif: ExifInterface): LocalDate {
        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { raw ->
            runCatching { LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")).toLocalDate() }
                .getOrNull()?.let { return it }
        }
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN), null, null, null)?.use { c ->
            val col = c.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
            if (col >= 0 && c.moveToFirst() && !c.isNull(col)) {
                return Instant.ofEpochMilli(c.getLong(col)).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
        return LocalDate.now()
    }
}
