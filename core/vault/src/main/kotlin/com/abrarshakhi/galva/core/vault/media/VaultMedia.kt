package com.abrarshakhi.galva.core.vault.media

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import coil3.fetch.Fetcher
import com.abrarshakhi.galva.core.vault.data.VaultContent

class VaultMedia internal constructor(private val content: VaultContent) {

    fun imageFetcherFactory(): Fetcher.Factory<coil3.Uri> = VaultImageFetcher.Factory { content }

    @UnstableApi
    fun dataSourceFactory(): DataSource.Factory = VaultDataSource.Factory(content)
}
