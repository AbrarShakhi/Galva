package com.abrarshakhi.galva.core.media.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
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
import com.abrarshakhi.galva.core.media.data.mediastore.MediaStoreThumbnails
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

        val resolver = context.contentResolver
        val bitmap = MediaStoreThumbnails.cached(resolver, uri, requested)
            ?: MediaStoreThumbnails.decodeSubsampled(resolver, uri, requested)
            ?: throw IllegalStateException("Cannot open $uri")
        ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
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
