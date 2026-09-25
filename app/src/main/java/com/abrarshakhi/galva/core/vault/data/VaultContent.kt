package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.vault.data.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.google.crypto.tink.StreamingAead
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

/**
 * Decrypting reads for the UI: whole images for Coil, a seekable stream for Media3.
 *
 * Plaintext only ever exists in memory — nothing here writes to disk — and every read needs the
 * vault unlocked, so a request that arrives after the vault locks fails instead of decrypting.
 */
class VaultContent(
    private val session: VaultSession,
    private val files: VaultFiles,
    private val crypto: VaultCrypto,
) {

    fun readThumbnail(id: Long): ByteArray =
        readAll(id, files.thumbnail(id), VaultContext.thumbnail(id))

    fun readOriginal(id: Long): ByteArray = readAll(id, files.blob(id), VaultContext.blob(id))

    /**
     * Random access into the original, so video can seek without decrypting from the start.
     *
     * The channel is primed with a one-byte read: Tink only settles on the key after the first
     * read, and cannot report [SeekableByteChannel.size] before it — which Media3 needs up front.
     * It also means a damaged file fails here rather than part-way through playback.
     */
    fun openOriginal(id: Long): SeekableByteChannel {
        val aead = aeadFor(id)
        val ciphertext = FileInputStream(files.blob(id)).channel
        return try {
            aead.newSeekableDecryptingChannel(ciphertext, VaultContext.blob(id)).apply {
                read(ByteBuffer.allocate(1))
                position(0)
            }
        } catch (error: Exception) {
            ciphertext.close()
            throw error
        }
    }

    private fun readAll(id: Long, file: File, context: ByteArray): ByteArray =
        aeadFor(id).newDecryptingStream(FileInputStream(file), context).use { it.readBytes() }

    private fun aeadFor(id: Long): StreamingAead {
        val entry = session.current?.entry(id) ?: throw VaultLockedException()
        return crypto.streamingAead(entry.keyset)
    }
}

class VaultLockedException : IllegalStateException("Secrets is locked")
