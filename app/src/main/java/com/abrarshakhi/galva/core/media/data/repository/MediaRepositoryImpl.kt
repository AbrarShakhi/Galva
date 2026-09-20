package com.abrarshakhi.galva.core.media.data.repository

import com.abrarshakhi.galva.core.media.data.local.dao.FavoriteDao
import com.abrarshakhi.galva.core.media.data.local.dao.MediaDao
import com.abrarshakhi.galva.core.media.data.local.dao.MediaRow
import com.abrarshakhi.galva.core.media.data.local.dao.UserAlbumDao
import com.abrarshakhi.galva.core.media.data.local.entity.FavoriteEntity
import com.abrarshakhi.galva.core.media.data.mapper.toDomain
import com.abrarshakhi.galva.core.media.data.sync.MediaSyncManager
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaFilter
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.MediaType
import com.abrarshakhi.galva.core.media.domain.model.SyncState
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Serves every read from the Room index, never from MediaStore directly.
 *
 * That single query surface is what makes the grid, albums and search cheap and consistent;
 * MediaStore is only touched by [MediaSyncManager] and by deletions.
 */
class MediaRepositoryImpl(
    private val mediaDao: MediaDao,
    private val favoriteDao: FavoriteDao,
    private val userAlbumDao: UserAlbumDao,
    private val syncManager: MediaSyncManager,
    private val dispatcher: CoroutineDispatcher,
) : MediaRepository {

    override fun observeMedia(source: MediaSource): Flow<List<MediaItem>> =
        source.rows().map { rows -> rows.map(MediaRow::toDomain) }.flowOn(dispatcher)

    private fun MediaSource.rows(): Flow<List<MediaRow>> = when (this) {
        MediaSource.AllMedia -> mediaDao.observeAll()

        is MediaSource.Album -> when (val target = ref) {
            is AlbumRef.Device -> mediaDao.observeByAlbum(target.bucketId)
            AlbumRef.Favorites -> mediaDao.observeFavorites()
            is AlbumRef.User -> userAlbumDao.observeMedia(target.id)
        }

        is MediaSource.Query -> mediaDao.search(
            query = text.trim(),
            type = filter.toTypeName(),
            favoritesOnly = filter == MediaFilter.FAVORITES,
        )
    }

    private fun MediaFilter.toTypeName(): String? = when (this) {
        MediaFilter.IMAGES -> MediaType.IMAGE.name
        MediaFilter.VIDEOS -> MediaType.VIDEO.name
        MediaFilter.ALL, MediaFilter.FAVORITES -> null
    }

    override suspend fun getMedia(id: Long): MediaItem? = withContext(dispatcher) {
        mediaDao.findById(id)?.toDomain()
    }

    override suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean) {
        if (ids.isEmpty()) return
        withContext(dispatcher) {
            if (favorite) {
                val now = System.currentTimeMillis()
                favoriteDao.favorite(ids.map { FavoriteEntity(mediaId = it, favoritedAtMs = now) })
            } else {
                favoriteDao.unfavorite(ids.toList())
            }
        }
    }

    /**
     * Scoped storage forbids deleting media this app did not create without the user agreeing in a
     * system dialog, which only an Activity can show. The repository therefore resolves the URIs
     * and hands them back; the UI launches the request and calls [forgetDeleted] on success.
     */
    override suspend fun delete(ids: Collection<Long>): DeleteOutcome = withContext(dispatcher) {
        val uris = mediaDao.urisFor(ids.toList())
        if (uris.isEmpty()) DeleteOutcome.Failed("Those items are no longer available")
        else DeleteOutcome.NeedsConsent(uris)
    }

    override suspend fun forgetDeleted(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        withContext(dispatcher) { mediaDao.deleteByIds(ids.toList()) }
    }

    override val syncState: Flow<SyncState> get() = syncManager.state

    override suspend fun sync() = syncManager.sync()
}
