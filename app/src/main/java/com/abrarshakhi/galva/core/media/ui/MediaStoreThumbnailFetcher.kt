package com.abrarshakhi.galva.core.media.ui

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import coil3.toAndroidUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Serves grid thumbnails from MediaStore's own thumbnail cache.
 *
 * Coil's built-in `ContentUriFetcher` opens the original file and decodes it, so a wall of 12 MP
 * photos costs a full decode per cell on every cold start. `ContentResolver.loadThumbnail` returns
 * a small, already-generated bitmap instead, which is what makes scrolling a large library viable.
 *
 * It only claims requests small enough to be thumbnails; the full-screen viewer falls through to
 * Coil's default path so the image is shown at full quality.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val width = options.size.width.pxOrElse { DEFAULT_THUMBNAIL_PX }
        val height = options.size.height.pxOrElse { DEFAULT_THUMBNAIL_PX }
        val requested = Size(max(width, MIN_PX), max(height, MIN_PX))

        val bitmap = loadMediaStoreThumbnail(requested) ?: decodeSubsampled(requested)
        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun loadMediaStoreThumbnail(size: Size): Bitmap? = try {
        context.contentResolver.loadThumbnail(uri, size, CancellationSignal())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        // Some OEM providers refuse loadThumbnail for particular items; fall back rather than
        // leaving a hole in the grid.
        null
    }

    /** Last resort: decode the original with the coarsest sample size that still fills [size]. */
    private fun decodeSubsampled(size: Size): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decode = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, size)
        }
        return context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, decode) }
            ?: throw IllegalStateException("Cannot open $uri")
    }

    private fun sampleSizeFor(sourceWidth: Int, sourceHeight: Int, target: Size): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0) return 1
        var sampleSize = 1
        while (sourceWidth / (sampleSize * 2) >= target.width &&
            sourceHeight / (sampleSize * 2) >= target.height
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    class Factory(private val context: Context) : Fetcher.Factory<coil3.Uri> {

        override fun create(
            data: coil3.Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            val androidUri = data.toAndroidUri()
            if (!androidUri.isMediaStoreUri()) return null
            if (!options.size.isThumbnailSized()) return null
            return MediaStoreThumbnailFetcher(context, androidUri, options)
        }

        private fun Uri.isMediaStoreUri(): Boolean =
            scheme == ContentResolver.SCHEME_CONTENT && authority == MEDIA_AUTHORITY

        private fun coil3.size.Size.isThumbnailSized(): Boolean {
            val width = width.pxOrElse { DEFAULT_THUMBNAIL_PX }
            val height = height.pxOrElse { DEFAULT_THUMBNAIL_PX }
            return width <= THUMBNAIL_MAX_PX && height <= THUMBNAIL_MAX_PX
        }
    }
}

private const val MEDIA_AUTHORITY = "media"
private const val DEFAULT_THUMBNAIL_PX = 384
private const val THUMBNAIL_MAX_PX = 512
private const val MIN_PX = 96
