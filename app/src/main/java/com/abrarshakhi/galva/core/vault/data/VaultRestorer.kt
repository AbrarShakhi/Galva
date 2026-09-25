package com.abrarshakhi.galva.core.vault.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.abrarshakhi.galva.core.media.domain.model.MediaType
import com.abrarshakhi.galva.core.vault.data.store.SecretEntry
import java.io.IOException
import java.io.OutputStream

/**
 * Puts decrypted items back into the gallery, in the folder they came from where possible.
 *
 * Each item is inserted as pending so other apps never see a half-written file, and a failed write
 * deletes its row — the vault copy is only destroyed once the gallery copy is complete.
 */
class VaultRestorer(private val context: Context) : SecretExporter {

    override fun export(entry: SecretEntry, writePlaintext: (OutputStream) -> Unit): Long {
        val isVideo = entry.mediaType == MediaType.VIDEO
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val uri = insertPending(collection, entry, entry.relativePath)
            ?: insertPending(collection, entry, fallbackFolder(isVideo))
            ?: throw IOException("The gallery would not accept ${entry.displayName}")

        val resolver = context.contentResolver
        try {
            val out = resolver.openOutputStream(uri)
                ?: throw IOException("Couldn't write ${entry.displayName}")
            out.use(writePlaintext)
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            return ContentUris.parseId(uri)
        } catch (error: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw error
        }
    }

    /**
     * Null when the collection refuses [relativePath] — an image cannot be written into
     * `Movies/`, for example, and a folder that held it before may no longer be allowed.
     */
    private fun insertPending(collection: Uri, entry: SecretEntry, relativePath: String?): Uri? {
        if (relativePath.isNullOrBlank()) return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entry.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, entry.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (entry.dateTakenMs > 0) put(MediaStore.MediaColumns.DATE_TAKEN, entry.dateTakenMs)
        }
        return try {
            context.contentResolver.insert(collection, values)
        } catch (refused: IllegalArgumentException) {
            null
        }
    }

    private fun fallbackFolder(isVideo: Boolean): String {
        val base = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        return "$base/$FOLDER_NAME"
    }

    private companion object {
        const val FOLDER_NAME = "Galva"
    }
}
