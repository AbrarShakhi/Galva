package com.abrarshakhi.galva.core.vault.store

import android.annotation.SuppressLint
import com.abrarshakhi.galva.core.vault.VaultIds
import java.io.File
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

internal class VaultFiles(val root: File) {

    private val container = File(root, CONTAINER)
    private val staging = File(root, "$CONTAINER.new")
    private val blobs = File(root, "blobs")
    private val thumbnails = File(root, "thumbs")

    fun containerExists(): Boolean = container.isFile

    fun readContainer(): ByteArray? = if (container.isFile) container.readBytes() else null

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

    fun deleteUnreferenced(ids: Set<Long>) {
        listOf(blobs, thumbnails).forEach { directory ->
            directory.listFiles()?.forEach { file ->
                val id = VaultIds.fromHex(file.name)
                if (id == null || id !in ids) file.delete()
            }
        }
    }

    @SuppressLint("UsableSpace")
    fun usableSpace(): Long {
        root.mkdirs()
        return root.usableSpace
    }

    fun deleteEverything() {
        root.deleteRecursively()
    }

    private fun syncDirectory(directory: File) {
        runCatching {
            FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) }
        }
    }

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
