package com.abrarshakhi.galva.core.vault.data.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * [HardwareKeyRing] on the Android Keystore: non-exportable AES-256-GCM keys generated in StrongBox
 * where the device has it, and in the TEE otherwise.
 *
 * Only 32-byte keys are ever wrapped here; StrongBox is far too slow for bulk data. The keys are
 * bound to neither user authentication nor an unlocked device. Both tie a key's life to the lock
 * screen, and a user removing their screen lock must not silently destroy a vault that their
 * passphrase and recovery phrase should still open. The vault is sealed while the phone is locked
 * anyway: it locks when the app stops, and its master key only ever exists in memory.
 */
class AndroidKeystoreKeyRing(private val context: Context) : HardwareKeyRing {

    private val keyStore: KeyStore by lazy { KeyStore.getInstance(PROVIDER).apply { load(null) } }
    private val random = SecureRandom()

    override fun createKey(): String {
        val alias = ALIAS_PREFIX + ByteArray(ALIAS_RANDOM_BYTES).also(random::nextBytes).toHex()
        val backings = if (hasStrongBox()) listOf(true, false) else listOf(false)
        var failure: Exception? = null
        for (strongBox in backings) {
            try {
                generate(alias, strongBox)
                // Some secure elements generate keys they then fail to use. Find out now, not when
                // the vault's only copy of its container key is wrapped with it.
                check(unwrap(alias, wrap(alias, PROBE, PROBE), PROBE).contentEquals(PROBE))
                return alias
            } catch (error: GeneralSecurityException) {
                failure = error
            } catch (error: ProviderException) {
                failure = error
            } catch (error: IllegalStateException) {
                failure = error
            }
            runCatching { keyStore.deleteEntry(alias) }
        }
        throw KeyUnavailableException("Could not create a hardware-backed key", failure)
    }

    override fun wrap(alias: String, key: ByteArray, context: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(alias))
        cipher.updateAAD(context)
        val sealed = cipher.doFinal(key)
        val iv = cipher.iv
        return byteArrayOf(iv.size.toByte()) + iv + sealed
    }

    override fun unwrap(alias: String, wrapped: ByteArray, context: ByteArray): ByteArray {
        val ivLength = wrapped.firstOrNull()?.toInt()
            ?.takeIf { it > 0 && wrapped.size > 1 + it }
            ?: throw GeneralSecurityException("Malformed wrapped key")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(alias),
            GCMParameterSpec(TAG_BITS, wrapped, 1, ivLength),
        )
        cipher.updateAAD(context)
        return cipher.doFinal(wrapped, 1 + ivLength, wrapped.size - 1 - ivLength)
    }

    override fun delete(alias: String) {
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
    }

    override fun aliases(): Set<String> =
        keyStore.aliases().toList().filterTo(HashSet()) { it.startsWith(ALIAS_PREFIX) }

    private fun secretKey(alias: String): SecretKey {
        val key = try {
            keyStore.getKey(alias, null)
        } catch (error: GeneralSecurityException) {
            throw KeyUnavailableException("The vault's hardware key cannot be read", error)
        }
        return key as? SecretKey
            ?: throw KeyUnavailableException("The vault's hardware key is gone")
    }

    private fun generate(alias: String, strongBox: Boolean) {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setIsStrongBoxBacked(strongBox)
            .build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    private fun hasStrongBox(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val ALIAS_PREFIX = "galva.vault.epoch."
        const val ALIAS_RANDOM_BYTES = 16
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        val PROBE = "galva-vault/v1/probe".toByteArray()
    }
}
