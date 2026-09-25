package com.abrarshakhi.galva.features.secrets.presentation

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem
import com.abrarshakhi.galva.core.vault.domain.model.VaultUri

/**
 * Vault items in the shape the shared grid and viewer components take.
 *
 * The `galva-vault://` URI is what keeps them apart from gallery items: only the vault's own Coil
 * fetcher and Media3 data source can resolve it, and share and MediaStore delete cannot.
 */
fun SecretItem.toGridItem(): MediaItem = toMediaItem(VaultUri.thumbnail(id))

/** Photos decode the whole original; a video shows its thumbnail until its page starts playing. */
fun SecretItem.toViewerItem(): MediaItem =
    toMediaItem(if (isVideo) VaultUri.thumbnail(id) else VaultUri.full(id))

private fun SecretItem.toMediaItem(uri: String): MediaItem = MediaItem(
    id = id,
    uri = uri,
    displayName = displayName,
    mimeType = mimeType,
    type = type,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs,
    dateTakenMs = dateTakenMs,
    dateModifiedMs = addedAtMs,
    albumId = 0L,
    albumName = "Secrets",
    isFavorite = false,
)
