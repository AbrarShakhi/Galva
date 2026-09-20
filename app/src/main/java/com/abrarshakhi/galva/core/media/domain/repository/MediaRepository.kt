package com.abrarshakhi.galva.core.media.domain.repository

import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.SyncState
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
