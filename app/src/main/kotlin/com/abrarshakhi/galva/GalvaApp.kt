package com.abrarshakhi.galva

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.abrarshakhi.galva.core.ui.media.MediaStoreThumbnailFetcher
import com.abrarshakhi.galva.core.vault.media.VaultMedia
import com.abrarshakhi.galva.di.galvaModules
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class GalvaApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@GalvaApp)
            modules(galvaModules)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@GalvaApp))
                add(get<VaultMedia>().imageFetcherFactory())
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
