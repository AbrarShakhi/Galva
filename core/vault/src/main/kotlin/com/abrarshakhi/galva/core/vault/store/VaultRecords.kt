package com.abrarshakhi.galva.core.vault.store

import com.abrarshakhi.galva.core.model.MediaType
import com.abrarshakhi.galva.core.model.PendingOriginal
import com.abrarshakhi.galva.core.model.SecretItem
import com.abrarshakhi.galva.core.vault.crypto.KdfParams
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal class VaultBody(
    @ProtoNumber(1) val kdf: KdfParams,
    @ProtoNumber(2) val masterKeyByPassphrase: ByteArray,
    @ProtoNumber(3) val masterKeyByRecovery: ByteArray,
    @ProtoNumber(4) val sealedIndex: ByteArray,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal class VaultIndex(
    @ProtoNumber(1) val entries: List<SecretEntry> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class SecretEntry(
    @ProtoNumber(1) val id: Long,
    @ProtoNumber(2) val keyset: ByteArray,
    @ProtoNumber(3) val type: String,
    @ProtoNumber(4) val mimeType: String,
    @ProtoNumber(5) val displayName: String,
    @ProtoNumber(6) val sizeBytes: Long,
    @ProtoNumber(7) val width: Int,
    @ProtoNumber(8) val height: Int,
    @ProtoNumber(9) val durationMs: Long,
    @ProtoNumber(10) val dateTakenMs: Long,
    @ProtoNumber(11) val addedAtMs: Long,
    @ProtoNumber(12) val relativePath: String? = null,
    @ProtoNumber(13) val wasFavorite: Boolean = false,
    @ProtoNumber(14) val pendingOriginalId: Long? = null,
    @ProtoNumber(15) val pendingOriginalUri: String? = null,
) {
    val mediaType: MediaType
        get() = MediaType.entries.firstOrNull { it.name == type } ?: MediaType.IMAGE

    fun settled(): SecretEntry = copy(pendingOriginalId = null, pendingOriginalUri = null)

    fun toSecretItem(): SecretItem = SecretItem(
        id = id,
        type = mediaType,
        mimeType = mimeType,
        displayName = displayName,
        sizeBytes = sizeBytes,
        width = width,
        height = height,
        durationMs = durationMs,
        dateTakenMs = dateTakenMs,
        addedAtMs = addedAtMs,
        pendingOriginal = if (pendingOriginalId != null && pendingOriginalUri != null) {
            PendingOriginal(mediaId = pendingOriginalId, uri = pendingOriginalUri)
        } else {
            null
        },
    )
}
