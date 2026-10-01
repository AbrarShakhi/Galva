package com.abrarshakhi.galva.common

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.abrarshakhi.galva.BuildConfig
import com.abrarshakhi.galva.common.di.appModules
import com.abrarshakhi.galva.core.media.ui.MediaStoreThumbnailFetcher
import com.abrarshakhi.galva.core.vault.data.VaultContent
import com.abrarshakhi.galva.core.vault.ui.VaultImageFetcher
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class GalvaApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@GalvaApp)
            modules(appModules)
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@GalvaApp))
                add(VaultImageFetcher.Factory { GlobalContext.get().get<VaultContent>() })
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
