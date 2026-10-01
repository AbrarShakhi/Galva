package com.abrarshakhi.galva.core.vault

import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.data.VaultLockedException
import com.abrarshakhi.galva.core.vault.data.VaultRepositoryImpl
import com.abrarshakhi.galva.core.vault.data.VaultSession
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.abrarshakhi.galva.core.vault.data.store.VaultStore
import com.abrarshakhi.galva.core.vault.domain.model.AddToVaultOutcome
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem
import com.abrarshakhi.galva.core.vault.domain.model.UnlockResult
import com.abrarshakhi.galva.core.vault.domain.model.VaultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class VaultRepositoryImplTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val crypto = VaultCrypto()
    private val keyRing = FakeKeyRing()
    private val phrases = bip39()

    private lateinit var files: VaultFiles
    private lateinit var session: VaultSession
    private lateinit var encryptor: FakeEncryptor
    private lateinit var exporter: FakeExporter
    private lateinit var repository: VaultRepositoryImpl
    private lateinit var recoveryWords: List<String>

    @Before
    fun setUp() = runBlocking {
        files = VaultFiles(File(temp.root, "vault"))
        encryptor = FakeEncryptor(files, crypto)
        exporter = FakeExporter()
        repository = newRepository()
        recoveryWords = repository.newRecoveryPhrase()
        repository.setUp(PASSPHRASE, recoveryWords)
    }

    /** A repository over the same files, as after the process restarts. */
    private fun newRepository(): VaultRepositoryImpl {
        session = VaultSession()
        return VaultRepositoryImpl(
            store = VaultStore(files, keyRing, FakeKdf(), crypto),
            files = files,
            session = session,
            crypto = crypto,
            phrases = lazyOf(phrases),
            encryptor = encryptor,
            exporter = exporter,
            dispatcher = Dispatchers.IO,
        )
    }

    private val items: List<SecretItem>
        get() = (repository.state.value as VaultState.Unlocked).items

    private suspend fun addToVault(vararg ids: Long): AddToVaultOutcome.Added {
        val media = ids.map { mediaItem(it, favorite = it % 2 == 0L) }
        encryptor.galleryUris += media.map { it.uri }
        return repository.add(media) as AddToVaultOutcome.Added
    }

    @Test
    fun `setting up leaves an empty, unlocked vault`() {
        assertEquals(VaultState.Unlocked(emptyList()), repository.state.value)
        assertEquals(24, recoveryWords.size)
    }

    @Test
    fun `a restarted app finds the vault locked`() = runBlocking {
        addToVault(1)

        repository = newRepository()

        assertEquals(VaultState.Locked, repository.state.value)
        assertEquals(UnlockResult.WrongSecret, repository.unlock("not it"))
        assertEquals(UnlockResult.Unlocked, repository.unlock(PASSPHRASE))
        assertEquals(listOf(1L).size, items.size)
    }

    @Test
    fun `moving in commits pending entries before the originals are touched`() = runBlocking {
        val added = addToVault(1, 2)

        assertEquals(listOf(1L, 2L), added.originalIds)
        assertEquals(2, items.size)
        assertTrue(items.all { it.pendingOriginal != null })

        // Committed to disk, not just memory.
        repository = newRepository()
        repository.unlock(PASSPHRASE)
        assertEquals(2, items.size)
        assertTrue(items.all { it.pendingOriginal != null })
    }

    @Test
    fun `confirming a move clears the pending marks`() = runBlocking {
        val added = addToVault(1, 2)

        repository.confirmMove(added.secretIds)

        assertTrue(items.none { it.pendingOriginal != null })
    }

    @Test
    fun `undoing a move destroys the copies and rotates`() = runBlocking {
        val added = addToVault(1, 2)
        val aliasBefore = keyRing.aliases().single()

        repository.undoMove(added.secretIds)

        assertTrue(items.isEmpty())
        added.secretIds.forEach { id ->
            assertFalse(files.blob(id).exists())
            assertFalse(files.thumbnail(id).exists())
        }
        assertNotEquals(aliasBefore, keyRing.aliases().single())
    }

    @Test
    fun `an interrupted move is settled on unlock once the originals are gone`() = runBlocking {
        addToVault(1, 2)
        // The delete dialog was confirmed for item 1, then the process died before the vault heard.
        encryptor.galleryUris -= mediaItem(1).uri
        repository.lock()

        repository.unlock(PASSPHRASE)

        val byName = items.associateBy { it.displayName }
        assertNull(byName.getValue("IMG_1.jpg").pendingOriginal)
        assertNotNull(byName.getValue("IMG_2.jpg").pendingOriginal)
    }

    @Test
    fun `a failure part-way through a move commits nothing`() = runBlocking {
        val media = listOf(mediaItem(1), mediaItem(2), mediaItem(3))
        encryptor.beforeEncrypt = { item -> if (item.id == 3L) error("disk full") }

        val outcome = repository.add(media)

        assertTrue(outcome is AddToVaultOutcome.Failed)
        assertTrue(items.isEmpty())
        assertTrue(File(files.root, "blobs").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `restoring writes the original bytes back and empties the vault`() = runBlocking {
        val added = addToVault(1, 2)

        val outcome = repository.restore(added.secretIds)

        assertNull(outcome.failure)
        assertEquals(2, outcome.restoredMediaIds.size)
        assertEquals(1, outcome.favoriteMediaIds.size)
        val restored = exporter.written.values.map(::String).toSet()
        assertEquals(setOf("original bytes of 1", "original bytes of 2"), restored)
        assertTrue(items.isEmpty())
    }

    @Test
    fun `a partial restore keeps what the gallery refused`() = runBlocking {
        val added = addToVault(1, 2)
        exporter.refuse += "IMG_2.jpg"

        val outcome = repository.restore(added.secretIds)

        assertNotNull(outcome.failure)
        assertEquals(1, outcome.restoredMediaIds.size)
        assertEquals(listOf("IMG_2.jpg"), items.map { it.displayName })
    }

    @Test
    fun `the recovery phrase opens the vault and forces a new passphrase`() = runBlocking {
        addToVault(1)
        repository.lock()

        assertEquals(UnlockResult.Unlocked, repository.unlockWithRecoveryPhrase(recoveryWords))
        assertTrue((repository.state.value as VaultState.Unlocked).needsNewPassphrase)

        assertTrue(repository.changePassphrase(current = null, new = "a brand new passphrase"))
        assertFalse((repository.state.value as VaultState.Unlocked).needsNewPassphrase)

        repository.lock()
        assertEquals(UnlockResult.WrongSecret, repository.unlock(PASSPHRASE))
        assertEquals(UnlockResult.Unlocked, repository.unlock("a brand new passphrase"))
        assertEquals(1, items.size)
    }

    @Test
    fun `a mistyped recovery phrase is reported as such`() = runBlocking {
        repository.lock()
        val typo = recoveryWords.toMutableList().apply { this[3] = "zzzz" }

        assertEquals(UnlockResult.InvalidPhrase, repository.unlockWithRecoveryPhrase(typo))
        assertEquals(VaultState.Locked, repository.state.value)
    }

    @Test
    fun `changing the passphrase needs the current one`() = runBlocking {
        assertFalse(repository.changePassphrase(current = null, new = "whatever it is"))
        assertFalse(repository.changePassphrase(current = "wrong", new = "whatever it is"))
        assertTrue(repository.changePassphrase(current = PASSPHRASE, new = "whatever it is"))
    }

    @Test
    fun `a lock requested mid-operation waits for it to finish`() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        encryptor.beforeEncrypt = {
            started.countDown()
            release.await(10, TimeUnit.SECONDS)
        }
        encryptor.galleryUris += mediaItem(1).uri

        val move = async(Dispatchers.IO) { repository.add(listOf(mediaItem(1))) }
        assertTrue(started.await(10, TimeUnit.SECONDS))
        repository.lock()
        assertTrue(repository.state.value is VaultState.Unlocked)

        release.countDown()
        assertTrue(move.await() is AddToVaultOutcome.Added)
        assertEquals(VaultState.Locked, repository.state.value)

        // The move landed intact before the lock.
        repository.unlock(PASSPHRASE)
        assertEquals(1, items.size)
    }

    @Test
    fun `a pending lock is cancelled when the user comes back`() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        encryptor.beforeEncrypt = {
            started.countDown()
            release.await(10, TimeUnit.SECONDS)
        }

        val move = async(Dispatchers.IO) { repository.add(listOf(mediaItem(1))) }
        assertTrue(started.await(10, TimeUnit.SECONDS))
        repository.lock()
        repository.cancelPendingLock()
        release.countDown()
        move.await()

        assertTrue(repository.state.value is VaultState.Unlocked)
    }

    @Test
    fun `content is readable only while unlocked, with random access`() = runBlocking {
        val original = Random(42).nextBytes(3 * 1024 * 1024 + 12_345)
        encryptor.content = { original }
        val id = addToVault(1).secretIds.single()
        val content = VaultContent(session, files, crypto)

        content.openOriginal(id).use { channel ->
            assertEquals(original.size.toLong(), channel.size())
            val random = Random(7)
            repeat(40) {
                // Include reads that straddle the 1 MiB segment boundaries.
                val position = if (it % 4 == 0) (1024L * 1024 * (1 + it % 3)) - 10 else random.nextLong(original.size.toLong())
                val length = minOf(4096, original.size - position.toInt())
                val buffer = ByteBuffer.allocate(length)
                channel.position(position)
                while (buffer.hasRemaining() && channel.read(buffer) > 0) Unit
                assertArrayEquals(original.copyOfRange(position.toInt(), position.toInt() + length), buffer.array())
            }
        }
        assertArrayEquals(FakeEncryptor.thumbnailOf(mediaItem(1)), content.readThumbnail(id))

        repository.lock()
        try {
            content.readThumbnail(id)
            throw AssertionError("Read a thumbnail from a locked vault")
        } catch (expected: VaultLockedException) {
        }
    }

    @Test
    fun `deleting rotates and removes the files`() = runBlocking {
        val added = addToVault(1, 2)
        val aliasBefore = keyRing.aliases().single()

        repository.delete(listOf(added.secretIds.first()))

        assertEquals(1, items.size)
        assertFalse(files.blob(added.secretIds.first()).exists())
        assertTrue(files.blob(added.secretIds.last()).exists())
        assertNotEquals(aliasBefore, keyRing.aliases().single())
    }

    @Test
    fun `reset destroys everything`() = runBlocking {
        addToVault(1)

        repository.reset()

        assertEquals(VaultState.NotSetUp, repository.state.value)
        assertTrue(keyRing.aliases().isEmpty())
        assertFalse(files.root.exists())
    }

    private companion object {
        const val PASSPHRASE = "correct horse battery staple"
    }
}
