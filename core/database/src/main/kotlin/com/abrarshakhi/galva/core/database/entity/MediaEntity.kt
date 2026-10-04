package com.abrarshakhi.galva.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.abrarshakhi.galva.core.model.MediaType

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
