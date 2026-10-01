package com.abrarshakhi.galva.core.vault.data.store

import com.abrarshakhi.galva.core.media.domain.model.MediaType
import com.abrarshakhi.galva.core.vault.data.crypto.KdfParams
import com.abrarshakhi.galva.core.vault.domain.model.PendingOriginal
import com.abrarshakhi.galva.core.vault.domain.model.SecretItem
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * What the container key protects: how to rebuild the master key, and the sealed index.
 *
 * Protobuf with explicit field numbers, so fields can be added later without breaking vaults that
 * already exist — a vault that cannot be read after an update is as lost as one never written.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
class VaultBody(
    @ProtoNumber(1) val kdf: KdfParams,
    @ProtoNumber(2) val masterKeyByPassphrase: ByteArray,
    @ProtoNumber(3) val masterKeyByRecovery: ByteArray,
    /** A [VaultIndex], sealed with the master key. */
    @ProtoNumber(4) val sealedIndex: ByteArray,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class VaultIndex(
    @ProtoNumber(1) val entries: List<SecretEntry> = emptyList(),
)

/**
 * One item's record. [keyset] is the only key that opens the item's blob and thumbnail, and it is
 * stored nowhere but here, inside the sealed index.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SecretEntry(
    @ProtoNumber(1) val id: Long,
    @ProtoNumber(2) val keyset: ByteArray,
    /** [MediaType] by name, so reordering the enum cannot reinterpret stored items. */
    @ProtoNumber(3) val type: String,
    @ProtoNumber(4) val mimeType: String,
    @ProtoNumber(5) val displayName: String,
    @ProtoNumber(6) val sizeBytes: Long,
    @ProtoNumber(7) val width: Int,
    @ProtoNumber(8) val height: Int,
    @ProtoNumber(9) val durationMs: Long,
    @ProtoNumber(10) val dateTakenMs: Long,
    @ProtoNumber(11) val addedAtMs: Long,
    /** The folder the original lived in, so restoring puts it back where it was. */
    @ProtoNumber(12) val relativePath: String? = null,
    @ProtoNumber(13) val wasFavorite: Boolean = false,
    @ProtoNumber(14) val pendingOriginalId: Long? = null,
    @ProtoNumber(15) val pendingOriginalUri: String? = null,
) {
    val mediaType: MediaType
        get() = MediaType.entries.firstOrNull { it.name == type } ?: MediaType.IMAGE

    /** The original has left the gallery; this entry is now the only copy. */
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
