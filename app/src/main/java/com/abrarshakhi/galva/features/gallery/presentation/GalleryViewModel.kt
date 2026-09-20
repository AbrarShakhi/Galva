package com.abrarshakhi.galva.features.gallery.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.common.ui.selection.selectedBy
import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.model.SyncState
import com.abrarshakhi.galva.core.media.domain.repository.MediaRepository
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveTimelineUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.SyncMediaUseCase
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GalleryViewModel(
    observeTimeline: ObserveTimelineUseCase,
    settingsRepository: SettingsRepository,
    mediaRepository: MediaRepository,
    private val selectionActions: MediaSelectionActions,
    private val syncMedia: SyncMediaUseCase,
) : MviViewModel<GalleryUiState, GalleryIntent, GalleryEffect>(GalleryUiState()) {

    private val source = MediaSource.AllMedia

    init {
        observeTimeline(source).onEach { sections ->
                val available =
                    sections.asSequence().flatMap { it.items.asSequence() }.map(MediaItem::id)
                        .toSet()
                setState {
                    copy(
                        sections = sections,
                        isLoading = false,
                        selection = selection.copy(selectedIds = selection.selectedIds intersect available),
                    )
                }
            }.launchIn(viewModelScope)

        settingsRepository.settings.map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }.launchIn(viewModelScope)

        mediaRepository.syncState.onEach { sync -> setState { copy(isSyncing = sync is SyncState.Syncing) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: GalleryIntent) {
        when (intent) {
            GalleryIntent.Refresh -> syncMedia()

            is GalleryIntent.MediaTapped -> if (currentState.selection.isActive) {
                setState { copy(selection = selection.toggle(intent.item.id)) }
            } else {
                sendEffect(GalleryEffect.OpenViewer(source, intent.item.id))
            }

            is GalleryIntent.MediaLongPressed -> setState { copy(selection = selection.toggle(intent.item.id)) }

            is GalleryIntent.SectionTapped -> {
                val ids = intent.section.items.map(MediaItem::id)
                val alreadySelected = ids.isNotEmpty() && ids.all(currentState.selection::contains)
                setState {
                    val next = if (alreadySelected) selection.selectedIds - ids.toSet()
                    else selection.selectedIds + ids
                    copy(selection = selection.copy(selectedIds = next))
                }
            }

            GalleryIntent.SelectAll -> setState {
                copy(
                    selection = selection.selectAll(
                        items.map(
                            MediaItem::id
                        )
                    )
                )
            }

            GalleryIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            GalleryIntent.ShareSelection -> selectionActions.share(selectedItems())
                ?.let { request ->
                    sendEffect(GalleryEffect.ShareItems(request.uris, request.mimeTypes))
                }

            GalleryIntent.FavoriteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                val favorited = selectionActions.favorite(selected)
                setState { copy(selection = selection.cleared()) }
                sendEffect(
                    GalleryEffect.ShowMessage(
                        if (favorited) "Added to Favorites" else "Removed from Favorites"
                    )
                )
            }

            GalleryIntent.DeleteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                when (val outcome = selectionActions.requestDelete(selected)) {
                    is DeleteOutcome.NeedsConsent -> sendEffect(
                        GalleryEffect.ConfirmDelete(selected.map(MediaItem::id), outcome.uris)
                    )

                    is DeleteOutcome.Deleted -> onDeleted(selected.map(MediaItem::id))
                    is DeleteOutcome.Failed -> sendEffect(GalleryEffect.ShowMessage(outcome.reason))
                }
            }

            GalleryIntent.AddToAlbumRequested -> if (selectedItems().isEmpty()) return else setState {
                copy(
                    showAddToAlbum = true
                )
            }

            GalleryIntent.AddToAlbumDismissed -> setState { copy(showAddToAlbum = false) }

            is GalleryIntent.AddedToAlbum -> {
                setState { copy(showAddToAlbum = false, selection = selection.cleared()) }
                sendEffect(GalleryEffect.ShowMessage("Added to ${intent.albumName}"))
            }

            is GalleryIntent.DeleteResolved -> if (intent.confirmed) onDeleted(intent.ids) else Unit
        }
    }

    private fun onDeleted(ids: List<Long>) {
        setState { copy(selection = selection.cleared()) }
        viewModelScope.launch {
            selectionActions.confirmDeleted(ids)
            sendEffect(GalleryEffect.ShowMessage("Deleted ${ids.size}"))
        }
    }

    private fun selectedItems(): List<MediaItem> =
        currentState.items.selectedBy(currentState.selection)
}
