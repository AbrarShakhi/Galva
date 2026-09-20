package com.abrarshakhi.galva.core.media.data.repository

import com.abrarshakhi.galva.core.media.data.local.dao.MediaDao
import com.abrarshakhi.galva.core.media.data.local.dao.UserAlbumDao
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumEntity
import com.abrarshakhi.galva.core.media.data.local.entity.UserAlbumMemberEntity
import com.abrarshakhi.galva.core.media.data.mapper.toDomain
import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Albums come from three places and only one of them is stored as an album.
 *
 * Device albums are derived by grouping the media index by MediaStore bucket, so a folder appears
 * and disappears with its contents and never needs syncing. Favorites and user albums are rows in
 * this app's database, which is why a photo can be in several of them at once while belonging to
 * exactly one folder.
 */
class AlbumRepositoryImpl(
    private val mediaDao: MediaDao,
    private val userAlbumDao: UserAlbumDao,
    private val dispatcher: CoroutineDispatcher,
    private val favoritesAlbumName: String = FAVORITES_NAME,
) : AlbumRepository {

    override fun observeAlbums(sort: AlbumSort): Flow<List<Album>> = combine(
        mediaDao.observeDeviceAlbums(),
        mediaDao.observeFavoritesSummary(),
        userAlbumDao.observeAlbums(),
    ) { deviceRows, favorites, userRows ->
        val favoritesAlbum = Album(
            ref = AlbumRef.Favorites,
            name = favoritesAlbumName,
            itemCount = favorites.itemCount,
            coverUri = favorites.coverUri,
            lastModifiedMs = favorites.lastModifiedMs ?: 0L,
        )
        // Favorites is pinned and the user's own albums come next: what the user made is easier to
        // lose among device folders than the other way round. Sorting applies within each group.
        buildList {
            add(favoritesAlbum)
            addAll(userRows.map { it.toDomain() }.sortedWith(sort.comparator()))
            addAll(deviceRows.map { it.toDomain() }.sortedWith(sort.comparator()))
        }
    }.flowOn(dispatcher)

    override fun observeAlbum(ref: AlbumRef): Flow<Album?> =
        observeAlbums(AlbumSort.RECENT_FIRST).map { albums -> albums.firstOrNull { it.ref == ref } }

    override fun observeUserAlbums(): Flow<List<Album>> = userAlbumDao.observeAlbums()
        .map { rows -> rows.map { it.toDomain() }.sortedWith(AlbumSort.RECENT_FIRST.comparator()) }
        .flowOn(dispatcher)

    override suspend fun createAlbum(name: String): AlbumRef.User = withContext(dispatcher) {
        val id = userAlbumDao.insertAlbum(
            UserAlbumEntity(name = name, createdAtMs = System.currentTimeMillis()),
        )
        AlbumRef.User(id)
    }

    override suspend fun addToAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>) {
        withContext(dispatcher) {
            val now = System.currentTimeMillis()
            userAlbumDao.addMembers(
                mediaIds.map { UserAlbumMemberEntity(albumId = ref.id, mediaId = it, addedAtMs = now) },
            )
        }
    }

    override suspend fun renameAlbum(ref: AlbumRef.User, name: String) {
        withContext(dispatcher) { userAlbumDao.renameAlbum(ref.id, name) }
    }

    override suspend fun deleteAlbum(ref: AlbumRef.User) {
        withContext(dispatcher) { userAlbumDao.deleteAlbum(ref.id) }
    }

    override suspend fun removeFromAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>) {
        withContext(dispatcher) { userAlbumDao.removeMembers(ref.id, mediaIds.toList()) }
    }

    private fun AlbumSort.comparator(): Comparator<Album> = when (this) {
        AlbumSort.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER, Album::name)
        AlbumSort.NAME_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER, Album::name)
        AlbumSort.RECENT_FIRST -> compareByDescending(Album::lastModifiedMs)
        AlbumSort.OLDEST_FIRST -> compareBy(Album::lastModifiedMs)
    }

    private companion object {
        const val FAVORITES_NAME = "Favorites"
    }
}
