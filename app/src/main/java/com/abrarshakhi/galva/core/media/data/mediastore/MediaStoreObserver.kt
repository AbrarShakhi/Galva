package com.abrarshakhi.galva.core.media.data.mediastore

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

class MediaStoreObserver(
    private val context: Context,
) {

    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        WATCHED_COLLECTIONS.forEach { collection ->
            resolver.registerContentObserver(
                collection, /* notifyForDescendants = */
                true, observer
            )
        }
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private companion object {
        val WATCHED_COLLECTIONS = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        )
    }
}
