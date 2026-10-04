package com.abrarshakhi.galva.core.ui.actions

import com.abrarshakhi.galva.core.data.repository.MediaRepository
import com.abrarshakhi.galva.core.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.model.DeleteOutcome
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaSource
import com.abrarshakhi.galva.core.model.MediaType
import com.abrarshakhi.galva.core.model.RestoreOutcome
import com.abrarshakhi.galva.core.model.SyncState
import com.abrarshakhi.galva.core.model.UnlockResult
import com.abrarshakhi.galva.core.model.VaultState
import com.abrarshakhi.galva.core.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class FakeMediaRepository(private val log: MutableList<String>) : MediaRepository {

    val favorites = mutableMapOf<Long, Boolean>()
    val forgotten = mutableListOf<Long>()
    var deleteOutcome: (Collection<Long>) -> DeleteOutcome = { ids ->
        DeleteOutcome.NeedsConsent(ids.map(::uriOf))
    }

    override fun observeMedia(source: MediaSource): Flow<List<MediaItem>> = flowOf(emptyList())

    override suspend fun getMedia(id: Long): MediaItem? = null

    override suspend fun setFavorite(ids: Collection<Long>, favorite: Boolean) {
        ids.forEach { favorites[it] = favorite }
    }

    override suspend fun delete(ids: Collection<Long>): DeleteOutcome = deleteOutcome(ids)

    override suspend fun forgetDeleted(ids: Collection<Long>) {
        log += "forget $ids"
        forgotten += ids
    }

    override val syncState: Flow<SyncState> = flowOf(SyncState.Idle)

    override suspend fun sync() = Unit
}

internal class FakeVaultRepository(private val log: MutableList<String>) : VaultRepository {

    override val state = MutableStateFlow<VaultState>(VaultState.Unlocked(items = emptyList()))
    var addOutcome: (List<MediaItem>) -> AddToVaultOutcome = { items ->
        AddToVaultOutcome.Added(
            secretIds = items.map { it.id + SECRET_ID_OFFSET },
            originalIds = items.map(MediaItem::id),
            originalUris = items.map(MediaItem::uri),
        )
    }

    override fun newRecoveryPhrase(): List<String> = emptyList()

    override suspend fun setUp(passphrase: CharSequence, recoveryPhrase: List<String>) = Unit

    override suspend fun unlock(passphrase: CharSequence): UnlockResult = UnlockResult.Unlocked

    override suspend fun unlockWithRecoveryPhrase(words: List<String>): UnlockResult =
        UnlockResult.Unlocked

    override suspend fun changePassphrase(current: CharSequence?, new: CharSequence): Boolean = true

    override fun lock() = Unit

    override fun cancelPendingLock() = Unit

    override suspend fun reset() = Unit

    override suspend fun add(
        items: List<MediaItem>,
        onProgress: (done: Int) -> Unit,
    ): AddToVaultOutcome {
        log += "add ${items.map(MediaItem::id)}"
        items.indices.forEach { onProgress(it + 1) }
        return addOutcome(items)
    }

    override suspend fun confirmMove(secretIds: Collection<Long>) {
        log += "confirm $secretIds"
    }

    override suspend fun undoMove(secretIds: Collection<Long>) {
        log += "undo $secretIds"
    }

    override suspend fun delete(secretIds: Collection<Long>) = Unit

    override suspend fun restore(secretIds: Collection<Long>): RestoreOutcome =
        RestoreOutcome(emptyList(), emptyList())

    companion object {
        const val SECRET_ID_OFFSET = 1_000L
    }
}

internal fun mediaItem(id: Long, favorite: Boolean = false): MediaItem = MediaItem(
    id = id,
    uri = uriOf(id),
    displayName = "IMG_$id.jpg",
    mimeType = "image/jpeg",
    type = MediaType.IMAGE,
    sizeBytes = 1_024L,
    width = 400,
    height = 300,
    durationMs = 0L,
    dateTakenMs = id,
    dateModifiedMs = id,
    albumId = 1L,
    albumName = "Camera",
    isFavorite = favorite,
)

internal fun uriOf(id: Long): String = "content://media/external/images/media/$id"
