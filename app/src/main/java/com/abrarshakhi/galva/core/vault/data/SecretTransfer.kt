package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.media.domain.model.MediaItem
import com.abrarshakhi.galva.core.vault.data.store.SecretEntry
import java.io.OutputStream

/**
 * Moves plaintext from the gallery into the vault's files.
 *
 * An interface so the repository's ordering — commit before the originals are touched, destroy on
 * undo — can be tested without a content resolver.
 */
interface SecretEncryptor {

    /**
     * Encrypts [item] into the vault's files under [id] with a key of its own, and returns the
     * entry describing it. Nothing is committed: until the entry is written into the index, the
     * files are unreadable noise.
     */
    fun encrypt(item: MediaItem, id: Long, addedAtMs: Long): SecretEntry

    /**
     * Whether the gallery still has the item at [uri]. Answers true when it cannot tell, so an
     * unanswerable question never marks a move as finished.
     */
    fun originalExists(uri: String): Boolean
}

/** Puts decrypted items back into the gallery. */
fun interface SecretExporter {

    /** Writes [entry]'s plaintext into a new gallery item and returns its MediaStore id. */
    fun export(entry: SecretEntry, writePlaintext: (OutputStream) -> Unit): Long
}
