package com.abrarshakhi.galva.feature.gallery

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.ObserveTimelineUseCase
import com.abrarshakhi.galva.core.domain.SyncMediaUseCase
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.model.SyncState
import com.abrarshakhi.galva.core.ui.actions.MediaActionResult
import com.abrarshakhi.galva.core.ui.actions.MediaActionsStateHolder
import com.abrarshakhi.galva.core.ui.message.shown
import com.abrarshakhi.galva.core.ui.message.withMessage
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class GalleryViewModel(
    observeTimeline: ObserveTimelineUseCase,
    settingsRepository: SettingsRepository,
    mediaRepository: MediaRepository,
    private val syncMedia: SyncMediaUseCase,
    private val mediaActions: MediaActionsStateHolder,
) : MviViewModel<GalleryUiState, GalleryIntent>(GalleryUiState()) {

    init {
        observeTimeline(MediaSource.AllMedia)
            .onEach { sections ->
                val available = sections.flatMapTo(HashSet()) { section ->
                    section.items.map(MediaItem::id)
                }
                setState {
                    copy(
                        sections = sections,
                        isLoading = false,
                        selection = selection.retainOnly(available),
                    )
                }
            }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)

        mediaRepository.syncState
            .onEach { sync -> setState { copy(isSyncing = sync is SyncState.Syncing) } }
            .launchIn(viewModelScope)

        mediaActions.state
            .onEach { actions -> setState { copy(actions = actions) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.Refresh -> {
                setState { copy(isRefreshing = true) }
                syncMedia()
                setState { copy(isRefreshing = false) }
            }

            is GalleryIntent.ToggleSelection ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            is GalleryIntent.ToggleSection -> setState {
                copy(selection = selection.toggleAll(intent.section.items.map(MediaItem::id)))
            }

            GalleryIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(items.map(MediaItem::id))) }

            GalleryIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            is GalleryIntent.Perform -> apply(mediaActions.perform(intent.action, selectedItems()))

            is GalleryIntent.ActionEvent -> apply(mediaActions.handle(intent.event))

            is GalleryIntent.MessageShown ->
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

    private fun selectedItems(): List<MediaItem> = currentState.selectedItems
}
