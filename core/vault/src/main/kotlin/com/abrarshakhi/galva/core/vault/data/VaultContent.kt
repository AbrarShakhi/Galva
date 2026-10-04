package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.vault.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.store.VaultFiles
import com.google.crypto.tink.StreamingAead
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

internal class VaultContent(
    private val session: VaultSession,
    private val files: VaultFiles,
    private val crypto: VaultCrypto,
) {

    fun readThumbnail(id: Long): ByteArray =
        readAll(id, files.thumbnail(id), VaultContext.thumbnail(id))

    fun readOriginal(id: Long): ByteArray = readAll(id, files.blob(id), VaultContext.blob(id))

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

internal class VaultLockedException : IllegalStateException("Secrets is locked")
