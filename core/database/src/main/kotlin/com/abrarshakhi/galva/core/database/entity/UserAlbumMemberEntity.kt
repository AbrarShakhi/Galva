package com.abrarshakhi.galva.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_album_members",
    primaryKeys = ["albumId", "mediaId"],
    foreignKeys = [
        ForeignKey(
            entity = UserAlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("mediaId")],
)
data class UserAlbumMemberEntity(
    val albumId: Long,
    val mediaId: Long,
    val addedAtMs: Long,
)
