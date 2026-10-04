package com.abrarshakhi.galva.core.data.mapper

import com.abrarshakhi.galva.core.database.dao.AlbumRow
import com.abrarshakhi.galva.core.database.dao.MediaRow
import com.abrarshakhi.galva.core.database.dao.UserAlbumRow
import com.abrarshakhi.galva.core.database.entity.MediaEntity
import com.abrarshakhi.galva.core.mediastore.MediaStoreRecord
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.MediaItem

internal fun MediaStoreRecord.toEntity(): MediaEntity = MediaEntity(
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
    dateModifiedMs = dateModifiedMs,
    albumId = albumId,
    albumName = albumName,
)

internal fun MediaRow.toDomain(): MediaItem = MediaItem(
    id = media.id,
    uri = media.uri,
    displayName = media.displayName,
    mimeType = media.mimeType,
    type = media.type,
    sizeBytes = media.sizeBytes,
    width = media.width,
    height = media.height,
    durationMs = media.durationMs,
    dateTakenMs = media.dateTakenMs,
    dateModifiedMs = media.dateModifiedMs,
    albumId = media.albumId,
    albumName = media.albumName,
    isFavorite = isFavorite,
)

internal fun AlbumRow.toDomain(): Album = Album(
    ref = AlbumRef.Device(bucketId = id),
    name = name,
    itemCount = itemCount,
    coverUri = coverUri,
    lastModifiedMs = lastModifiedMs,
)

internal fun UserAlbumRow.toDomain(): Album = Album(
    ref = AlbumRef.User(id = id),
    name = name,
    itemCount = itemCount,
    coverUri = coverUri,
    lastModifiedMs = newestItemMs ?: createdAtMs,
)
