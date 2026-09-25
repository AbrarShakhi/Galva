package com.abrarshakhi.galva.features.albums.presentation

import androidx.lifecycle.viewModelScope
import com.abrarshakhi.galva.common.mvi.MviViewModel
import com.abrarshakhi.galva.common.ui.selection.selectedBy
import com.abrarshakhi.galva.core.media.domain.model.AlbumRef
import com.abrarshakhi.galva.core.media.domain.model.DeleteOutcome
import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.media.domain.model.MediaSource
import com.abrarshakhi.galva.core.media.domain.repository.AlbumRepository
import com.abrarshakhi.galva.core.media.domain.usecase.MediaSelectionActions
import com.abrarshakhi.galva.core.media.domain.usecase.DeleteAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.ObserveMediaUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.RemoveFromAlbumUseCase
import com.abrarshakhi.galva.core.media.domain.usecase.RenameAlbumUseCase
import com.abrarshakhi.galva.core.settings.domain.SettingsRepository
import com.abrarshakhi.galva.core.vault.domain.usecase.MoveStart
import com.abrarshakhi.galva.core.vault.domain.usecase.MoveToSecretsActions
import com.abrarshakhi.galva.features.secrets.presentation.MoveProgress
import com.abrarshakhi.galva.features.secrets.presentation.SET_UP_SECRETS_FIRST
import com.abrarshakhi.galva.features.secrets.presentation.movedMessage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val albumRef: AlbumRef,
    observeMedia: ObserveMediaUseCase,
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    private val selectionActions: MediaSelectionActions,
    private val renameAlbum: RenameAlbumUseCase,
    private val deleteAlbum: DeleteAlbumUseCase,
    private val removeFromAlbum: RemoveFromAlbumUseCase,
    private val moveToSecrets: MoveToSecretsActions,
) : MviViewModel<AlbumDetailUiState, AlbumDetailIntent, AlbumDetailEffect>(AlbumDetailUiState()) {

    private val source = MediaSource.Album(albumRef)

    private val managedRef: AlbumRef.User? = albumRef as? AlbumRef.User

    init {
        setState { copy(canManage = managedRef != null) }

        observeMedia(source)
            .onEach { items ->
                val available = items.mapTo(HashSet(items.size), MediaItem::id)
                setState {
                    copy(
                        items = items,
                        isLoading = false,
                        selection = selection.copy(selectedIds = selection.selectedIds intersect available),
                    )
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
    }

    override suspend fun reduce(intent: AlbumDetailIntent) {
        when (intent) {
            is AlbumDetailIntent.MediaTapped ->
                if (currentState.selection.isActive) {
                    setState { copy(selection = selection.toggle(intent.item.id)) }
                } else {
                    sendEffect(AlbumDetailEffect.OpenViewer(source, intent.item.id))
                }

            is AlbumDetailIntent.MediaLongPressed ->
                setState { copy(selection = selection.toggle(intent.item.id)) }

            AlbumDetailIntent.SelectAll ->
                setState { copy(selection = selection.selectAll(items.map(MediaItem::id))) }

            AlbumDetailIntent.ClearSelection -> setState { copy(selection = selection.cleared()) }

            AlbumDetailIntent.ShareSelection ->
                selectionActions.share(selectedItems())?.let { request ->
                    sendEffect(AlbumDetailEffect.ShareItems(request.uris, request.mimeTypes))
                }

            AlbumDetailIntent.FavoriteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                val favorited = selectionActions.favorite(selected)
                setState { copy(selection = selection.cleared()) }
                sendEffect(
                    AlbumDetailEffect.ShowMessage(
                        if (favorited) "Added to Favorites" else "Removed from Favorites"
                    )
                )
            }

            AlbumDetailIntent.DeleteSelection -> {
                val selected = selectedItems()
                if (selected.isEmpty()) return
                when (val outcome = selectionActions.requestDelete(selected)) {
                    is DeleteOutcome.NeedsConsent -> sendEffect(
                        AlbumDetailEffect.ConfirmDelete(selected.map(MediaItem::id), outcome.uris)
                    )

                    is DeleteOutcome.Deleted -> onDeleted(selected.map(MediaItem::id))
                    is DeleteOutcome.Failed ->
                        sendEffect(AlbumDetailEffect.ShowMessage(outcome.reason))
                }
            }

            AlbumDetailIntent.AddToAlbumRequested ->
                if (selectedItems().isEmpty()) return else setState { copy(showAddToAlbum = true) }

            AlbumDetailIntent.AddToAlbumDismissed -> setState { copy(showAddToAlbum = false) }

            is AlbumDetailIntent.AddedToAlbum -> {
                setState { copy(showAddToAlbum = false, selection = selection.cleared()) }
                sendEffect(AlbumDetailEffect.ShowMessage("Added to ${intent.albumName}"))
            }

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
                sendEffect(AlbumDetailEffect.AlbumDeleted)
            }

            AlbumDetailIntent.RemoveFromAlbum -> {
                val ref = managedRef ?: return
                val selected = selectedItems()
                if (selected.isEmpty()) return
                removeFromAlbum(ref, selected.map(MediaItem::id))
                setState { copy(selection = selection.cleared()) }
                sendEffect(
                    AlbumDetailEffect.ShowMessage(
                        "Removed ${selected.size} from album",
                    )
                )
            }

            is AlbumDetailIntent.DeleteResolved ->
                if (intent.confirmed) onDeleted(intent.ids) else Unit

            AlbumDetailIntent.MoveToSecretsSelection -> moveSelectionToSecrets()

            AlbumDetailIntent.VaultUnlocked -> {
                setState { copy(showVaultUnlock = false) }
                moveSelectionToSecrets()
            }

            AlbumDetailIntent.VaultUnlockDismissed -> setState { copy(showVaultUnlock = false) }

            is AlbumDetailIntent.MoveResolved -> {
                val moved = moveToSecrets.resolve(intent.confirmed)
                setState { copy(selection = selection.cleared()) }
                sendEffect(AlbumDetailEffect.ShowMessage(movedMessage(moved)))
            }
        }
    }

    private suspend fun moveSelectionToSecrets() {
        val selected = selectedItems()
        if (selected.isEmpty()) return
        val start = moveToSecrets.blocker() ?: encryptIntoSecrets(selected)
        when (start) {
            MoveStart.NeedsSetup -> sendEffect(AlbumDetailEffect.ShowMessage(SET_UP_SECRETS_FIRST))
            MoveStart.NeedsUnlock -> setState { copy(showVaultUnlock = true) }
            is MoveStart.Failed -> sendEffect(AlbumDetailEffect.ShowMessage(start.reason))
            is MoveStart.NeedsConsent ->
                sendEffect(AlbumDetailEffect.ConfirmMove(start.originalIds, start.originalUris))
        }
    }

    private suspend fun encryptIntoSecrets(items: List<MediaItem>): MoveStart {
        setState { copy(moveProgress = MoveProgress(done = 0, total = items.size)) }
        return try {
            moveToSecrets.start(items) { done ->
                setState { copy(moveProgress = MoveProgress(done, items.size)) }
            }
        } finally {
            setState { copy(moveProgress = null) }
        }
    }

    private fun onDeleted(ids: List<Long>) {
        setState { copy(selection = selection.cleared()) }
        viewModelScope.launch {
            selectionActions.confirmDeleted(ids)
            sendEffect(AlbumDetailEffect.ShowMessage("Deleted ${ids.size}"))
        }
    }

    private fun selectedItems(): List<MediaItem> =
        currentState.items.selectedBy(currentState.selection)
}
