package com.abrarshakhi.galva.core.vault

import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.model.MediaType
import com.abrarshakhi.galva.core.vault.crypto.HardwareKeyRing
import com.abrarshakhi.galva.core.vault.crypto.KdfParams
import com.abrarshakhi.galva.core.vault.crypto.KeyUnavailableException
import com.abrarshakhi.galva.core.vault.crypto.PassphraseKdf
import com.abrarshakhi.galva.core.vault.crypto.RecoveryPhrase
import com.abrarshakhi.galva.core.vault.crypto.VaultContext
import com.abrarshakhi.galva.core.vault.crypto.VaultCrypto
import com.abrarshakhi.galva.core.vault.data.SecretEncryptor
import com.abrarshakhi.galva.core.vault.data.SecretExporter
import com.abrarshakhi.galva.core.vault.store.SecretEntry
import com.abrarshakhi.galva.core.vault.store.VaultFiles
import com.google.crypto.tink.subtle.AesGcmJce
import com.google.crypto.tink.subtle.Hkdf
import java.io.File
import java.io.OutputStream
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class FakeKeyRing : HardwareKeyRing {

    private val keys = ConcurrentHashMap<String, ByteArray>()
    private val counter = AtomicInteger()
    private val random = SecureRandom()

    override fun createKey(): String {
        val alias = "test.epoch.${counter.getAndIncrement()}"
        keys[alias] = ByteArray(32).also(random::nextBytes)
        return alias
    }

    override fun wrap(alias: String, key: ByteArray, context: ByteArray): ByteArray =
        AesGcmJce(keys[alias] ?: throw KeyUnavailableException("no key $alias")).encrypt(key, context)

    override fun unwrap(alias: String, wrapped: ByteArray, context: ByteArray): ByteArray =
        AesGcmJce(keys[alias] ?: throw KeyUnavailableException("no key $alias")).decrypt(wrapped, context)

    override fun delete(alias: String) {
        keys.remove(alias)
    }

    override fun aliases(): Set<String> = keys.keys.toSet()

    fun plantOrphan(): String = createKey()
}

internal class FakeKdf : PassphraseKdf {

    override fun newParams(salt: ByteArray): KdfParams =
        KdfParams(salt = salt, memoryKib = 8, iterations = 1, parallelism = 1)

    override fun deriveKey(passphrase: ByteArray, params: KdfParams): ByteArray =
        Hkdf.computeHkdf("HMACSHA256", passphrase, params.salt, "fake-kdf".toByteArray(), 32)
}

internal class FakeEncryptor(
    private val files: VaultFiles,
    private val crypto: VaultCrypto,
) : SecretEncryptor {

    val galleryUris: MutableSet<String> = ConcurrentHashMap.newKeySet()

    var beforeEncrypt: (MediaItem) -> Unit = {}

    var content: (MediaItem) -> ByteArray = ::plaintextOf

    override fun encrypt(item: MediaItem, id: Long, addedAtMs: Long): SecretEntry {
        beforeEncrypt(item)
        val keyset = crypto.newFileKeyset()
        val aead = crypto.streamingAead(keyset)
        files.writeAtomically(files.blob(id)) { out ->
            aead.newEncryptingStream(out, VaultContext.blob(id)).use { it.write(content(item)) }
        }
        files.writeAtomically(files.thumbnail(id)) { out ->
            aead.newEncryptingStream(out, VaultContext.thumbnail(id)).use { it.write(thumbnailOf(item)) }
        }
        return SecretEntry(
            id = id,
            keyset = keyset,
            type = item.type.name,
            mimeType = item.mimeType,
            displayName = item.displayName,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            durationMs = item.durationMs,
            dateTakenMs = item.dateTakenMs,
            addedAtMs = addedAtMs,
            relativePath = "DCIM/Camera/",
            wasFavorite = item.isFavorite,
            pendingOriginalId = item.id,
            pendingOriginalUri = item.uri,
        )
    }

    override fun originalExists(uri: String): Boolean = uri in galleryUris

    companion object {
        fun plaintextOf(item: MediaItem): ByteArray = "original bytes of ${item.id}".toByteArray()
        fun thumbnailOf(item: MediaItem): ByteArray = "thumbnail of ${item.id}".toByteArray()
    }
}

internal class FakeExporter : SecretExporter {

    val written = ConcurrentHashMap<Long, ByteArray>()
    private val nextId = AtomicInteger(10_000)

    val refuse: MutableSet<String> = ConcurrentHashMap.newKeySet()

    override fun export(entry: SecretEntry, writePlaintext: (OutputStream) -> Unit): Long {
        if (entry.displayName in refuse) throw java.io.IOException("refused ${entry.displayName}")
        val out = java.io.ByteArrayOutputStream()
        writePlaintext(out)
        val mediaId = nextId.getAndIncrement().toLong()
        written[mediaId] = out.toByteArray()
        return mediaId
    }
}

internal fun mediaItem(id: Long, favorite: Boolean = false, type: MediaType = MediaType.IMAGE): MediaItem =
    MediaItem(
        id = id,
        uri = "content://media/external/images/media/$id",
        displayName = "IMG_$id.jpg",
        mimeType = "image/jpeg",
        type = type,
        sizeBytes = 1_000,
        width = 4000,
        height = 3000,
        durationMs = 0,
        dateTakenMs = 1_700_000_000_000 + id,
        dateModifiedMs = 1_700_000_000_000 + id,
        albumId = 1,
        albumName = "Camera",
        isFavorite = favorite,
    )

internal fun bip39(): RecoveryPhrase =
    RecoveryPhrase(File("src/main/assets/bip39-english.txt").readLines().filter(String::isNotBlank))
