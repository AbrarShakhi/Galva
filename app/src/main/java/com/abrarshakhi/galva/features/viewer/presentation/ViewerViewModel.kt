package com.abrarshakhi.galva.features.viewer.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveMediaUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Pages through the same collection the user opened from.
 *
 * The list is re-observed rather than passed in, so favouriting or deleting from inside the viewer
 * is reflected immediately, and the position is re-anchored to the item the user is actually
 * looking at whenever the list changes underneath it.
 */
class ViewerViewModel(
    private val source: MediaSource,
    private val initialMediaId: Long,
    observeMedia: ObserveMediaUseCase,
    private val selectionActions: MediaSelectionActions,
) : MviViewModel<ViewerUiState, ViewerIntent, ViewerEffect>(ViewerUiState()) {

    /** Tracks the visible item across list updates; ids survive reordering, indices do not. */
    private var anchorId: Long = initialMediaId

    init {
        observeMedia(source)
            .onEach { items ->
                if (items.isEmpty()) {
                    setState { copy(items = items, isLoading = false) }
                    sendEffect(ViewerEffect.Close)
                    return@onEach
                }
                val index = items.indexOfFirst { it.id == anchorId }
                    .takeIf { it >= 0 }
                    ?: currentState.currentIndex.coerceIn(items.indices)
                anchorId = items[index].id
                setState { copy(items = items, currentIndex = index, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: ViewerIntent) {
        when (intent) {
            is ViewerIntent.PageSettled -> {
                val item = currentState.items.getOrNull(intent.index) ?: return
                anchorId = item.id
                setState { copy(currentIndex = intent.index) }
            }

            ViewerIntent.ChromeToggled -> setState { copy(chromeVisible = !chromeVisible) }

            ViewerIntent.FavoriteToggled -> {
                val item = currentState.current ?: return
                val favorited = selectionActions.favorite(listOf(item))
                sendEffect(
                    ViewerEffect.ShowMessage(
                        if (favorited) "Added to Favorites" else "Removed from Favorites"
                    )
                )
            }

            ViewerIntent.ShareRequested -> {
                val item = currentState.current ?: return
                selectionActions.share(listOf(item))?.let { request ->
                    sendEffect(ViewerEffect.ShareItem(request.uris, request.mimeTypes))
                }
            }

            ViewerIntent.DeleteRequested -> {
                val item = currentState.current ?: return
                when (val outcome = selectionActions.requestDelete(listOf(item))) {
                    is DeleteOutcome.NeedsConsent ->
                        sendEffect(ViewerEffect.ConfirmDelete(listOf(item.id), outcome.uris))

                    is DeleteOutcome.Deleted -> onDeleted(listOf(item.id))
                    is DeleteOutcome.Failed -> sendEffect(ViewerEffect.ShowMessage(outcome.reason))
                }
            }

            is ViewerIntent.DeleteResolved -> if (intent.confirmed) onDeleted(intent.ids) else Unit
        }
    }

    private fun onDeleted(ids: List<Long>) {
        // Re-anchor to the neighbour before the list update arrives, so the pager lands on the
        // next photo instead of snapping back to the start.
        val items = currentState.items
        val removed = ids.toSet()
        val nextAnchor = items.drop(currentState.currentIndex + 1).firstOrNull { it.id !in removed }
            ?: items.take(currentState.currentIndex).lastOrNull { it.id !in removed }
        anchorId = nextAnchor?.id ?: NO_ANCHOR

        viewModelScope.launch { selectionActions.confirmDeleted(ids) }
    }

    private companion object {
        const val NO_ANCHOR = -1L
    }
}
