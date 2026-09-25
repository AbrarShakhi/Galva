package com.abrarshakhi.galva.core.media.domain.model

/**
 * A single photo or video that exists on the device.
 *
 * [uri] is the string form of a MediaStore content URI. It is kept as a String so the domain layer
 * stays free of Android types; the UI parses it back into a `Uri` when handing it to Coil, Media3
 * or an Intent.
 *
 * The Secrets screens also use this type, to reuse the shared grid and viewer, with a
 * `galva-vault://` [uri] (see `VaultUri`) that only the vault's own fetcher and data source
 * resolve. Such items must never reach share or MediaStore delete.
 */
data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val type: MediaType,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    /** Playback length in milliseconds. Always 0 for [MediaType.IMAGE]. */
    val durationMs: Long,
    /** Best-effort capture time: DATE_TAKEN when MediaStore has it, DATE_MODIFIED otherwise. */
    val dateTakenMs: Long,
    val dateModifiedMs: Long,
    val albumId: Long,
    val albumName: String,
    val isFavorite: Boolean,
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO

    /** Width / height, guarding against the zeroes MediaStore reports for some rows. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
}
