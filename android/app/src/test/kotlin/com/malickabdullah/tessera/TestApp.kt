package com.malickabdullah.tessera

import android.app.Application
import com.malickabdullah.tessera.security.DataKey
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Robolectric has no AndroidKeyStore provider; the data key is wrapped with a software key instead. */
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DataKey.wrappingKey = { wrapping }
    }

    companion object {
        private val wrapping: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }
}
