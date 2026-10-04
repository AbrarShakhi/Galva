package com.abrarshakhi.galva.feature.search

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.ObserveMediaUseCase
import com.abrarshakhi.galva.core.model.MediaFilter
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionResult
import com.abrarshakhi.galva.core.ui.actions.MediaActionsStateHolder
import com.abrarshakhi.galva.core.ui.message.shown
import com.abrarshakhi.galva.core.ui.message.withMessage
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import com.abrarshakhi.galva.core.ui.selection.selectedBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    observeMedia: ObserveMediaUseCase,
    settingsRepository: SettingsRepository,
    private val mediaActions: MediaActionsStateHolder,
) : MviViewModel<SearchUiState, SearchIntent>(SearchUiState()) {

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
                        selection = selection.retainOnly(available),
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)

        mediaActions.state
            .onEach { actions -> setState { copy(actions = actions) } }
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

            is SearchIntent.ToggleSelection ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            SearchIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(results.map(MediaItem::id))) }

            SearchIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            is SearchIntent.Perform -> apply(mediaActions.perform(intent.action, selectedItems()))

            is SearchIntent.ActionEvent -> apply(mediaActions.handle(intent.event))

            is SearchIntent.MessageShown ->
                setState { copy(messages = messages.shown(intent.id)) }
        }
    }

    private fun apply(result: MediaActionResult) {
        setState {
            copy(
                selection = if (result.completed) selection.cleared() else selection,
                messages = messages.withMessage(result.message),
            )
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
