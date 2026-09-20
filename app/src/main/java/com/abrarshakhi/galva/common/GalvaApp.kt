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
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
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

    /**
     * The video decoder is what lets a video thumbnail render at all: without it Coil has no way
     * to turn a video URI into a bitmap and every clip in the grid would come up blank.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                // Claims grid-sized requests first; larger ones fall through to Coil's default
                // content-URI path, and the video decoder covers frames it cannot thumbnail.
                add(MediaStoreThumbnailFetcher.Factory(this@GalvaApp))
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
}
