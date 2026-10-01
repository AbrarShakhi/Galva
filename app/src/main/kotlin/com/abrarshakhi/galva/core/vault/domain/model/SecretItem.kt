package com.abrarshakhi.galva.core.vault.domain.model

import com.abrarshakhi.galva.core.media.domain.model.MediaType

/**
 * One photo or video held in the vault.
 *
 * Everything here was decrypted from the vault's index at unlock time and exists only in memory.
 * [id] is random rather than the original MediaStore id, so nothing on disk — file names
 * included — says which photo a vault file used to be.
 */
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
    /**
     * Set while the original may still be in the gallery: the item was committed to the vault but
     * the system delete dialog for the original was never confirmed.
     */
    val pendingOriginal: PendingOriginal?,
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}

/** The gallery copy a move has not yet removed. */
data class PendingOriginal(
    val mediaId: Long,
    val uri: String,
)
