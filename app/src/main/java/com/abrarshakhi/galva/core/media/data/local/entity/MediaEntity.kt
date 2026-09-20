package com.abrarshakhi.galva.core.media.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abrarshakhi.galva.core.media.domain.model.MediaType

/**
 * A mirror of one MediaStore row.
 *
 * [id] is the MediaStore `_ID`, which lets the sync pass diff against MediaStore without keeping a
 * second identity. [dateModifiedMs] doubles as the sync fingerprint: when it differs from the
 * value MediaStore reports, the row is re-read.
 */
@Entity(
    tableName = "media",
    indices = [
        Index("dateTakenMs"),
        Index("albumId"),
        Index("type"),
    ],
)
data class MediaEntity(
    @PrimaryKey val id: Long,
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
)
