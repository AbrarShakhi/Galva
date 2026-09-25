package com.abrarshakhi.galva.core.vault.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.core.net.toUri
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreThumbnails
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.vault.data.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.crypto.wipe
import com.abrarshakhi.galva.core.vault.data.store.SecretEntry
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.google.crypto.tink.StreamingAead
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

/**
 * Encrypts gallery items into the vault's files.
 *
 * The original is streamed from the content resolver straight into the encrypting stream, and the
 * thumbnail is compressed in memory, so no plaintext copy ever touches disk. The thumbnail is made
 * now because this is the last moment the original is readable: once it is deleted, the vault's
 * own copy is the only source left.
 */
class VaultImporter(
    private val context: Context,
    private val files: VaultFiles,
    private val crypto: VaultCrypto,
) : SecretEncryptor {

    override fun encrypt(item: MediaItem, id: Long, addedAtMs: Long): SecretEntry {
        val keyset = crypto.newFileKeyset()
        val aead = crypto.streamingAead(keyset)
        val uri = item.uri.toUri()
        try {
            files.writeAtomically(files.blob(id)) { out ->
                aead.newEncryptingStream(out, VaultContext.blob(id)).use { encrypted ->
                    val original = context.contentResolver.openInputStream(uri)
                        ?: throw FileNotFoundException("${item.displayName} is no longer available")
                    original.use { it.copyTo(encrypted, COPY_BUFFER_BYTES) }
                }
            }
            writeThumbnail(aead, uri, item, id)
        } catch (error: Exception) {
            files.deleteItem(id)
            keyset.wipe()
            throw error
        }
        return SecretEntry(
            id = id,
            keyset = keyset,
            type = item.type.name,
            mimeType = item.mimeType,
            displayName = item.displayName,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            durationMs = item.durationMs,
            dateTakenMs = item.dateTakenMs,
            addedAtMs = addedAtMs,
            relativePath = relativePathOf(uri),
            wasFavorite = item.isFavorite,
            pendingOriginalId = item.id,
            pendingOriginalUri = item.uri,
        )
    }

    override fun originalExists(uri: String): Boolean = runCatching {
        context.contentResolver
            .query(uri.toUri(), arrayOf(MediaStore.MediaColumns._ID), null, null, null)
            ?.use { it.moveToFirst() }
            ?: true
    }.getOrDefault(true)

    /** Best effort: an item without a thumbnail still opens, it just shows blank in the grid. */
    private fun writeThumbnail(aead: StreamingAead, uri: Uri, item: MediaItem, id: Long) {
        val bitmap = thumbnailOf(uri, item) ?: return
        val jpeg = ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
        }.toByteArray()
        bitmap.recycle()
        try {
            files.writeAtomically(files.thumbnail(id)) { out ->
                aead.newEncryptingStream(out, VaultContext.thumbnail(id)).use { it.write(jpeg) }
            }
        } finally {
            jpeg.wipe()
        }
    }

    private fun thumbnailOf(uri: Uri, item: MediaItem): Bitmap? {
        val resolver = context.contentResolver
        return MediaStoreThumbnails.cached(resolver, uri, THUMBNAIL_SIZE)
            ?: if (item.isVideo) videoFrameOf(uri)
            else MediaStoreThumbnails.decodeSubsampled(resolver, uri, THUMBNAIL_SIZE)
    }

    private fun videoFrameOf(uri: Uri): Bitmap? = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            retriever.getScaledFrameAtTime(
                FIRST_FRAME,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                THUMBNAIL_SIZE.width,
                THUMBNAIL_SIZE.height,
            )
        }
    }.getOrNull()

    private fun relativePathOf(uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    private companion object {
        val THUMBNAIL_SIZE = Size(512, 512)
        const val THUMBNAIL_QUALITY = 85
        const val FIRST_FRAME = -1L
        const val COPY_BUFFER_BYTES = 256 * 1024
    }
}
