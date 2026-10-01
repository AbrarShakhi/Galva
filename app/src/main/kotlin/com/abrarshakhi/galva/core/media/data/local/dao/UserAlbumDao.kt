package com.abrarshakhi.galva.core.media.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumEntity
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumMemberEntity
import kotlinx.coroutines.flow.Flow

/** An album row with the aggregates the albums grid needs, computed rather than stored. */
data class UserAlbumRow(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
    val newestItemMs: Long?,
    val createdAtMs: Long,
)

@Dao
interface UserAlbumDao {

    @Insert
    suspend fun insertAlbum(album: UserAlbumEntity): Long

    /**
     * Membership is ignored on conflict rather than replaced: adding a photo that is already in the
     * album is a no-op, not a re-add that would move it to the end.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMembers(members: List<UserAlbumMemberEntity>)

    @Query("UPDATE user_albums SET name = :name WHERE id = :albumId")
    suspend fun renameAlbum(albumId: Long, name: String)

    /** Membership rows go with it via the cascade; the media rows themselves are untouched. */
    @Query("DELETE FROM user_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Query("DELETE FROM user_album_members WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun removeMembers(albumId: Long, mediaIds: List<Long>)

    @Query(
        """
        SELECT a.id AS id,
               a.name AS name,
               a.createdAtMs AS createdAtMs,
               COUNT(media.id) AS itemCount,
               MAX(media.dateTakenMs) AS newestItemMs,
               (
                   SELECT cover.uri FROM media AS cover
                   INNER JOIN user_album_members AS cm ON cm.mediaId = cover.id
                   WHERE cm.albumId = a.id
                   ORDER BY cover.dateTakenMs DESC, cover.id DESC
                   LIMIT 1
               ) AS coverUri
        FROM user_albums AS a
        LEFT JOIN user_album_members AS m ON m.albumId = a.id
        LEFT JOIN media ON media.id = m.mediaId
        GROUP BY a.id
        """
    )
    fun observeAlbums(): Flow<List<UserAlbumRow>>

    @Query(
        """
        SELECT media.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media
        INNER JOIN user_album_members AS m ON m.mediaId = media.id
        LEFT JOIN favorites AS f ON f.mediaId = media.id
        WHERE m.albumId = :albumId
        ORDER BY media.dateTakenMs DESC, media.id DESC
        """
    )
    fun observeMedia(albumId: Long): Flow<List<MediaRow>>
}
