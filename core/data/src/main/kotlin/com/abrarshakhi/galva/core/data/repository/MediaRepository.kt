package com.abrarshakhi.galva.core.data.repository

import com.abrarshakhi.galva.core.model.DeleteOutcome
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.model.SyncState
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    fun observeMedia(source: MediaSource): Flow<List<MediaItem>>

    suspend fun getMedia(id: Long): MediaItem?

    suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean)

    suspend fun delete(ids: Collection<Long>): DeleteOutcome

    suspend fun forgetDeleted(ids: Collection<Long>)

    val syncState: Flow<SyncState>

    suspend fun sync()
}
