package com.malickabdullah.tessera.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.AEADBadTagException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VaultTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val key = ByteArray(32) { it.toByte() }

    @Test
    fun aPayloadRoundTripsAndHidesThePlaintext() {
        val sealed = Payload.seal(key, "Buy milk".toByteArray())
        assertFalse(String(sealed, Charsets.ISO_8859_1).contains("Buy milk"))
        assertEquals("Buy milk", String(Payload.open(key, sealed)))
    }

    @Test
    fun aTamperedPayloadDoesNotOpen() {
        val sealed = Payload.seal(key, "Buy milk".toByteArray())
        sealed[sealed.size - 1] = (sealed.last() + 1).toByte()
        assertThrows(AEADBadTagException::class.java) { Payload.open(key, sealed) }
    }

    /** Sealed by core_crypto's CipherService (Dart) with the key 00 01 .. 1f. */
    @Test
    fun aCoreCryptoPayloadOpensHere() {
        val hex = "4f8a9fbcccd59b598a64227e54c8ad9ffc63c5d442ed26dae5ccec3f8b78014daf50779309ca1093d5b4bc39" +
            "bea1bb79f64d6f270b370cea707d6a4cd52a92de944d22d14466623bf5455094b5ffe23e9df2482a14cf69c9b069daa2"
        val payload = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertEquals("{\"note\":\"Sealed by core_crypto\"}", String(Payload.open(key, payload)))
    }

    @Test
    fun theDataKeyIsWrappedAndReadBackFromNoBackup() {
        val first = DataKey.load(context, dataExists = false)
        val wrapped = File(context.noBackupFilesDir, "data.key").readBytes()
        assertFalse(wrapped.toList().windowed(first.size).any { it == first.toList() })
        assertArrayEquals(first, DataKey.load(context, dataExists = true))
    }

    @Test
    fun aLostKeyWithSealedDataFailsInsteadOfStartingOver() {
        assertThrows(DataKeyLost::class.java) { DataKey.load(context, dataExists = true) }
    }

    @Test
    fun vaultDocumentsRoundTrip() {
        assertNull(Vault.readText(context, "test"))
        Vault.writeText(context, "test", "hello")
        assertEquals("hello", Vault.readText(context, "test"))
        val onDisk = File(context.filesDir, "vault/engine/test").readBytes()
        assertFalse(String(onDisk, Charsets.ISO_8859_1).contains("hello"))
    }
}
