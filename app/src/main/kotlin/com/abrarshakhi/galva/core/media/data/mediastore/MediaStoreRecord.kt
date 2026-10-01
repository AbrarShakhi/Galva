package com.abrarshakhi.galva.core.media.data.mediastore

import com.abrarshakhi.galva.core.media.domain.model.MediaType

/**
 * One row as MediaStore reports it, before it is mapped into the local index.
 *
 * Keeping this distinct from the Room entity means the query code owns MediaStore's quirks — unit
 * mismatches, nullable buckets — and the rest of the data layer never has to know about them.
 */
data class MediaStoreRecord(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val type: MediaType,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateTakenMs: Long,
    val dateModifiedMs: Long,
    val albumId: Long,
    val albumName: String,
)
