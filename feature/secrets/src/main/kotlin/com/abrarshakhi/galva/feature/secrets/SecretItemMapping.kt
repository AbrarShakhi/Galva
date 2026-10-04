package com.abrarshakhi.galva.feature.secrets

import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.SecretItem
import com.abrarshakhi.galva.core.vault.VaultUri

const val SECRET_NAMESPACE = "secret"

fun SecretItem.toGridItem(): MediaItem = toMediaItem(VaultUri.thumbnail(id))

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
