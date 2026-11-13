package com.malickabdullah.tessera.security

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM in core_crypto's `EncryptedPayload` layout — salt (32) | nonce
 * (12) | ciphertext | tag (16) — so a file sealed on either side of the
 * engine channel opens on the other. The salt is unused by AES-GCM; it is
 * kept so the layout stays byte-for-byte core_crypto's.
 */
internal object Payload {
    private const val SALT_BYTES = 32
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val HEADER = SALT_BYTES + NONCE_BYTES
    private val random = SecureRandom()

    fun seal(key: ByteArray, plain: ByteArray): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        return salt + nonce + cipher.doFinal(plain)
    }

    fun open(key: ByteArray, payload: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, payload, SALT_BYTES, NONCE_BYTES))
        return cipher.doFinal(payload, HEADER, payload.size - HEADER)
    }
}

internal object Files {
    /** Writes through a synced temp file and a rename, so a crash never leaves half a file. */
    fun writeAtomically(file: File, bytes: ByteArray) {
        file.parentFile!!.mkdirs()
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use {
            it.write(bytes)
            it.fd.sync()
        }
        check(tmp.renameTo(file)) { "Could not replace ${file.name}" }
    }
}

/**
 * Everything Tessera stores about the user, encrypted under [DataKey]. The
 * engine's documents live in `files/vault/engine`; the app's, sealed by Dart
 * with core_crypto, in `files/vault/app`.
 */
object Vault {
    @Volatile
    private var key: ByteArray? = null

    private fun root(context: Context) = File(context.filesDir, "vault")

    private fun engine(context: Context) = File(root(context), "engine")

    fun appDir(context: Context): File = File(root(context), "app")

    /**
     * The data key, once v0.3.0's plaintext has been sealed. Any entry point
     * may be first after an update (the app, a launcher update, the boot or
     * package-replaced receiver, a worker), so every read and write passes
     * through here.
     */
    fun key(context: Context): ByteArray = key ?: synchronized(this) {
        key ?: run {
            val app = context.applicationContext
            val loaded = DataKey.load(app, dataExists = root(app).walk().any { it.isFile })
            LegacyPlaintext.migrate(app, loaded)
            key = loaded
            loaded
        }
    }

    fun read(context: Context, name: String): ByteArray? {
        val key = key(context)
        val file = File(engine(context), name)
        return if (file.exists()) Payload.open(key, file.readBytes()) else null
    }

    fun exists(context: Context, name: String): Boolean {
        key(context)
        return File(engine(context), name).exists()
    }

    fun write(context: Context, name: String, bytes: ByteArray) = put(context, key(context), name, bytes)

    fun delete(context: Context, name: String) {
        key(context)
        File(engine(context), name).delete()
    }

    fun readText(context: Context, name: String): String? = read(context, name)?.toString(Charsets.UTF_8)

    fun writeText(context: Context, name: String, text: String) = write(context, name, text.toByteArray(Charsets.UTF_8))

    /** Writes with an explicit key: the migration runs inside [key] and must not re-enter it. */
    internal fun put(context: Context, key: ByteArray, name: String, bytes: ByteArray) {
        Files.writeAtomically(File(engine(context), name), Payload.seal(key, bytes))
    }
}
