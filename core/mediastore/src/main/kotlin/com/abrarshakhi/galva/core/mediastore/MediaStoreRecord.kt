package com.abrarshakhi.galva.core.mediastore

import com.abrarshakhi.galva.core.model.MediaType

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

data class MediaStoreFingerprint(
    val id: Long,
    val dateModifiedMs: Long,
)
