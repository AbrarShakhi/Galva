package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.vault.data.crypto.KeyUnavailableException
import com.abrarshakhi.galva.core.vault.data.crypto.RecoveryPhrase
import com.abrarshakhi.galva.core.vault.data.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.crypto.toPassphraseBytes
import com.abrarshakhi.galva.core.vault.data.crypto.wipe
import com.abrarshakhi.galva.core.vault.data.store.OpenVault
import com.abrarshakhi.galva.core.vault.data.store.SecretEntry
import com.abrarshakhi.galva.core.vault.data.store.VaultCorruptedException
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.abrarshakhi.galva.core.vault.data.store.VaultIndex
import com.abrarshakhi.galva.core.vault.data.store.VaultStore
import com.abrarshakhi.galva.core.vault.data.store.WrongSecretException
import com.abrarshakhi.galva.core.vault.domain.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.vault.domain.model.RestoreOutcome
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem
import com.abrarshakhi.galva.core.vault.domain.model.UnlockResult
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import com.abrarshakhi.galva.core.vault.domain.repository.VaultRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.OutputStream

/**
 * The vault's operations, serialised and kept consistent with what is on disk.
 *
 * Two orderings carry the data-safety guarantees:
 * - Moving in commits the new entries *before* anything touches the originals, so an interruption
 *   leaves a copy in both places rather than in neither.
 * - Removing rotates the container *before* deleting files, so a removed item's key is already
 *   unrecoverable by the time its ciphertext is unlinked.
 */
