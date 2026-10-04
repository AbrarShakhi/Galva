package com.abrarshakhi.galva.core.model

data class SecretItem(
    val id: Long,
    val type: MediaType,
    val mimeType: String,
    val displayName: String,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateTakenMs: Long,
    val addedAtMs: Long,
    val pendingOriginal: PendingOriginal?,
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}

data class PendingOriginal(
    val mediaId: Long,
    val uri: String,
)
