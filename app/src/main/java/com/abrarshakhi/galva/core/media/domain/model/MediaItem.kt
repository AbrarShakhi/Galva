package com.abrarshakhi.galva.core.media.domain.model

data class MediaItem(
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
    val isFavorite: Boolean,
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO

    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
}