class VaultRepositoryImpl(
    private val store: VaultStore,
    private val files: VaultFiles,
    private val session: VaultSession,
    private val crypto: VaultCrypto,
    private val phrases: Lazy<RecoveryPhrase>,
    private val encryptor: SecretEncryptor,
    private val exporter: SecretExporter,
    private val dispatcher: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
) : VaultRepository {

    private val mutex = Mutex()

    /** Guards [running] and [lockPending], which [lock] reads from the main thread. */
    private val lockGuard = Any()
    private var running = 0
    private var lockPending = false

    @Volatile
    private var needsNewPassphrase = false

    private val _state =
        MutableStateFlow(if (store.exists()) VaultState.Locked else VaultState.NotSetUp)
    override val state: StateFlow<VaultState> = _state.asStateFlow()

    override fun newRecoveryPhrase(): List<String> {
        val entropy = crypto.randomBytes(RecoveryPhrase.ENTROPY_BYTES)
        return phrases.value.encode(entropy).also { entropy.wipe() }
    }

    override suspend fun setUp(passphrase: CharSequence, recoveryPhrase: List<String>) = operation {
        check(!store.exists()) { "Secrets is already set up" }
        val recoveryKey = requireNotNull(phrases.value.decode(recoveryPhrase)) {
            "Invalid recovery phrase"
        }
        val secret = passphrase.toPassphraseBytes()
        try {
            removeStaleKeys()
            session.open(store.create(secret, recoveryKey))
        } finally {
            secret.wipe()
            recoveryKey.wipe()
        }
        needsNewPassphrase = false
        publish()
    }

    override suspend fun unlock(passphrase: CharSequence): UnlockResult = operation {
        if (session.current != null) return@operation UnlockResult.Unlocked
        val secret = passphrase.toPassphraseBytes()
        try {
            open { store.openWithPassphrase(secret) }
        } finally {
            secret.wipe()
        }
    }

    override suspend fun unlockWithRecoveryPhrase(words: List<String>): UnlockResult = operation {
        if (session.current != null) return@operation UnlockResult.Unlocked
        val recoveryKey = phrases.value.decode(words) ?: return@operation UnlockResult.InvalidPhrase
        try {
            open { store.openWithRecoveryKey(recoveryKey) }.also { result ->
                if (result == UnlockResult.Unlocked) {
                    needsNewPassphrase = true
                    publish()
                }
            }
        } finally {
            recoveryKey.wipe()
        }
    }

    override suspend fun changePassphrase(current: CharSequence?, new: CharSequence): Boolean =
        operation {
            val vault = session.current ?: return@operation false
            if (!needsNewPassphrase) {
                val currentSecret = current?.toPassphraseBytes() ?: return@operation false
                val matches = try {
                    store.matchesPassphrase(vault, currentSecret)
                } finally {
                    currentSecret.wipe()
                }
                if (!matches) return@operation false
            }
            val newSecret = new.toPassphraseBytes()
            try {
                session.open(store.changePassphrase(vault, newSecret))
            } finally {
                newSecret.wipe()
            }
            needsNewPassphrase = false
            publish()
            true
        }

    override fun lock() {
        val sealNow = synchronized(lockGuard) {
            if (running > 0) lockPending = true
            running == 0
        }
        if (sealNow) seal()
    }

    override fun cancelPendingLock() {
        synchronized(lockGuard) { lockPending = false }
    }

    override suspend fun reset() = operation {
        session.close()
        store.destroy()
        needsNewPassphrase = false
        publish()
    }

    override suspend fun add(
        items: List<MediaItem>,
        onProgress: (done: Int) -> Unit,
    ): AddToVaultOutcome = operation {
        val vault = session.current ?: return@operation AddToVaultOutcome.Failed(LOCKED)
        if (items.isEmpty()) return@operation AddToVaultOutcome.Failed("Nothing was selected")
        // Both copies exist until the originals are deleted, so the whole size is needed up front.
        val needed = items.sumOf { it.sizeBytes.coerceAtLeast(0L) } + FREE_SPACE_MARGIN_BYTES
        if (files.usableSpace() < needed) {
            return@operation AddToVaultOutcome.Failed("There isn't enough free space to move these")
        }

        val taken = vault.index.entries.mapTo(HashSet(), SecretEntry::id)
        val added = ArrayList<SecretEntry>(items.size)
        try {
            items.forEachIndexed { done, item ->
                added += encryptor.encrypt(item, newId(taken), clock())
                onProgress(done + 1)
            }
            val index = VaultIndex(vault.index.entries + added)
            session.open(store.write(vault, index, rotate = false))
        } catch (error: Exception) {
            // Nothing was committed, so these files' keys exist only in memory and die here.
            added.forEach { entry ->
                files.deleteItem(entry.id)
                entry.keyset.wipe()
            }
            return@operation AddToVaultOutcome.Failed(
                error.message ?: "Couldn't move these to Secrets",
            )
        }
        publish()
        AddToVaultOutcome.Added(
            secretIds = added.map(SecretEntry::id),
            originalIds = items.map(MediaItem::id),
            originalUris = items.map(MediaItem::uri),
        )
    }

    override suspend fun confirmMove(secretIds: Collection<Long>) = operation {
        settle(secretIds.toSet())
        publish()
    }

    override suspend fun undoMove(secretIds: Collection<Long>) = operation {
        remove(secretIds.toSet())
    }

    override suspend fun delete(secretIds: Collection<Long>) = operation {
        remove(secretIds.toSet())
    }

    override suspend fun restore(secretIds: Collection<Long>): RestoreOutcome = operation {
        val vault = session.current
            ?: return@operation RestoreOutcome(emptyList(), emptyList(), failure = LOCKED)
        val restored = ArrayList<Pair<SecretEntry, Long>>(secretIds.size)
        var failure: String? = null
        for (entry in secretIds.mapNotNull(vault::entry)) {
            try {
                restored += entry to exporter.export(entry) { out -> decryptOriginal(entry, out) }
            } catch (error: Exception) {
                failure = error.message ?: "Couldn't restore ${entry.displayName}"
                break
            }
        }
        // Only what actually reached the gallery leaves the vault.
        if (restored.isNotEmpty()) remove(restored.mapTo(HashSet()) { (entry, _) -> entry.id })
        RestoreOutcome(
            restoredMediaIds = restored.map { (_, mediaId) -> mediaId },
            favoriteMediaIds = restored
                .filter { (entry, _) -> entry.wasFavorite }
                .map { (_, mediaId) -> mediaId },
            failure = failure,
        )
    }

    /**
     * Runs [block] off the main thread, one operation at a time. A lock requested meanwhile waits
     * for it, so no operation has its keys wiped halfway through rewriting the vault.
     */
    private suspend fun <T> operation(block: () -> T): T = withContext(dispatcher) {
        mutex.withLock {
            synchronized(lockGuard) { running++ }
            try {
                block()
            } finally {
                val sealNow = synchronized(lockGuard) {
                    running--
                    val due = running == 0 && lockPending
                    if (due) lockPending = false
                    due
                }
                if (sealNow) seal()
            }
        }
    }

    private fun open(read: () -> OpenVault): UnlockResult {
        removeStaleKeys()
        val vault = try {
            read()
        } catch (wrong: WrongSecretException) {
            return UnlockResult.WrongSecret
        } catch (gone: KeyUnavailableException) {
            return UnlockResult.Unavailable(HARDWARE_KEY_GONE)
        } catch (damaged: VaultCorruptedException) {
            return UnlockResult.Unavailable(DAMAGED)
        }
        session.open(vault)
        needsNewPassphrase = false
        tidyAfterUnlock()
        publish()
        return UnlockResult.Unlocked
    }

    /**
     * Finishes moves whose originals have gone from the gallery since, and deletes files no entry
     * accounts for. Both are left over from an interruption; neither may block an unlock.
     */
    private fun tidyAfterUnlock() {
        runCatching {
            val entries = session.current?.index?.entries.orEmpty()
            val gone = entries.filter { entry ->
                val original = entry.pendingOriginalUri
                original != null && !encryptor.originalExists(original)
            }
            settle(gone.mapTo(HashSet(), SecretEntry::id))
        }
        runCatching {
            val entries = session.current?.index?.entries ?: return@runCatching
            files.deleteUnreferenced(entries.mapTo(HashSet(), SecretEntry::id))
        }
    }

    /** Clears the pending mark from [ids]: their originals have left the gallery. */
    private fun settle(ids: Set<Long>) {
        val vault = session.current ?: return
        val entries = vault.index.entries
        if (entries.none { it.id in ids && it.pendingOriginalUri != null }) return
        val settled = entries.map { if (it.id in ids) it.settled() else it }
        session.open(store.write(vault, VaultIndex(settled), rotate = false))
    }

    /** Rotates first, so the removed items' keys are unrecoverable before their files go. */
    private fun remove(ids: Set<Long>) {
        val vault = session.current ?: return
        val (removed, remaining) = vault.index.entries.partition { it.id in ids }
        if (removed.isEmpty()) return
        session.open(store.write(vault, VaultIndex(remaining), rotate = true))
        removed.forEach { entry ->
            files.deleteItem(entry.id)
            entry.keyset.wipe()
        }
        publish()
    }

    private fun decryptOriginal(entry: SecretEntry, out: OutputStream) {
        crypto.streamingAead(entry.keyset)
            .newDecryptingStream(FileInputStream(files.blob(entry.id)), VaultContext.blob(entry.id))
            .use { plaintext -> plaintext.copyTo(out, COPY_BUFFER_BYTES) }
    }

    private fun removeStaleKeys() {
        runCatching { store.removeStaleKeys() }
    }

    private fun newId(taken: MutableSet<Long>): Long {
        while (true) {
            val id = crypto.randomId()
            if (id != 0L && taken.add(id)) return id
        }
    }

    private fun seal() {
        session.close()
        needsNewPassphrase = false
        publish()
    }

    private fun publish() {
        val vault = session.current
        _state.value = when {
            vault != null -> VaultState.Unlocked(
                items = vault.index.entries.map(SecretEntry::toSecretItem).sortedWith(NEWEST_FIRST),
                needsNewPassphrase = needsNewPassphrase,
            )

            store.exists() -> VaultState.Locked
            else -> VaultState.NotSetUp
        }
    }

    private companion object {
        const val LOCKED = "Secrets is locked"
        const val HARDWARE_KEY_GONE =
            "This phone's secure hardware no longer holds the key to Secrets, so it can't be " +
                "opened with any passphrase or recovery phrase."
        const val DAMAGED = "The Secrets file is damaged and can't be opened."
        const val FREE_SPACE_MARGIN_BYTES = 64L * 1024 * 1024
        const val COPY_BUFFER_BYTES = 256 * 1024

        val NEWEST_FIRST: Comparator<SecretItem> =
            compareByDescending<SecretItem> { it.dateTakenMs }.thenByDescending { it.addedAtMs }
    }
}
