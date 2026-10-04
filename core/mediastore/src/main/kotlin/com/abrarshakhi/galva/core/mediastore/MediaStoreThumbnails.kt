package com.abrarshakhi.galva.core.mediastore

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import kotlinx.coroutines.CancellationException

object MediaStoreThumbnails {

    fun cached(resolver: ContentResolver, uri: Uri, size: Size): Bitmap? = try {
        resolver.loadThumbnail(uri, size, CancellationSignal())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        null
    }

    fun decodeSubsampled(resolver: ContentResolver, uri: Uri, size: Size): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decode = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, size)
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }
    }

    fun sampleSizeFor(sourceWidth: Int, sourceHeight: Int, target: Size): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0) return 1
        var sampleSize = 1
        while (sourceWidth / (sampleSize * 2) >= target.width &&
            sourceHeight / (sampleSize * 2) >= target.height
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
