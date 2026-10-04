package com.abrarshakhi.galva.core.vault.data

import com.abrarshakhi.galva.core.model.MediaItem
import com.abrarshakhi.galva.core.vault.store.SecretEntry
import java.io.OutputStream

internal interface SecretEncryptor {

    fun encrypt(item: MediaItem, id: Long, addedAtMs: Long): SecretEntry

    fun originalExists(uri: String): Boolean
}

internal fun interface SecretExporter {

    fun export(entry: SecretEntry, writePlaintext: (OutputStream) -> Unit): Long
}
