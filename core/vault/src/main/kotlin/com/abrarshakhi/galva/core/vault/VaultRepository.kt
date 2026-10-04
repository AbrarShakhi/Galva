package com.abrarshakhi.galva.core.vault

import com.abrarshakhi.galva.core.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.RestoreOutcome
import com.abrarshakhi.galva.core.model.UnlockResult
import com.abrarshakhi.galva.core.model.VaultState
import kotlinx.coroutines.flow.StateFlow

interface VaultRepository {

    val state: StateFlow<VaultState>

    fun newRecoveryPhrase(): List<String>

    suspend fun setUp(passphrase: CharSequence, recoveryPhrase: List<String>)

    suspend fun unlock(passphrase: CharSequence): UnlockResult

    suspend fun unlockWithRecoveryPhrase(words: List<String>): UnlockResult

    suspend fun changePassphrase(current: CharSequence?, new: CharSequence): Boolean

    fun lock()

    fun cancelPendingLock()

    suspend fun reset()

    suspend fun add(items: List<MediaItem>, onProgress: (done: Int) -> Unit = {}): AddToVaultOutcome

    suspend fun confirmMove(secretIds: Collection<Long>)

    suspend fun undoMove(secretIds: Collection<Long>)

    suspend fun delete(secretIds: Collection<Long>)

    suspend fun restore(secretIds: Collection<Long>): RestoreOutcome
}
