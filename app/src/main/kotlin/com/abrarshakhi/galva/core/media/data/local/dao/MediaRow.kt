package com.abrarshakhi.galva.core.media.data.local.dao

import androidx.room.Embedded
import com.abrarshakhi.galva.core.media.data.local.entity.MediaEntity

/** A media row joined with its favourite flag. */
data class MediaRow(
    @Embedded val media: MediaEntity,
    val isFavorite: Boolean,
)

/** Cheap (id, fingerprint) pair used to diff the local index against MediaStore. */
data class MediaFingerprint(
    val id: Long,
    val dateModifiedMs: Long,
)

/** Aggregate produced by grouping media rows per MediaStore bucket. */
data class AlbumRow(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
    val lastModifiedMs: Long,
)
