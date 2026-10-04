package com.abrarshakhi.galva.feature.albums

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.core.data.repository.AlbumRepository
import com.abrarshakhi.galva.core.data.repository.SettingsRepository
import com.abrarshakhi.galva.core.domain.DeleteAlbumUseCase
import com.abrarshakhi.galva.core.domain.ObserveMediaUseCase
import com.abrarshakhi.galva.core.domain.RemoveFromAlbumUseCase
import com.abrarshakhi.galva.core.domain.RenameAlbumUseCase
import com.abrarshakhi.galva.core.model.AlbumRef
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.ui.actions.MediaActionResult
import com.abrarshakhi.galva.core.ui.actions.MediaActionsStateHolder
import com.abrarshakhi.galva.core.ui.message.shown
import com.abrarshakhi.galva.core.ui.message.withMessage
import com.abrarshakhi.galva.core.ui.mvi.MviViewModel
import com.abrarshakhi.galva.core.ui.selection.selectedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class AlbumDetailViewModel(
    albumRef: AlbumRef,
    observeMedia: ObserveMediaUseCase,
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    private val renameAlbum: RenameAlbumUseCase,
    private val deleteAlbum: DeleteAlbumUseCase,
    private val removeFromAlbum: RemoveFromAlbumUseCase,
    private val mediaActions: MediaActionsStateHolder,
) : MviViewModel<AlbumDetailUiState, AlbumDetailIntent>(
    AlbumDetailUiState(
        source = MediaSource.Album(albumRef),
        canManage = albumRef is AlbumRef.User,
    ),
) {

    private val managedRef: AlbumRef.User? = albumRef as? AlbumRef.User

    init {
        observeMedia(currentState.source)
            .onEach { items ->
                val available = items.mapTo(HashSet(items.size), MediaItem::id)
                setState {
                    copy(items = items, isLoading = false, selection = selection.retainOnly(available))
                }
            }
            .launchIn(viewModelScope)

        albumRepository.observeAlbum(albumRef)
            .onEach { album -> setState { copy(albumName = album?.name.orEmpty()) } }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .map { it.galleryColumns }
            .onEach { columns -> setState { copy(columns = columns) } }
            .launchIn(viewModelScope)

        mediaActions.state
            .onEach { actions -> setState { copy(actions = actions) } }
            .launchIn(viewModelScope)
    }

    override suspend fun reduce(intent: AlbumDetailIntent) {
        when (intent) {
            is AlbumDetailIntent.ToggleSelection ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            AlbumDetailIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(items.map(MediaItem::id))) }

            AlbumDetailIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            is AlbumDetailIntent.Perform ->
                apply(mediaActions.perform(intent.action, selectedItems()))

            is AlbumDetailIntent.ActionEvent -> apply(mediaActions.handle(intent.event))

            AlbumDetailIntent.RemoveFromAlbum -> removeSelection()

            AlbumDetailIntent.RenameRequested -> setState { copy(showRenameDialog = true) }

            AlbumDetailIntent.RenameDismissed -> setState { copy(showRenameDialog = false) }

            is AlbumDetailIntent.RenameConfirmed -> {
                setState { copy(showRenameDialog = false) }
                managedRef?.let { renameAlbum(it, intent.name) }
            }

            AlbumDetailIntent.DeleteAlbumRequested ->
                setState { copy(showDeleteAlbumDialog = true) }

            AlbumDetailIntent.DeleteAlbumDismissed ->
                setState { copy(showDeleteAlbumDialog = false) }

            AlbumDetailIntent.DeleteAlbumConfirmed -> {
                setState { copy(showDeleteAlbumDialog = false) }
                val ref = managedRef ?: return
                deleteAlbum(ref)
                setState { copy(isDeleted = true) }
            }

            is AlbumDetailIntent.MessageShown ->
                setState { copy(messages = messages.shown(intent.id)) }
        }
    }

    private suspend fun removeSelection() {
        val ref = managedRef ?: return
        val selected = selectedItems()
        if (selected.isEmpty()) return
        removeFromAlbum(ref, selected.map(MediaItem::id))
        setState {
            copy(
                selection = selection.cleared(),
                messages = messages.withMessage("Removed ${selected.size} from album"),
            )
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
        currentState.items.selectedBy(currentState.selection)
}
