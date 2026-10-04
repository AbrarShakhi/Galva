package com.abrarshakhi.galva.core.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.abrarshakhi.galva.core.model.MediaType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class MediaStoreDataSource(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher,
) : MediaStoreReader {

    override suspend fun queryFingerprints(): List<MediaStoreFingerprint> = withContext(dispatcher) {
        query(projection = FINGERPRINT_PROJECTION, selection = MEDIA_TYPE_SELECTION) { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val modifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    add(
                        MediaStoreFingerprint(
                            id = cursor.getLong(idColumn),
                            dateModifiedMs = cursor.getLong(modifiedColumn) * MILLIS_PER_SECOND,
                        )
                    )
                }
            }
        }
    }

    override suspend fun queryByIds(ids: List<Long>): List<MediaStoreRecord> = withContext(dispatcher) {
        ids.chunked(ID_CHUNK).flatMap { chunk ->
            val placeholders = chunk.joinToString(separator = ",") { "?" }
            query(
                projection = FULL_PROJECTION,
                selection = "$MEDIA_TYPE_SELECTION AND ${MediaStore.Files.FileColumns._ID} IN ($placeholders)",
                selectionArgs = MEDIA_TYPE_ARGS + chunk.map(Long::toString),
                block = ::readRecords,
            )
        }
    }

    private fun readRecords(cursor: Cursor): List<MediaStoreRecord> {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
        val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
        val modifiedColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
        val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
        val bucketNameColumn =
            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
        val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

        return buildList(cursor.count) {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val type = when (cursor.getInt(mediaTypeColumn)) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> MediaType.VIDEO
                    else -> MediaType.IMAGE
                }
                val dateModifiedMs = cursor.getLong(modifiedColumn) * MILLIS_PER_SECOND
                val dateTakenMs = cursor.takeIf { !it.isNull(takenColumn) }
                    ?.getLong(takenColumn)
                    ?.takeIf { it > 0L }
                    ?: dateModifiedMs

                add(
                    MediaStoreRecord(
                        id = id,
                        uri = contentUriFor(id, type).toString(),
                        displayName = cursor.getStringOrNull(nameColumn) ?: UNTITLED,
                        mimeType = cursor.getStringOrNull(mimeColumn) ?: type.fallbackMimeType(),
                        type = type,
                        sizeBytes = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        durationMs = if (type == MediaType.VIDEO) cursor.getLong(durationColumn) else 0L,
                        dateTakenMs = dateTakenMs,
                        dateModifiedMs = dateModifiedMs,
                        albumId = cursor.getLong(bucketIdColumn),
                        albumName = cursor.getStringOrNull(bucketNameColumn) ?: UNKNOWN_ALBUM,
                    )
                )
            }
        }
    }

    private fun contentUriFor(id: Long, type: MediaType): Uri {
        val collection = when (type) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        return ContentUris.withAppendedId(collection, id)
    }

    private fun <T> query(
        projection: Array<String>,
        selection: String,
        selectionArgs: List<String> = MEDIA_TYPE_ARGS,
        block: (Cursor) -> List<T>,
    ): List<T> = context.contentResolver.query(
        COLLECTION,
        projection,
        selection,
        selectionArgs.toTypedArray(),
        "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
    )?.use(block).orEmpty()

    private fun Cursor.getStringOrNull(column: Int): String? =
        if (isNull(column)) null else getString(column)

    private fun MediaType.fallbackMimeType(): String = when (this) {
        MediaType.IMAGE -> "image/*"
        MediaType.VIDEO -> "video/*"
    }

    private companion object {
        val COLLECTION: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        const val MILLIS_PER_SECOND = 1_000L
        const val ID_CHUNK = 400
        const val UNTITLED = "Untitled"
        const val UNKNOWN_ALBUM = "Unknown"

        const val MEDIA_TYPE_SELECTION = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (?, ?)"

        val MEDIA_TYPE_ARGS = listOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )

        val FINGERPRINT_PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )

        val FULL_PROJECTION = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
        )
    }
}
