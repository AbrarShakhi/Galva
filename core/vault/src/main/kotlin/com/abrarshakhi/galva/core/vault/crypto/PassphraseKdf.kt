package com.abrarshakhi.galva.core.vault.crypto

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.nio.ByteBuffer
import java.text.Normalizer

internal interface PassphraseKdf {

    fun newParams(salt: ByteArray): KdfParams

    fun deriveKey(passphrase: ByteArray, params: KdfParams): ByteArray
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal class KdfParams(
    @ProtoNumber(1) val salt: ByteArray,
    @ProtoNumber(2) val memoryKib: Int,
    @ProtoNumber(3) val iterations: Int,
    @ProtoNumber(4) val parallelism: Int,
)

internal class Argon2idKdf(private val lowRamDevice: Boolean) : PassphraseKdf {

    private val argon2 by lazy { Argon2Kt() }

    override fun newParams(salt: ByteArray): KdfParams = KdfParams(
        salt = salt,
        memoryKib = if (lowRamDevice) LOW_RAM_MEMORY_KIB else MEMORY_KIB,
        iterations = ITERATIONS,
        parallelism = PARALLELISM,
    )

    override fun deriveKey(passphrase: ByteArray, params: KdfParams): ByteArray {
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
        const val MEMORY_KIB = 64 * 1024
        const val LOW_RAM_MEMORY_KIB = 32 * 1024
        const val ITERATIONS = 3
        const val PARALLELISM = 1
    }
}

internal fun CharSequence.toPassphraseBytes(): ByteArray =
    Normalizer.normalize(this, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)
