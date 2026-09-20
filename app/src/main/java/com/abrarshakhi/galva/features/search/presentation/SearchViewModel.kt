package com.abrarshakhi.galva.features.search.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.common.ui.selection.selectedBy
import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaFilter
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveMediaUseCase
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Local, non-semantic search over file and folder names.
 *
 * The query is debounced before it reaches the database so that typing issues one query per pause
 * rather than one per keystroke, while the text field itself stays fully responsive because its
 * value is state, not a query result.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    observeMedia: ObserveMediaUseCase,
    settingsRepository: SettingsRepository,
    private val selectionActions: MediaSelectionActions,
) : MviViewModel<SearchUiState, SearchIntent, SearchEffect>(SearchUiState()) {

    private val criteria = MutableStateFlow(SearchCriteria())

    init {
        criteria
            .debounce { if (it.text.isBlank()) 0L else QUERY_DEBOUNCE_MS }
            .distinctUntilChanged()
            .flatMapLatest { current ->
                if (!current.isActive) flowOf(emptyList())
                else observeMedia(MediaSource.Query(current.text, current.filter))
            }
            .onEach { results ->
                val available = results.mapTo(HashSet(results.size), MediaItem::id)
                setState {
                    copy(
                        results = results,
                        isSearching = false,
                        selection = selection.copy(selectedIds = selection.selectedIds intersect available),
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                setState { copy(query = intent.query, isSearching = intent.query.isNotBlank()) }
                criteria.value = criteria.value.copy(text = intent.query)
            }

            is SearchIntent.FilterChanged -> {
                setState { copy(filter = intent.filter, isSearching = true) }
                criteria.value = criteria.value.copy(filter = intent.filter)
            }

            SearchIntent.QueryCleared -> {
                setState { copy(query = "", results = emptyList(), selection = selection.cleared()) }
                criteria.value = criteria.value.copy(text = "")
            }

            is SearchIntent.MediaTapped ->
                if (currentState.selection.isActive) {
                    setState { copy(selection = selection.toggle(intent.item.id)) }
                } else {
                    sendEffect(
                        SearchEffect.OpenViewer(
                            source = MediaSource.Query(currentState.query, currentState.filter),
                            mediaId = intent.item.id,
                        )
                    )
                }

            is SearchIntent.MediaLongPressed ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            SearchIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(results.map(MediaItem::id))) }

            SearchIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            SearchIntent.ShareSelection ->
                selectionActions.share(selectedItems())?.let { request ->
                    sendEffect(SearchEffect.ShareItems(request.uris, request.mimeTypes))
                }

            SearchIntent.FavoriteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                val favorited = selectionActions.favorite(selected)
                setState { copy(selection = selection.cleared()) }
                sendEffect(
                    SearchEffect.ShowMessage(
                        if (favorited) "Added to Favorites" else "Removed from Favorites"
                    )
                )
            }

            SearchIntent.DeleteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                when (val outcome = selectionActions.requestDelete(selected)) {
                    is DeleteOutcome.NeedsConsent -> sendEffect(
                        SearchEffect.ConfirmDelete(selected.map(MediaItem::id), outcome.uris)
                    )

                    is DeleteOutcome.Deleted -> onDeleted(selected.map(MediaItem::id))
                    is DeleteOutcome.Failed -> sendEffect(SearchEffect.ShowMessage(outcome.reason))
                }
            }

            SearchIntent.AddToAlbumRequested ->
                if (selectedItems().isEmpty()) return else setState { copy(showAddToAlbum = true) }

            SearchIntent.AddToAlbumDismissed -> setState { copy(showAddToAlbum = false) }

            is SearchIntent.AddedToAlbum -> {
                setState { copy(showAddToAlbum = false, selection = selection.cleared()) }
                sendEffect(SearchEffect.ShowMessage("Added to ${intent.albumName}"))
            }

                        is SearchIntent.DeleteResolved -> if (intent.confirmed) onDeleted(intent.ids) else Unit
        }
    }

    private fun onDeleted(ids: List<Long>) {
        setState { copy(selection = selection.cleared()) }
        viewModelScope.launch {
            selectionActions.confirmDeleted(ids)
            sendEffect(SearchEffect.ShowMessage("Deleted ${ids.size}"))
        }
    }

    private fun selectedItems(): List<MediaItem> =
        currentState.results.selectedBy(currentState.selection)

    private data class SearchCriteria(
        val text: String = "",
        val filter: MediaFilter = MediaFilter.ALL,
    ) {
        val isActive: Boolean get() = text.isNotBlank() || filter != MediaFilter.ALL
    }

    private companion object {
        const val QUERY_DEBOUNCE_MS = 250L
    }
}
