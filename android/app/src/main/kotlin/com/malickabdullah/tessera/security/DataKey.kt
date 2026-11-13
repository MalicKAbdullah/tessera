package com.malickabdullah.tessera.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The random 256-bit key that encrypts everything Tessera stores. Widgets
 * draw without the Flutter engine (boot, ticks, launcher updates), so the key
 * is owned here rather than by a Dart plugin: it is wrapped by a
 * non-exportable AES key in the Android Keystore and the wrapped bytes live in
 * no_backup. The app's Dart side receives it over the engine channel and
 * encrypts its own documents with core_crypto.
 */
internal object DataKey {
    private const val ALIAS = "tessera.data"
    private const val WRAPPED = "data.key"
    private const val KEY_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    /** Robolectric has no AndroidKeyStore provider, so unit tests install a software key here. */
    var wrappingKey: () -> SecretKey = ::keystoreKey

    /**
     * The stored key, or a new one when nothing has been encrypted yet.
     * [dataExists] tells a first start from a lost key: data sealed with the
     * old key can never be read with a new one, so that throws [DataKeyLost].
     */
    fun load(context: Context, dataExists: Boolean): ByteArray {
        val file = File(context.noBackupFilesDir, WRAPPED)
        if (file.exists()) return unwrap(file.readBytes())
        if (dataExists) throw DataKeyLost()
        val key = ByteArray(KEY_BYTES).also { SecureRandom().nextBytes(it) }
        Files.writeAtomically(file, wrap(key))
        return key
    }

    private fun wrap(key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Keystore keys refuse caller-chosen nonces; the cipher picks one.
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        check(cipher.iv.size == NONCE_BYTES) { "Unexpected GCM nonce length ${cipher.iv.size}" }
        return cipher.iv + cipher.doFinal(key)
    }

    private fun unwrap(wrapped: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(TAG_BITS, wrapped, 0, NONCE_BYTES))
        return cipher.doFinal(wrapped, NONCE_BYTES, wrapped.size - NONCE_BYTES)
    }

    private fun keystoreKey(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(ALIAS, null) as SecretKey?)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}

/** The data key is gone while data sealed with it is still on the phone (the Keystore was reset). */
class DataKeyLost : IllegalStateException("The data key is gone; data sealed with it can never be read again")
