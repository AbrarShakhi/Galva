package com.abrarshakhi.galva.core.media.data.mediastore

import com.abrarshakhi.galva.core.media.data.local.dao.MediaFingerprint

/**
 * The reads the sync pass makes against the device's media collection.
 *
 * `MediaSyncManager` depends on this rather than on [MediaStoreDataSource] directly so its
 * deletion guard can be exercised against a reader that returns a truncated result — a condition
 * that is real on device but cannot be provoked through a live content resolver.
 */
interface MediaStoreReader {

    suspend fun queryFingerprints(): List<MediaFingerprint>

    suspend fun queryByIds(ids: List<Long>): List<MediaStoreRecord>
}
