package com.abrarshakhi.galva.core.data.repository

import com.abrarshakhi.galva.core.model.Album
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.AlbumSort
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {

    fun observeAlbums(sort: AlbumSort): Flow<List<Album>>

    fun observeAlbum(ref: AlbumRef): Flow<Album?>

    fun observeUserAlbums(): Flow<List<Album>>

    suspend fun createAlbum(name: String): AlbumRef.User

    suspend fun addToAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>)

    suspend fun renameAlbum(ref: AlbumRef.User, name: String)

    suspend fun deleteAlbum(ref: AlbumRef.User)

    suspend fun removeFromAlbum(ref: AlbumRef.User, mediaIds: Collection<Long>)
}
