package com.abrarshakhi.galva.core.vault.data.store

import android.annotation.SuppressLint
import com.abrarshakhi.galva.core.vault.domain.model.VaultIds
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

/**
 * Where the vault lives: one container file, plus a ciphertext blob and thumbnail per item.
 *
 * [root] sits inside `noBackupFilesDir`, which Android never copies into cloud backups or
 * device-to-device transfers. A copy would be useless anyway — the container's outer key never
 * leaves this phone's Keystore — but a copy would also be a second place a weak passphrase could
 * be attacked from.
 */
class VaultFiles(val root: File) {

    private val container = File(root, CONTAINER)
    private val staging = File(root, "$CONTAINER.new")
    private val blobs = File(root, "blobs")
    private val thumbnails = File(root, "thumbs")

    fun containerExists(): Boolean = container.isFile

    fun readContainer(): ByteArray? = if (container.isFile) container.readBytes() else null

    /** Replaces the container atomically: a crash leaves the old file or the new one intact. */
    fun writeContainer(bytes: ByteArray) {
        root.mkdirs()
        FileOutputStream(staging).use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        if (!staging.renameTo(container)) throw IOException("Could not replace the vault file")
        syncDirectory(root)
    }

    fun blob(id: Long): File = File(blobs, VaultIds.toHex(id))

    fun thumbnail(id: Long): File = File(thumbnails, VaultIds.toHex(id))

    /**
     * Writes [target] through a temporary sibling, so a half-written file never carries an item's
     * name. [write] may close the stream it is given; the file is synced before it is renamed.
     */
    fun writeAtomically(target: File, write: (OutputStream) -> Unit) {
        val directory = checkNotNull(target.parentFile).apply { mkdirs() }
        val temp = File(directory, target.name + PARTIAL_SUFFIX)
        try {
            FileOutputStream(temp).use { out ->
                write(NonClosingOutputStream(out))
                out.fd.sync()
            }
            if (!temp.renameTo(target)) throw IOException("Could not write ${target.name}")
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
    }

    fun deleteItem(id: Long) {
        blob(id).delete()
        thumbnail(id).delete()
    }

    /**
     * Deletes files no index entry accounts for — the leftovers of an interrupted import or
     * delete. Their keys were never committed, or have already been destroyed, so they are noise.
     */
    fun deleteUnreferenced(ids: Set<Long>) {
        listOf(blobs, thumbnails).forEach { directory ->
            directory.listFiles()?.forEach { file ->
                val id = VaultIds.fromHex(file.name)
                if (id == null || id !in ids) file.delete()
            }
        }
    }

    /**
     * Space available right now. Deliberately not `StorageManager.getAllocatableBytes`: that counts
     * cache the system *could* clear, and a move that runs out of room half-way is worse than one
     * that is refused up front.
     */
    @SuppressLint("UsableSpace")
    fun usableSpace(): Long {
        root.mkdirs()
        return root.usableSpace
    }

    fun deleteEverything() {
        root.deleteRecursively()
    }

    /** Makes the rename itself durable, not just the file's contents. Best effort. */
    private fun syncDirectory(directory: File) {
        runCatching {
            FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) }
        }
    }

    /** Lets an encrypting stream be closed without closing the file before it is synced. */
    private class NonClosingOutputStream(
        private val file: OutputStream,
    ) : FilterOutputStream(file) {

        override fun write(b: ByteArray, off: Int, len: Int) = file.write(b, off, len)

        override fun close() = file.flush()
    }

    private companion object {
        const val CONTAINER = "vault.bin"
        const val PARTIAL_SUFFIX = ".part"
    }
}
