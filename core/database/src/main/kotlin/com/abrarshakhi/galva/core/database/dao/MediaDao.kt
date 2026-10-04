package com.abrarshakhi.galva.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.abrarshakhi.galva.core.database.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

private const val SQLITE_PARAM_CHUNK = 500

@Dao
interface MediaDao {

    @Query(
        """
        SELECT media.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media
        LEFT JOIN favorites AS f ON f.mediaId = media.id
        ORDER BY media.dateTakenMs DESC, media.id DESC
        """
    )
    fun observeAll(): Flow<List<MediaRow>>

    @Query(
        """
        SELECT media.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media
        LEFT JOIN favorites AS f ON f.mediaId = media.id
        WHERE media.albumId = :albumId
        ORDER BY media.dateTakenMs DESC, media.id DESC
        """
    )
    fun observeByAlbum(albumId: Long): Flow<List<MediaRow>>

    @Query(
        """
        SELECT media.*, 1 AS isFavorite
        FROM media
        INNER JOIN favorites AS f ON f.mediaId = media.id
        ORDER BY media.dateTakenMs DESC, media.id DESC
        """
    )
    fun observeFavorites(): Flow<List<MediaRow>>

    @Query(
        """
        SELECT media.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media
        LEFT JOIN favorites AS f ON f.mediaId = media.id
        WHERE (
                :query = ''
                OR media.displayName LIKE '%' || :query || '%'
                OR media.albumName LIKE '%' || :query || '%'
              )
          AND (:type IS NULL OR media.type = :type)
          AND (:favoritesOnly = 0 OR f.mediaId IS NOT NULL)
        ORDER BY media.dateTakenMs DESC, media.id DESC
        """
    )
    fun search(query: String, type: String?, favoritesOnly: Boolean): Flow<List<MediaRow>>

    @Query(
        """
        SELECT media.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media
        LEFT JOIN favorites AS f ON f.mediaId = media.id
        WHERE media.id = :id
        """
    )
    suspend fun findById(id: Long): MediaRow?

    @Query("SELECT id, dateModifiedMs FROM media")
    suspend fun fingerprints(): List<MediaFingerprint>

    @Query("SELECT uri FROM media WHERE id IN (:ids)")
    suspend fun urisFor(ids: List<Long>): List<String>

    @Upsert
    suspend fun upsertAll(media: List<MediaEntity>)

    @Query("DELETE FROM media WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @RawQuery
    fun checkpoint(query: SupportSQLiteQuery): WalCheckpoint?

    @Transaction
    suspend fun applySyncDelta(upserted: List<MediaEntity>, removedIds: List<Long>) {
        upserted.chunked(SQLITE_PARAM_CHUNK).forEach { upsertAll(it) }
        removedIds.chunked(SQLITE_PARAM_CHUNK).forEach { deleteByIds(it) }
    }

    @Query(
        """
        SELECT media.albumId AS id,
               media.albumName AS name,
               COUNT(*) AS itemCount,
               MAX(media.dateTakenMs) AS lastModifiedMs,
               (
                   SELECT cover.uri FROM media AS cover
                   WHERE cover.albumId = media.albumId
                   ORDER BY cover.dateTakenMs DESC, cover.id DESC
                   LIMIT 1
               ) AS coverUri
        FROM media
        GROUP BY media.albumId, media.albumName
        """
    )
    fun observeDeviceAlbums(): Flow<List<AlbumRow>>

    @Query(
        """
        SELECT COUNT(*) AS itemCount,
               MAX(media.dateTakenMs) AS lastModifiedMs,
               (
                   SELECT cover.uri FROM media AS cover
                   INNER JOIN favorites AS cf ON cf.mediaId = cover.id
                   ORDER BY cover.dateTakenMs DESC, cover.id DESC
                   LIMIT 1
               ) AS coverUri
        FROM media
        INNER JOIN favorites AS f ON f.mediaId = media.id
        """
    )
    fun observeFavoritesSummary(): Flow<FavoritesSummary>
}

data class FavoritesSummary(
    val itemCount: Int,
    val coverUri: String?,
    val lastModifiedMs: Long?,
)

data class WalCheckpoint(
    val busy: Int,
    val log: Int,
    val checkpointed: Int,
)

val TRUNCATING_CHECKPOINT: SupportSQLiteQuery = SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)")
