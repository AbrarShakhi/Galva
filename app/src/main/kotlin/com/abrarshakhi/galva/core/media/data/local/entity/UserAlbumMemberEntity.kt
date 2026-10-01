package com.abrarshakhi.galva.core.media.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Membership of a photo in a user album.
 *
 * A join table rather than a column, because a photo can be in any number of albums. The cascade to
 * `media` prunes membership when the underlying file really is gone; the cascade to `user_albums`
 * cleans up when an album is deleted.
 */
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
