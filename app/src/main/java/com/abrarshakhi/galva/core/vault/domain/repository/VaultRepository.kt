package com.abrarshakhi.galva.core.vault.domain.repository

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.vault.domain.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.vault.domain.model.RestoreOutcome
import com.abrarshakhi.galva.core.vault.domain.model.UnlockResult
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import kotlinx.coroutines.flow.StateFlow

/**
 * The Secrets vault: an encrypted store only the user's passphrase or recovery phrase can open.
 *
 * Every change that removes something — delete, restore, an undone move, a new passphrase —
 * re-keys the vault's outer layer with a fresh hardware key and destroys the old one, so older
 * copies of the vault left behind on flash can never be opened again, even with the passphrase.
 */
interface VaultRepository {

    val state: StateFlow<VaultState>

    /** A fresh 24-word recovery phrase for setup. Nothing is stored until [setUp] uses it. */
    fun newRecoveryPhrase(): List<String>

    /** Creates the vault and leaves it unlocked. */
    suspend fun setUp(passphrase: CharSequence, recoveryPhrase: List<String>)

    suspend fun unlock(passphrase: CharSequence): UnlockResult

    /** Opens the vault with the recovery phrase; the passphrase must then be replaced. */
    suspend fun unlockWithRecoveryPhrase(words: List<String>): UnlockResult

    /**
     * Replaces the passphrase. [current] must match unless the vault was just opened with the
     * recovery phrase. Returns false when [current] is wrong.
     */
    suspend fun changePassphrase(current: CharSequence?, new: CharSequence): Boolean

    /** Seals the vault now, or as soon as a running operation finishes. */
    fun lock()

    /** Cancels a lock that is waiting for an operation to finish; the user came back in time. */
    fun cancelPendingLock()

    /** Destroys the vault and everything in it. There is no undo. */
    suspend fun reset()

    /**
     * Encrypts [items] into the vault and commits them, marked pending, before anything touches the
     * originals: a crash after this leaves two copies, never none.
     */
    suspend fun add(items: List<MediaItem>, onProgress: (done: Int) -> Unit = {}): AddToVaultOutcome

    /** The originals were deleted from the gallery, so the pending marks come off. */
    suspend fun confirmMove(secretIds: Collection<Long>)

    /** The originals stayed in the gallery, so the vault copies are destroyed. */
    suspend fun undoMove(secretIds: Collection<Long>)

    /** Destroys the items. There is no undo, and no copy of their keys survives. */
    suspend fun delete(secretIds: Collection<Long>)

    /** Decrypts the items back into the gallery, then destroys the vault copies. */
    suspend fun restore(secretIds: Collection<Long>): RestoreOutcome
}
