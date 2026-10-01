package com.abrarshakhi.galva.core.media.domain.repository

import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    /** Media in [source], newest capture time first. Re-emits whenever the local index changes. */
    fun observeMedia(source: MediaSource): Flow<List<MediaItem>>

    suspend fun getMedia(id: Long): MediaItem?

    suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean)

    suspend fun delete(ids: Collection<Long>): DeleteOutcome

    /**
     * Drops [ids] from the local index after the system delete dialog reported success, so the
     * grid updates without waiting for the next MediaStore change notification.
     */
    suspend fun forgetDeleted(ids: Collection<Long>)

    val syncState: Flow<SyncState>

    /** Reconciles the local index with MediaStore. Safe to call repeatedly. */
    suspend fun sync()
}
