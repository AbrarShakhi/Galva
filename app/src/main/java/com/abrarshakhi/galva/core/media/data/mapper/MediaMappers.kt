package com.abrarshakhi.galva.core.media.data.mapper

import com.abrarshakhi.galva.core.media.data.local.dao.AlbumRow
import com.abrarshakhi.galva.core.media.data.local.dao.MediaRow
import com.abrarshakhi.galva.core.media.data.local.entity.MediaEntity
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreRecord
import com.abrarshakhi.galva.core.media.data.local.dao.UserAlbumRow
import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.MediaItem

fun MediaStoreRecord.toEntity(): MediaEntity = MediaEntity(
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

fun MediaRow.toDomain(): MediaItem = MediaItem(
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

fun AlbumRow.toDomain(): Album = Album(
    ref = AlbumRef.Device(bucketId = id),
    name = name,
    itemCount = itemCount,
    coverUri = coverUri,
    lastModifiedMs = lastModifiedMs,
)

fun UserAlbumRow.toDomain(): Album = Album(
    ref = AlbumRef.User(id = id),
    name = name,
    itemCount = itemCount,
    coverUri = coverUri,
    // A brand new album has no items to date it, so it sorts by when it was made.
    lastModifiedMs = newestItemMs ?: createdAtMs,
)
