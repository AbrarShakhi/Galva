package com.abrarshakhi.galva.core.vault.media

import android.graphics.ImageDecoder
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.size.pxOrElse
import com.abrarshakhi.galva.core.mediastore.MediaStoreThumbnails
import com.abrarshakhi.galva.core.vault.VaultUri
import com.abrarshakhi.galva.core.vault.crypto.wipe
import com.abrarshakhi.galva.core.vault.data.VaultContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

internal class VaultImageFetcher(
    private val content: VaultContent,
    private val ref: VaultUri.Ref,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val encoded = when (ref.kind) {
            VaultUri.Kind.Thumbnail -> content.readThumbnail(ref.id)
            VaultUri.Kind.Full, VaultUri.Kind.Blob -> content.readOriginal(ref.id)
        }
        try {
            decode(encoded)
        } finally {
            encoded.wipe()
        }
    }

    private fun decode(encoded: ByteArray): ImageFetchResult {
        var sampleSize = 1
        val source = ImageDecoder.createSource(ByteBuffer.wrap(encoded))
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val target = Size(
                options.size.width.pxOrElse { width },
                options.size.height.pxOrElse { height },
            )
            sampleSize = MediaStoreThumbnails.sampleSizeFor(width, height, target)
            decoder.setTargetSampleSize(sampleSize)
        }
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = sampleSize > 1,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val content: () -> VaultContent) : Fetcher.Factory<coil3.Uri> {

        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != VaultUri.SCHEME) return null
            val ref = VaultUri.parse(data.toString()) ?: return null
            return VaultImageFetcher(content(), ref, options)
        }
    }
}

fun ImageLoader.purgeVaultImages() {
    val cache = memoryCache ?: return
    cache.keys
        .filter { it.key.startsWith("${VaultUri.SCHEME}://") }
        .forEach(cache::remove)
}
