package com.abrarshakhi.galva.core.media.domain.repository

import com.abrarshakhi.galva.core.media.domain.model.Album
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.AlbumSort
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {

    /** Device folders, the synthetic Favorites album, and the user's own albums, ordered by [sort]. */
    fun observeAlbums(sort: AlbumSort): Flow<List<Album>>

    fun observeAlbum(ref: AlbumRef): Flow<Album?>

    /** Albums the user can add to. Device folders are excluded: adding there would move files. */
    fun observeUserAlbums(): Flow<List<Album>>

    suspend fun createAlbum(name: String): AlbumRef.User

    suspend fun addToAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>)

    suspend fun renameAlbum(ref: AlbumRef.User, name: String)

    /** Removes the album itself. The photos in it are untouched — they live on the filesystem. */
    suspend fun deleteAlbum(ref: AlbumRef.User)

    /** Removes membership only. The photos stay on the device and in every other album. */
    suspend fun removeFromAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>)
}
