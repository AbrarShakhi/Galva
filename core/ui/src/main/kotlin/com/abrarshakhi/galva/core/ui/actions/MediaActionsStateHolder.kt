package com.abrarshakhi.galva.core.ui.actions

import com.abrarshakhi.galva.core.domain.MediaSelectionActions
import com.abrarshakhi.galva.core.domain.MoveStart
import com.abrarshakhi.galva.core.domain.MoveToSecretsActions
import com.abrarshakhi.galva.core.model.DeleteOutcome
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.ui.message.UserMessage
import com.abrarshakhi.galva.core.ui.vault.MoveProgress
import com.abrarshakhi.galva.core.ui.vault.SET_UP_SECRETS_FIRST
import com.abrarshakhi.galva.core.ui.vault.movedMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MediaActionsStateHolder(
    private val selectionActions: MediaSelectionActions,
    private val moveToSecrets: MoveToSecretsActions,
) {

    private val _state = MutableStateFlow(MediaActionsState())
    val state: StateFlow<MediaActionsState> = _state.asStateFlow()

    private var pendingConsent: PendingConsent? = null
    private var awaitingUnlock: List<MediaItem> = emptyList()

    suspend fun perform(
        action: MediaAction,
        items: List<MediaItem>,
        onRemoving: (ids: List<Long>) -> Unit = {},
    ): MediaActionResult {
        if (items.isEmpty()) return MediaActionResult.None
        return when (action) {
            MediaAction.Share -> share(items)
            MediaAction.Favorite -> favorite(items)
            MediaAction.AddToAlbum -> addToAlbum(items)
            MediaAction.MoveToSecrets -> moveToSecrets(items)
            MediaAction.Delete -> delete(items, onRemoving)
        }
    }

    suspend fun handle(
        event: MediaActionEvent,
        onRemoving: (ids: List<Long>) -> Unit = {},
    ): MediaActionResult = when (event) {
        is MediaActionEvent.ConsentLaunched -> {
            _state.update { state ->
                val consent = state.consent?.takeIf { it.id == event.requestId }
                state.copy(consent = consent?.copy(launched = true) ?: state.consent)
            }
            MediaActionResult.None
        }

        is MediaActionEvent.ConsentResolved -> resolveConsent(event, onRemoving)

        MediaActionEvent.ShareLaunched -> {
            _state.update { it.copy(share = null) }
            MediaActionResult.None
        }

        MediaActionEvent.AddToAlbumDismissed -> {
            _state.update { it.copy(addToAlbumIds = null) }
            MediaActionResult.None
        }

        is MediaActionEvent.AddedToAlbum -> {
            _state.update { it.copy(addToAlbumIds = null) }
            MediaActionResult(UserMessage("Added to ${event.albumName}"), completed = true)
        }

        MediaActionEvent.VaultUnlocked -> {
            _state.update { it.copy(unlockingVault = false) }
            val items = awaitingUnlock
            awaitingUnlock = emptyList()
            if (items.isEmpty()) MediaActionResult.None else moveToSecrets(items)
        }

        MediaActionEvent.VaultUnlockDismissed -> {
            _state.update { it.copy(unlockingVault = false) }
            awaitingUnlock = emptyList()
            MediaActionResult.None
        }
    }

    private fun share(items: List<MediaItem>): MediaActionResult {
        val request = selectionActions.share(items) ?: return MediaActionResult.None
        _state.update { it.copy(share = request) }
        return MediaActionResult.None
    }

    private suspend fun favorite(items: List<MediaItem>): MediaActionResult {
        val favorited = selectionActions.favorite(items)
        val text = if (favorited) "Added to Favorites" else "Removed from Favorites"
        return MediaActionResult(UserMessage(text), completed = true)
    }

    private fun addToAlbum(items: List<MediaItem>): MediaActionResult {
        _state.update { it.copy(addToAlbumIds = items.map(MediaItem::id)) }
        return MediaActionResult.None
    }

    private suspend fun delete(
        items: List<MediaItem>,
        onRemoving: (List<Long>) -> Unit,
    ): MediaActionResult {
        val ids = items.map(MediaItem::id)
        return when (val outcome = selectionActions.requestDelete(items)) {
            is DeleteOutcome.NeedsConsent -> {
                askConsent(outcome.uris) { requestId -> PendingConsent.Delete(requestId, ids) }
                MediaActionResult.None
            }

            is DeleteOutcome.Deleted -> removed(ids, onRemoving)
            is DeleteOutcome.Failed -> MediaActionResult(UserMessage(outcome.reason))
        }
    }

    private suspend fun moveToSecrets(items: List<MediaItem>): MediaActionResult =
        when (val start = moveToSecrets.blocker() ?: encrypt(items)) {
            MoveStart.NeedsSetup -> MediaActionResult(UserMessage(SET_UP_SECRETS_FIRST))

            MoveStart.NeedsUnlock -> {
                awaitingUnlock = items
                _state.update { it.copy(unlockingVault = true) }
                MediaActionResult.None
            }

            is MoveStart.Failed -> MediaActionResult(UserMessage(start.reason))

            is MoveStart.NeedsConsent -> {
                askConsent(start.originalUris) { requestId ->
                    PendingConsent.Move(requestId, start.originalIds)
                }
                MediaActionResult.None
            }
        }

    private suspend fun encrypt(items: List<MediaItem>): MoveStart {
        _state.update { it.copy(progress = MoveProgress(done = 0, total = items.size)) }
        return try {
            moveToSecrets.start(items) { done ->
                _state.update { it.copy(progress = MoveProgress(done, items.size)) }
            }
        } finally {
            _state.update { it.copy(progress = null) }
        }
    }

    private suspend fun resolveConsent(
        event: MediaActionEvent.ConsentResolved,
        onRemoving: (List<Long>) -> Unit,
    ): MediaActionResult {
        val pending = pendingConsent?.takeIf { it.requestId == event.requestId }
            ?: return MediaActionResult.None
        pendingConsent = null
        _state.update { it.copy(consent = null) }
        return when (pending) {
            is PendingConsent.Delete ->
                if (event.confirmed) removed(pending.mediaIds, onRemoving) else MediaActionResult.None

            is PendingConsent.Move -> {
                if (event.confirmed) onRemoving(pending.mediaIds)
                val moved = moveToSecrets.resolve(event.confirmed)
                MediaActionResult(UserMessage(movedMessage(moved)), completed = true)
            }
        }
    }

    private suspend fun removed(ids: List<Long>, onRemoving: (List<Long>) -> Unit): MediaActionResult {
        onRemoving(ids)
        selectionActions.confirmDeleted(ids)
        val text = if (ids.size == 1) "Deleted 1 item" else "Deleted ${ids.size} items"
        return MediaActionResult(UserMessage(text), completed = true)
    }

    private fun askConsent(uris: List<String>, pending: (requestId: Long) -> PendingConsent) {
        val request = ConsentRequest(uris = uris)
        pendingConsent = pending(request.id)
        _state.update { it.copy(consent = request) }
    }

    private sealed interface PendingConsent {
        val requestId: Long

        data class Delete(override val requestId: Long, val mediaIds: List<Long>) : PendingConsent

        data class Move(override val requestId: Long, val mediaIds: List<Long>) : PendingConsent
    }
}
