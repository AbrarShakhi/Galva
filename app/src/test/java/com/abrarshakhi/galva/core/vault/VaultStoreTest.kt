package com.abrarshakhi.galva.core.vault

import com.abrarshakhi.galva.core.vault.data.crypto.KeyUnavailableException
import com.abrarshakhi.galva.core.vault.data.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.store.SecretEntry
import com.abrarshakhi.galva.core.vault.data.store.VaultCorruptedException
import com.abrarshakhi.galva.core.vault.data.store.VaultFiles
import com.abrarshakhi.galva.core.vault.data.store.VaultIndex
import com.abrarshakhi.galva.core.vault.data.store.VaultStore
import com.abrarshakhi.galva.core.vault.data.store.WrongSecretException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VaultStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val crypto = VaultCrypto()
    private val keyRing = FakeKeyRing()
    private val files by lazy { VaultFiles(File(temp.root, "vault")) }
    private val store by lazy { VaultStore(files, keyRing, FakeKdf(), crypto) }

    private val passphrase = "correct horse battery".toByteArray()
    private val recoveryKey = ByteArray(32) { 7 }

    private fun entry(id: Long) = SecretEntry(
        id = id,
        keyset = crypto.newFileKeyset(),
        type = "IMAGE",
        mimeType = "image/jpeg",
        displayName = "IMG_$id.jpg",
        sizeBytes = 1,
        width = 1,
        height = 1,
        durationMs = 0,
        dateTakenMs = id,
        addedAtMs = id,
    )

    private val containerFile get() = File(files.root, "vault.bin")

    @Test
    fun `the passphrase and the recovery key both open a new vault`() {
        store.create(passphrase, recoveryKey)

        assertTrue(store.openWithPassphrase(passphrase).index.entries.isEmpty())
        assertTrue(store.openWithRecoveryKey(recoveryKey).index.entries.isEmpty())
    }

    @Test(expected = WrongSecretException::class)
    fun `a wrong passphrase is rejected`() {
        store.create(passphrase, recoveryKey)
        store.openWithPassphrase("wrong horse".toByteArray())
    }

    @Test(expected = WrongSecretException::class)
    fun `a wrong recovery key is rejected`() {
        store.create(passphrase, recoveryKey)
        store.openWithRecoveryKey(ByteArray(32) { 8 })
    }

    @Test
    fun `adding does not rotate`() {
        val vault = store.create(passphrase, recoveryKey)
        val aliasBefore = keyRing.aliases().single()

        store.write(vault, VaultIndex(listOf(entry(1))), rotate = false)

        assertEquals(aliasBefore, keyRing.aliases().single())
        assertEquals(listOf(1L), store.openWithPassphrase(passphrase).index.entries.map { it.id })
    }

    @Test
    fun `an older copy of the vault cannot be opened after a removal, even with the passphrase`() {
        var vault = store.create(passphrase, recoveryKey)
        vault = store.write(vault, VaultIndex(listOf(entry(1), entry(2))), rotate = false)
        // What a forensic image of the flash could still hold after the delete below.
        val oldCopy = containerFile.readBytes()

        store.write(vault, VaultIndex(listOf(entry(2))), rotate = true)
        val current = containerFile.readBytes()

        containerFile.writeBytes(oldCopy)
        assertThrowsKeyUnavailable { store.openWithPassphrase(passphrase) }
        assertThrowsKeyUnavailable { store.openWithRecoveryKey(recoveryKey) }

        containerFile.writeBytes(current)
        assertEquals(listOf(2L), store.openWithPassphrase(passphrase).index.entries.map { it.id })
        assertEquals(1, keyRing.aliases().size)
    }

    @Test
    fun `changing the passphrase retires the old one everywhere`() {
        val vault = store.create(passphrase, recoveryKey)
        val beforeChange = containerFile.readBytes()
        val newPassphrase = "a much better passphrase".toByteArray()

        store.changePassphrase(vault, newPassphrase)

        assertTrue(store.openWithPassphrase(newPassphrase).index.entries.isEmpty())
        assertTrue(store.openWithRecoveryKey(recoveryKey).index.entries.isEmpty())
        try {
            store.openWithPassphrase(passphrase)
            throw AssertionError("The old passphrase still opens the vault")
        } catch (expected: WrongSecretException) {
        }
        // Nor can the old passphrase open a copy of the file from before the change.
        containerFile.writeBytes(beforeChange)
        assertThrowsKeyUnavailable { store.openWithPassphrase(passphrase) }
    }

    @Test
    fun `matchesPassphrase checks without side effects`() {
        val vault = store.create(passphrase, recoveryKey)
        val alias = keyRing.aliases().single()

        assertTrue(store.matchesPassphrase(vault, passphrase))
        assertFalse(store.matchesPassphrase(vault, "nope".toByteArray()))
        assertEquals(alias, keyRing.aliases().single())
    }

    @Test
    fun `stale keys are removed but the current one is kept`() {
        store.create(passphrase, recoveryKey)
        val current = keyRing.aliases().single()
        keyRing.plantOrphan()

        store.removeStaleKeys()

        assertEquals(setOf(current), keyRing.aliases())
        assertTrue(store.openWithPassphrase(passphrase).index.entries.isEmpty())
    }

    @Test
    fun `an unreadable container keeps every key`() {
        store.create(passphrase, recoveryKey)
        keyRing.plantOrphan()
        containerFile.writeBytes("garbage".toByteArray())

        store.removeStaleKeys()

        assertEquals(2, keyRing.aliases().size)
    }

    @Test(expected = VaultCorruptedException::class)
    fun `a tampered container is refused`() {
        store.create(passphrase, recoveryKey)
        val bytes = containerFile.readBytes()
        bytes[bytes.size - 5] = (bytes[bytes.size - 5].toInt() xor 1).toByte()
        containerFile.writeBytes(bytes)

        store.openWithPassphrase(passphrase)
    }

    @Test
    fun `destroy removes keys and files`() {
        store.create(passphrase, recoveryKey)

        store.destroy()

        assertTrue(keyRing.aliases().isEmpty())
        assertFalse(store.exists())
    }

    @Test
    fun `each rotation uses a new key`() {
        var vault = store.create(passphrase, recoveryKey)
        val first = keyRing.aliases().single()
        vault = store.write(vault, VaultIndex(listOf(entry(1))), rotate = true)
        val second = keyRing.aliases().single()
        store.write(vault, VaultIndex(emptyList()), rotate = true)

        assertNotEquals(first, second)
        assertNotEquals(second, keyRing.aliases().single())
    }

    private fun assertThrowsKeyUnavailable(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected the old copy to be unreadable")
        } catch (expected: KeyUnavailableException) {
        }
    }
}
