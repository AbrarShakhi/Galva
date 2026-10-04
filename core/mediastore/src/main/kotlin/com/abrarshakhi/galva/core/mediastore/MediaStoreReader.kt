package com.abrarshakhi.galva.core.mediastore

interface MediaStoreReader {

    suspend fun queryFingerprints(): List<MediaStoreFingerprint>

    suspend fun queryByIds(ids: List<Long>): List<MediaStoreRecord>
}
