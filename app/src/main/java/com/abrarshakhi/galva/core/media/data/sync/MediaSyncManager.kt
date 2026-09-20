package com.abrarshakhi.galva.core.media.data.sync

import com.abrarshakhi.galva.core.media.data.local.dao.MediaDao
import com.abrarshakhi.galva.core.media.data.local.dao.MediaFingerprint
import com.abrarshakhi.galva.core.media.data.mapper.toEntity
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreReader
import com.abrarshakhi.galva.core.media.domain.model.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Reconciles the Room mirror with MediaStore.
 *
 * The pass is a fingerprint diff, not a full re-read: ids and `DATE_MODIFIED` are pulled for the
 * whole collection (cheap), compared against the index, and only rows that are new or actually
 * changed are re-read in full. That keeps a launch-time sync proportional to what changed rather
 * than to library size.
 *
 * A [Mutex] serialises passes so overlapping triggers — launch, resume and a burst of
 * `ContentObserver` notifications — collapse into one run instead of racing on the same rows.
 */
class MediaSyncManager(
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
                // Deliberately not applied. The next trigger retries from a clean slate.
                _state.value = SyncState.Failed(truncated.message.orEmpty())
            } catch (security: SecurityException) {
                // The media permission was revoked while a pass was in flight.
                _state.value = SyncState.Failed(security.message ?: "Media permission denied")
            } catch (error: Exception) {
                _state.value = SyncState.Failed(error.message ?: "Could not read device media")
            }
        }
    }

    private suspend fun reconcile() {
        val remote = dataSource.queryFingerprints().asMap()
        val local = mediaDao.fingerprints().asMap()

        // An empty read is never authoritative, however many times it repeats: a revoked
        // permission or a volume mid-remount both produce one, and both look identical to "the
        // user deleted everything". Confirming it would only confirm the same broken read.
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

    /**
     * A MediaStore query can come back empty or truncated while the volume is being re-scanned.
     * Applied blindly that reads as "the user deleted everything", and because `favorites` is
     * foreign-keyed to the media table the deletion cascades — destroying the only data in this app
     * the user actually authored, permanently, with no way to tell it happened.
     *
     * So a pass that wants to drop a large share of the index has to prove it: the collection is
     * read a second time, and the removals are only applied if both reads agree. A genuine bulk
     * delete reproduces; a transient bad read does not.
     *
     * Known limitation: if the user really does delete every photo from another app, the index
     * keeps those rows until some non-empty read arrives. Stale rows are recoverable — a cascaded
     * `favorites` table is not — so the trade is deliberate.
     */
    private fun List<Long>.isImplausibleAgainst(indexSize: Int): Boolean =
        indexSize >= GUARD_MIN_INDEX_SIZE &&
            size >= (indexSize * GUARD_REMOVAL_FRACTION).roundToInt()

    private suspend fun confirmRemovalsOrAbort(
        local: Map<Long, Long>,
        firstPassRemovals: Int,
    ) {
        val confirmation = dataSource.queryFingerprints().asMap()
        val confirmedRemovals = local.keys.count { it !in confirmation }

        if (confirmedRemovals != firstPassRemovals) {
            throw TruncatedMediaReadException(
                "Device media read was incomplete ($firstPassRemovals then $confirmedRemovals " +
                    "of ${local.size} items missing); skipped this sync rather than deleting them."
            )
        }
    }

    private fun List<MediaFingerprint>.asMap(): Map<Long, Long> =
        associate { it.id to it.dateModifiedMs }

    private companion object {
        /** Below this, a full clear-out is cheap to rebuild and not worth second-guessing. */
        const val GUARD_MIN_INDEX_SIZE = 20

        /** Removing at least half the library in one pass is worth confirming. */
        const val GUARD_REMOVAL_FRACTION = 0.5
    }
}

/** A MediaStore read that returned too little to be trusted as a source of deletions. */
class TruncatedMediaReadException(message: String) : Exception(message)
