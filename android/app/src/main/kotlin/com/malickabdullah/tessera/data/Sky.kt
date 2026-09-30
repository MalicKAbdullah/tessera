package com.malickabdullah.tessera.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager

/** Where the sky is computed for; [name] is the weather city, or null for the device's own last fix. */
data class SkyPlace(val name: String?, val latitude: Double, val longitude: Double)

/**
 * The weather city when one is set, else the last coarse fix if the user has
 * already granted coarse location. Tessera never asks for location for this:
 * without either, sky designs draw a "set a place" state.
 */
object SkyPlaceSource : DataSource<SkyPlace?> {
    override fun read(context: Context): SkyPlace? {
        ContentSource.read(context).city?.let { return SkyPlace(it.name, it.latitude, it.longitude) }
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val manager = context.getSystemService(LocationManager::class.java)
        val fix = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { it in manager.allProviders }
            .mapNotNull { manager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?: return null
        return SkyPlace(null, fix.latitude, fix.longitude)
    }
}
