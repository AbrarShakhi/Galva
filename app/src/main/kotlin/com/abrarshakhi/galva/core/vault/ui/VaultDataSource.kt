package com.abrarshakhi.galva.core.vault.ui

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.domain.model.VaultUri
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import kotlin.math.min

/**
 * Plays vault videos by decrypting them as Media3 reads, one segment at a time.
 *
 * Seeking maps straight onto the decrypting channel's position, so jumping to the end of a long
 * clip decrypts the segment there rather than everything before it — and nothing decrypted is
 * ever written anywhere, not even to a cache.
 */
@UnstableApi
class VaultDataSource(private val content: VaultContent) : BaseDataSource(/* isNetwork = */ false) {

    private var channel: SeekableByteChannel? = null
    private var uri: Uri? = null
    private var bytesRemaining = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)
        val ref = VaultUri.parse(dataSpec.uri.toString())?.takeIf { it.kind == VaultUri.Kind.Blob }
            ?: throw DataSourceException(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)

        val decrypting = try {
            content.openOriginal(ref.id)
        } catch (error: Exception) {
            throw DataSourceException(error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        channel = decrypting

        val size = decrypting.size()
        if (dataSpec.position > size) {
            throw DataSourceException(PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE)
        }
        decrypting.position(dataSpec.position)
        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else {
            size - dataSpec.position
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val target = ByteBuffer.wrap(buffer, offset, min(length.toLong(), bytesRemaining).toInt())
        val read = try {
            val decrypting = checkNotNull(channel)
            var count: Int
            // A zero-length read means "nothing yet"; Media3 needs at least one byte or the end.
            do {
                count = decrypting.read(target)
            } while (count == 0)
            count
        } catch (error: IOException) {
            throw DataSourceException(error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        }
        if (read < 0) return C.RESULT_END_OF_INPUT
        bytesRemaining -= read
        bytesTransferred(read)
        return read
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            channel?.close()
        } catch (error: IOException) {
            throw DataSourceException(error, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)
        } finally {
            channel = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    class Factory(private val content: VaultContent) : DataSource.Factory {

        override fun createDataSource(): DataSource = VaultDataSource(content)
    }
}
