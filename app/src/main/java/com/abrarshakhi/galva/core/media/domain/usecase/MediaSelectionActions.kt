package com.abrarshakhi.galva.core.media.domain.usecase

import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem

/**
 * The share / favourite / delete trio that every grid surface offers.
 *
 * Collected here as a collaborator rather than a base ViewModel: the gallery, an album and a
 * search result differ in how they obtain their list, not in what the toolbar does to a selection,
 * so composition keeps the three ViewModels independent while the behaviour stays defined once.
 */
class MediaSelectionActions(
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteMedia: DeleteMediaUseCase,
) {

    fun share(items: List<MediaItem>): ShareRequest? =
        if (items.isEmpty()) null
        else ShareRequest(
            uris = items.map(MediaItem::uri),
            mimeTypes = items.map(MediaItem::mimeType),
        )

    /**
     * Applies the favourite toggle to [items] and reports which way it went, so the caller can
     * phrase its confirmation without re-deriving the decision.
     */
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
