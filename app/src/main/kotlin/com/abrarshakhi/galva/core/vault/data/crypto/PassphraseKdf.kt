package com.abrarshakhi.galva.core.vault.data.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.nio.ByteBuffer
import java.text.Normalizer

/**
 * Stretches a passphrase into a key-encryption key.
 *
 * Deliberately slow and memory-hard: anyone who copies the vault off the phone can only test
 * passphrases at the rate this allows, on hardware that pays the same memory cost for every guess.
 */
interface PassphraseKdf {

    /** Parameters for a new vault or a new passphrase, around a fresh [salt]. */
    fun newParams(salt: ByteArray): KdfParams

    fun deriveKey(passphrase: ByteArray, params: KdfParams): ByteArray
}

/** Stored with each vault, so the cost can be raised later without breaking existing vaults. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
class KdfParams(
    @ProtoNumber(1) val salt: ByteArray,
    @ProtoNumber(2) val memoryKib: Int,
    @ProtoNumber(3) val iterations: Int,
    @ProtoNumber(4) val parallelism: Int,
)

/** Argon2id via the reference C implementation. */
class Argon2idKdf(private val lowRamDevice: Boolean) : PassphraseKdf {

    private val argon2 by lazy { Argon2Kt() }

    override fun newParams(salt: ByteArray): KdfParams = KdfParams(
        salt = salt,
        memoryKib = if (lowRamDevice) LOW_RAM_MEMORY_KIB else MEMORY_KIB,
        iterations = ITERATIONS,
        parallelism = PARALLELISM,
    )

    override fun deriveKey(passphrase: ByteArray, params: KdfParams): ByteArray {
        // The library copies the passphrase into a native buffer and wipes that copy itself.
        val result = argon2.hash(
            mode = Argon2Mode.ARGON2_ID,
            password = passphrase,
            salt = params.salt,
            tCostInIterations = params.iterations,
            mCostInKibibyte = params.memoryKib,
            parallelism = params.parallelism,
            hashLengthInBytes = VaultCrypto.KEY_BYTES,
            version = Argon2Version.V13,
        )
        return result.rawHashAsByteArray().also {
            result.rawHash.wipe()
            result.encodedOutput.wipe()
        }
    }

    private fun ByteBuffer.wipe() {
        runCatching {
            clear()
            while (hasRemaining()) put(0)
        }
    }

    private companion object {
        /** RFC 9106's second recommended option, which phones handle in well under a second. */
        const val MEMORY_KIB = 64 * 1024
        const val LOW_RAM_MEMORY_KIB = 32 * 1024
        const val ITERATIONS = 3
        const val PARALLELISM = 1
    }
}

/**
 * The bytes a passphrase is derived from.
 *
 * NFC-normalised so the same characters typed on a different keyboard, which may compose accents
 * differently, still produce the same key.
 */
fun CharSequence.toPassphraseBytes(): ByteArray =
    Normalizer.normalize(this, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)
