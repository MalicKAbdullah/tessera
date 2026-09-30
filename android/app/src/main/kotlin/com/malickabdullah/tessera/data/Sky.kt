package com.malickabdullah.tessera.data

import android.content.Context

/** Where the sky is computed for: the weather city. */
data class SkyPlace(val name: String, val latitude: Double, val longitude: Double)

/**
 * The weather city, or null before one is set; sky designs then draw a "set a
 * place" state. Tessera never uses device location.
 */
object SkyPlaceSource : DataSource<SkyPlace?> {
    override fun read(context: Context): SkyPlace? =
        ContentSource.read(context).city?.let { SkyPlace(it.name, it.latitude, it.longitude) }
}
