package com.abrarshakhi.galva.core.data.repository

import com.abrarshakhi.galva.core.data.mapper.toDomain
import com.abrarshakhi.galva.core.database.dao.MediaDao
import com.abrarshakhi.galva.core.database.dao.UserAlbumDao
import com.abrarshakhi.galva.core.database.entity.UserAlbumEntity
import com.abrarshakhi.galva.core.database.entity.UserAlbumMemberEntity
import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.AlbumSort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class AlbumRepositoryImpl(
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
