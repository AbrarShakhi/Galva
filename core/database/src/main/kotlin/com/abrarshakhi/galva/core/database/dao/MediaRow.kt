package com.abrarshakhi.galva.core.database.dao

import androidx.room.Embedded
import com.abrarshakhi.galva.core.database.entity.MediaEntity

data class MediaRow(
    @Embedded val media: MediaEntity,
    val isFavorite: Boolean,
)

data class MediaFingerprint(
    val id: Long,
    val dateModifiedMs: Long,
)

data class AlbumRow(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
    val lastModifiedMs: Long,
)
