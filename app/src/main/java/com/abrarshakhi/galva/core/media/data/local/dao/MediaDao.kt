package com.abrarshakhi.galva.core.media.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.abrarshakhi.galva.core.media.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

/** Well under SQLite's 999-variable default so chunked statements always bind. */
private const val SQLITE_PARAM_CHUNK = 500

/**
 * Reads always project through a LEFT JOIN on `favorites` so callers get a complete [MediaRow]
 * without a second round trip, and always order newest capture time first — the order every
 * gallery surface in the app expects.
 */
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

    /**
     * Free-text search over file and album name.
     *
     * The predicates are written so a null/blank parameter disables that clause, which keeps
     * filtering to one query instead of a combinatorial set of them.
     */
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

    /**
     * Folds the write-ahead log into the database file and truncates it. secure_delete zeroes a
     * deleted row in the database file, but older copies of its page can sit in the log until it
     * is reset; this resets it. Pass [TRUNCATING_CHECKPOINT].
     */
    @RawQuery
    fun checkpoint(query: SupportSQLiteQuery): WalCheckpoint?

    /**
     * Applies one sync delta atomically so observers never see a half-reconciled index.
     * Both lists are chunked because SQLite caps the number of statement parameters.
     */
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

/** The row `PRAGMA wal_checkpoint` reports. */
data class WalCheckpoint(
    val busy: Int,
    val log: Int,
    val checkpointed: Int,
)

val TRUNCATING_CHECKPOINT: SupportSQLiteQuery = SimpleSQLiteQuery("PRAGMA wal_checkpoint(TRUNCATE)")
