package com.abrarshakhi.galva.core.vault.data.crypto

import com.abrarshakhi.galva.core.vault.domain.model.VaultIds
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.streamingaead.PredefinedStreamingAeadParameters
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf
import java.security.SecureRandom

/**
 * The vault's use of Tink, kept in one place.
 *
 * Raw 256-bit keys — the master key, each epoch's container key, and the keys derived from the
 * passphrase and recovery phrase — are used with AES-256-GCM. Each file gets a streaming keyset
 * of its own: forgetting that one keyset shreds that one file, and the segmented format is what
 * lets a video seek without decrypting everything before the playhead.
 */
class VaultCrypto {

    private val random = SecureRandom()

    init {
        StreamingAeadConfig.register()
    }

    fun newKey(): ByteArray = randomBytes(KEY_BYTES)

    fun randomBytes(count: Int): ByteArray = ByteArray(count).also(random::nextBytes)

    fun randomId(): Long = random.nextLong()

    /**
     * AES-256-GCM. [context] is authenticated but not encrypted: it pins what a ciphertext is
     * for.
     */
    fun seal(key: ByteArray, plaintext: ByteArray, context: ByteArray): ByteArray =
        AesGcmJce(key).encrypt(plaintext, context)

    /** @throws java.security.GeneralSecurityException when the key or context does not match. */
    fun open(key: ByteArray, ciphertext: ByteArray, context: ByteArray): ByteArray =
        AesGcmJce(key).decrypt(ciphertext, context)

    /** A serialized keyset for one file. It is only ever stored inside the encrypted index. */
    fun newFileKeyset(): ByteArray = TinkProtoKeysetFormat.serializeKeyset(
        KeysetHandle.generateNew(PredefinedStreamingAeadParameters.AES256_GCM_HKDF_1MB),
        InsecureSecretKeyAccess.get(),
        RegistryConfiguration.get(),
    )

    fun streamingAead(keyset: ByteArray): StreamingAead {
        val configuration = RegistryConfiguration.get()
        val handle =
            TinkProtoKeysetFormat.parseKeyset(keyset, InsecureSecretKeyAccess.get(), configuration)
        return handle.getPrimitive(configuration, StreamingAead::class.java)
    }

    /** HKDF-SHA256: turns one high-entropy secret into a key with a single purpose. */
    fun deriveKey(secret: ByteArray, purpose: String): ByteArray =
        Hkdf.computeHkdf("HMACSHA256", secret, ByteArray(0), purpose.toByteArray(), KEY_BYTES)

    companion object {
        const val KEY_BYTES = 32
    }
}

/**
 * Authenticated labels for every ciphertext the vault writes.
 *
 * A ciphertext only opens under the label it was sealed with, so a blob cannot be passed off as
 * another item's, and a wrapped master key cannot be replayed as the index.
 */
internal object VaultContext {

    val MasterKeyByPassphrase = "galva-vault/v1/master-key/passphrase".toByteArray()
    val MasterKeyByRecovery = "galva-vault/v1/master-key/recovery".toByteArray()
    val Index = "galva-vault/v1/index".toByteArray()
    const val RECOVERY_KEY_PURPOSE = "galva-vault/v1/recovery-key"

    fun containerKey(alias: String): ByteArray = "galva-vault/v1/container-key/$alias".toByteArray()

    fun blob(id: Long): ByteArray = "galva-vault/v1/blob/${VaultIds.toHex(id)}".toByteArray()

    fun thumbnail(id: Long): ByteArray = "galva-vault/v1/thumb/${VaultIds.toHex(id)}".toByteArray()
}

/** Overwrites key material that is no longer needed. Best effort: the JVM may hold other copies. */
fun ByteArray.wipe() = fill(0)
