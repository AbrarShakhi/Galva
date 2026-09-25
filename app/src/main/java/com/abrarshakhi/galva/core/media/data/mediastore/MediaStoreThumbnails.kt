package com.abrarshakhi.galva.core.media.data.mediastore

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import kotlinx.coroutines.CancellationException

/**
 * Small bitmaps of MediaStore items, preferring MediaStore's own thumbnail cache.
 *
 * Shared by the grid's Coil fetcher and by the vault, which makes each item's thumbnail once while
 * the original is still readable and then keeps only an encrypted copy of it.
 */
object MediaStoreThumbnails {

    /**
     * MediaStore's already-generated thumbnail, or null when the provider refuses this item —
     * some OEM providers do for particular files, and the caller should fall back rather than
     * leave a hole in the grid.
     */
    fun cached(resolver: ContentResolver, uri: Uri, size: Size): Bitmap? = try {
        resolver.loadThumbnail(uri, size, CancellationSignal())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        null
    }

    /**
     * Last resort for images: decode the original at the coarsest sample size that still fills
     * [size]. Null when the item cannot be opened or is not an image BitmapFactory understands.
     */
    fun decodeSubsampled(resolver: ContentResolver, uri: Uri, size: Size): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decode = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, size)
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decode) }
    }

    /** The largest power-of-two reduction that keeps both edges at least as big as [target]. */
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
