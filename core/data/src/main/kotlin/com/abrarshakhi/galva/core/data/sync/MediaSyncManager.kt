package com.abrarshakhi.galva.core.data.sync

import com.abrarshakhi.galva.core.data.mapper.toEntity
import com.abrarshakhi.galva.core.database.dao.MediaDao
import com.abrarshakhi.galva.core.mediastore.MediaStoreReader
import com.abrarshakhi.galva.core.model.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal class MediaSyncManager(
    private val dataSource: MediaStoreReader,
    private val mediaDao: MediaDao,
    private val dispatcher: CoroutineDispatcher,
) {

    private val mutex = Mutex()
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    suspend fun sync() {
        mutex.withLock {
            _state.value = SyncState.Syncing
            try {
                withContext(dispatcher) { reconcile() }
                _state.value = SyncState.Idle
            } catch (cancellation: CancellationException) {
                _state.value = SyncState.Idle
                throw cancellation
            } catch (truncated: TruncatedMediaReadException) {
                _state.value = SyncState.Failed(truncated.message.orEmpty())
            } catch (security: SecurityException) {
                _state.value = SyncState.Failed(security.message ?: "Media permission denied")
            } catch (error: Exception) {
                _state.value = SyncState.Failed(error.message ?: "Could not read device media")
            }
        }
    }

    private suspend fun reconcile() {
        val remote = dataSource.queryFingerprints().associate { it.id to it.dateModifiedMs }
        val local = mediaDao.fingerprints().associate { it.id to it.dateModifiedMs }

        if (local.isNotEmpty() && remote.isEmpty()) {
            throw TruncatedMediaReadException(
                "Device media read returned nothing while ${local.size} items are indexed; " +
                    "skipped this sync rather than deleting them."
            )
        }

        val removedIds = local.keys.filterNot(remote::containsKey)

        if (removedIds.isImplausibleAgainst(local.size)) {
            confirmRemovalsOrAbort(local = local, firstPassRemovals = removedIds.size)
        }

        val staleIds = remote.mapNotNull { (id, modifiedMs) -> id.takeIf { local[id] != modifiedMs } }
        if (staleIds.isEmpty() && removedIds.isEmpty()) return

        val records = dataSource.queryByIds(staleIds)
        mediaDao.applySyncDelta(
            upserted = records.map { it.toEntity() },
            removedIds = removedIds,
        )
    }

    private fun List<Long>.isImplausibleAgainst(indexSize: Int): Boolean =
        indexSize >= GUARD_MIN_INDEX_SIZE &&
            size >= (indexSize * GUARD_REMOVAL_FRACTION).roundToInt()

    private suspend fun confirmRemovalsOrAbort(
        local: Map<Long, Long>,
        firstPassRemovals: Int,
    ) {
        val confirmation = dataSource.queryFingerprints().associate { it.id to it.dateModifiedMs }
        val confirmedRemovals = local.keys.count { it !in confirmation }

        if (confirmedRemovals != firstPassRemovals) {
            throw TruncatedMediaReadException(
                "Device media read was incomplete ($firstPassRemovals then $confirmedRemovals " +
                    "of ${local.size} items missing); skipped this sync rather than deleting them."
            )
        }
    }

    private companion object {
        const val GUARD_MIN_INDEX_SIZE = 20

        const val GUARD_REMOVAL_FRACTION = 0.5
    }
}

internal class TruncatedMediaReadException(message: String) : Exception(message)
