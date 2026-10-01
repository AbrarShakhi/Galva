package com.abrarshakhi.galva.core.media.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * App-owned favourite flag, kept in its own table rather than as a column on [MediaEntity].
 *
 * The sync pass upserts and deletes media rows freely; holding favourites separately means a
 * re-read of a changed photo cannot silently clear the user's flag. The cascade still drops the
 * favourite when the underlying file genuinely disappears.
 */
@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = MediaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val favoritedAtMs: Long,
)
