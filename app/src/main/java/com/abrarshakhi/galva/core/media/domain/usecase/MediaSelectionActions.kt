package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem

class MediaSelectionActions(
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteMedia: DeleteMediaUseCase,
) {

    fun share(items: List<MediaItem>): ShareRequest? = if (items.isEmpty()) null
    else ShareRequest(
        uris = items.map(MediaItem::uri),
        mimeTypes = items.map(MediaItem::mimeType),
    )

    suspend fun favorite(items: List<MediaItem>): Boolean {
        val shouldFavorite = items.any { !it.isFavorite }
        toggleFavorite(items.map(MediaItem::id), shouldFavorite)
        return shouldFavorite
    }

    suspend fun requestDelete(items: List<MediaItem>): DeleteOutcome =
        deleteMedia(items.map(MediaItem::id))

    suspend fun confirmDeleted(ids: Collection<Long>) = deleteMedia.confirm(ids)
}

data class ShareRequest(
    val uris: List<String>,
    val mimeTypes: List<String>,
)
